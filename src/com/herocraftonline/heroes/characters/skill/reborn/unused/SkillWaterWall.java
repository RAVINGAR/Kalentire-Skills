package com.herocraftonline.heroes.characters.skill.reborn.unused;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SkillWaterWall extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillWaterWall(Heroes plugin) {
        super(plugin, "Waterwall");
        setDescription("Create a wall of Water up to $1 blocks in front of you.");
        setUsage("/skill Waterwall");
        setArgumentRange(0, 0);
        setIdentifiers("skill Waterwall");
        setTypes(SkillType.ABILITY_PROPERTY_WATER, SkillType.SILENCEABLE, SkillType.BLOCK_CREATING);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("height", 4);
        node.set("width", 2);
        node.set(SkillSetting.MAX_DISTANCE.node(), 12);
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% conjures a wall of Water!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero%'s wall has crumbled");

        return node;
    }

    public String getDescription(Hero hero) {
        //int height = SkillConfigManager.getUseSetting(hero, this, "height", 3, false) * 2;
        //int width = SkillConfigManager.getUseSetting(hero, this, "width", 2, false) * 2;
        int maxDist = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.MAX_DISTANCE, false);

        return getDescription().replace("$1", maxDist + "");//.replace("$2", width + "").replace("$3", height + "");
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% conjures a wall of Water!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero%'s wall has crumbled").replace("%hero%", "$1");
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        if (hero.hasEffect("WaterWall")) {
            hero.removeEffect(hero.getEffect("WaterWall"));
            return SkillResult.SKIP_POST_USAGE;
        }

        int height = SkillConfigManager.getUseSetting(hero, this, "height", 4, false);
        int width = SkillConfigManager.getUseSetting(hero, this, "width", 2, false);

        int maxDist = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.MAX_DISTANCE, false);

        long duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        Block tBlock = player.getTargetBlock((HashSet<Material>)null, maxDist);

        ShieldWallEffect swEffect = new ShieldWallEffect(this, player, duration, tBlock, width, height);
        hero.addEffect(swEffect);

        return SkillResult.NORMAL;
    }

    public class ShieldWallEffect extends ExpirableEffect {
        private final Block tBlock;
        private final int width;
        private final int height;
        private final Material setter = Material.WATER;
        private Set<Location> changedBlocks = new HashSet<Location>();
        private List<Location> locations = new ArrayList<Location>();
        private SkillBlockListener listener = new SkillBlockListener();

        public ShieldWallEffect(Skill skill, Player applier, long duration, Block tBlock, int width, int height) {
            super(skill, "WaterWall", applier, duration);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.MAGIC);

            this.tBlock = tBlock;
            this.width = width;
            this.height = height;
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            Player player = hero.getPlayer();
            Bukkit.getServer().getPluginManager().registerEvents(listener, plugin);

            int maxDist = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.MAX_DISTANCE, 12, false);

            List<Entity> entities = player.getNearbyEntities(maxDist * 2, maxDist * 2, maxDist * 2);
            List<Entity> blockEntities = new ArrayList<>();
            for (Entity entity : entities) {
                if (entity instanceof ItemFrame)
                    blockEntities.add(entity);
                else if (entity instanceof Painting)
                    blockEntities.add(entity);
            }

            if (is_X_Direction(player)) {
                BuildXWall(blockEntities);
            } else {
                BuildYWall(blockEntities);
            }

            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        private void BuildYWall(List<Entity> blocksToChange) {
            for (int yDir = 0; yDir < height; yDir++) {
                for (int zDir = -width; zDir < width + 1; zDir++) {
                    Block chBlock = tBlock.getRelative(0, yDir, zDir);
                    Location location = chBlock.getLocation();
                    switch (chBlock.getType()) {
                        case SNOW:
                        case AIR:
                            boolean isBlockEntityBlock = false;
                            for (Entity blockEntity : blocksToChange) {
                                if (blockEntity.getLocation().getBlock().equals(chBlock))
                                    isBlockEntityBlock = true;
                            }
                            if (!isBlockEntityBlock) {
                                changedBlocks.add(location);
                                locations.add(location);
                                location.getBlock().setType(setter);
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        private void BuildXWall(List<Entity> blocksToChange) {
            for (int yDir = 0; yDir < height; yDir++) {
                for (int xDir = -width; xDir < width + 1; xDir++) {
                    Block chBlock = tBlock.getRelative(xDir, yDir, 0);
                    Location location = chBlock.getLocation();
                    switch (chBlock.getType()) {
                        case SNOW:
                        case AIR:
                            boolean isBlockEntityBlock = false;
                            for (Entity blockEntity : blocksToChange) {
                                if (blockEntity.getLocation().getBlock().equals(chBlock))
                                    isBlockEntityBlock = true;
                            }
                            if (!isBlockEntityBlock) {
                                changedBlocks.add(location);
                                locations.add(location);
                                location.getBlock().setType(setter);
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            Player player = hero.getPlayer();
            revertBlocks();

            broadcast(player.getLocation(), "    " + expireText, player.getName());
        }

        private void revertBlocks() {
            for (Location location : locations) {
                location.getBlock().setType(Material.AIR);
                changedBlocks.remove(location);
            }

            locations.clear();
            HandlerList.unregisterAll(listener);
        }

        private boolean is_X_Direction(Player player) {
            Vector u = player.getLocation().getDirection();
            u = new Vector(u.getX(), 0.0D, u.getZ()).normalize();
            Vector v = new Vector(0, 0, -1);
            double magU = Math.sqrt(Math.pow(u.getX(), 2.0D) + Math.pow(u.getZ(), 2.0D));
            double magV = Math.sqrt(Math.pow(v.getX(), 2.0D) + Math.pow(v.getZ(), 2.0D));
            double angle = Math.acos(u.dot(v) / (magU * magV));
            angle = angle * 180.0D / Math.PI;
            angle = Math.abs(angle - 180.0D);

            return (angle <= 45.0D) || (angle > 135.0D);
        }

        public class SkillBlockListener implements Listener {

            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void onBlockPlace(BlockPlaceEvent event) {
                Block block = event.getBlock();
                if (block != null && changedBlocks.contains(block.getLocation())) {
                    event.setCancelled(true);
                }
            }

            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void onBlockBreak(BlockBreakEvent event) {
                Block block = event.getBlock();
                if (block != null && changedBlocks.contains(block.getLocation())) {
                    event.setCancelled(true);
                }
            }

            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void onBlockFromTo(BlockFromToEvent event) {
                Block fromBlock = event.getBlock();
                Block toBlock = event.getToBlock();
                if (changedBlocks.contains(toBlock.getLocation()) || changedBlocks.contains(fromBlock.getLocation()))
                    event.setCancelled(true);
            }

            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void onBlockPistonExtend(BlockPistonExtendEvent event) {
                Block block = event.getBlock();
                if (block != null && changedBlocks.contains(block.getLocation()))
                    event.setCancelled(true);
            }
        }
    }
}
