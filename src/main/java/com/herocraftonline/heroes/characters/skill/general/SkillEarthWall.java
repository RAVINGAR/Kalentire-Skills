package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SkillEarthWall extends ActiveSkill {

    private static final Set<Location> changedBlocks = new HashSet<>();
    private String applyText;
    private String expireText;

    public SkillEarthWall(final Heroes plugin) {
        super(plugin, "Earthwall");
        setDescription("Create a wall of Earth up to $1 blocks in front of you.");
        setUsage("/skill earthwall");
        setArgumentRange(0, 0);
        setIdentifiers("skill earthwall");
        setTypes(SkillType.ABILITY_PROPERTY_EARTH, SkillType.SILENCEABLE, SkillType.BLOCK_CREATING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillBlockListener(), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set("height", 4);
        node.set("width", 2);
        node.set(SkillSetting.MAX_DISTANCE.node(), 12);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), 0.2);
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% conjures a wall of earth!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero%'s wall has crumbled");
        node.set("block-type", "DIRT");

        return node;
    }

    @Override
    public String getDescription(final Hero hero) {
        //int height = SkillConfigManager.getUseSetting(hero, this, "height", 3, false) * 2;
        //int width = SkillConfigManager.getUseSetting(hero, this, "width", 2, false) * 2;
        int maxDist = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 12, false);
        final double maxDistIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT, 0.2, false);
        maxDist += (int) (hero.getAttributeValue(AttributeType.INTELLECT) * maxDistIncrease);

        //String type = SkillConfigManager.getUseSetting(hero, this, "block-type", "DIRT");

        return getDescription().replace("$1", maxDist + "");//.replace("$2", width + "").replace("$3", height + "");
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% conjures a wall of earth!").replace("%hero%", "$1").replace("$hero$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero%'s wall has crumbled").replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        final int height = SkillConfigManager.getUseSetting(hero, this, "height", 4, false);
        final int width = SkillConfigManager.getUseSetting(hero, this, "width", 2, false);

        int maxDist = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 12, false);
        final double maxDistIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT, 0.2, false);
        maxDist += (int) (hero.getAttributeValue(AttributeType.INTELLECT) * maxDistIncrease);

        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        final Material setter = Material.valueOf(SkillConfigManager.getUseSetting(hero, this, "block-type", "DIRT"));

        final Block tBlock = player.getTargetBlock((HashSet<Material>) null, maxDist);

        final ShieldWallEffect swEffect = new ShieldWallEffect(this, player, duration, tBlock, width, height, setter);
        hero.addEffect(swEffect);

        return SkillResult.NORMAL;
    }

    public static class SkillBlockListener implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onBlockBreak(final BlockBreakEvent event) {
            if (changedBlocks.contains(event.getBlock().getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    public class ShieldWallEffect extends ExpirableEffect {
        private final Block tBlock;
        private final int width;
        private final int height;
        private final Material setter;
        private final List<Location> locations = new ArrayList<>();

        public ShieldWallEffect(final Skill skill, final Player applier, final long duration, final Block tBlock, final int width, final int height, final Material setter) {
            super(skill, "EarthWall", applier, duration);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.MAGIC);

            this.tBlock = tBlock;
            this.width = width;
            this.height = height;
            this.setter = setter;
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);

            final Player player = hero.getPlayer();

            int maxDist = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.MAX_DISTANCE, 12, false);
            final double maxDistIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT, 0.2, false);
            maxDist += (int) (hero.getAttributeValue(AttributeType.INTELLECT) * maxDistIncrease);

            final List<Entity> entities = player.getNearbyEntities(maxDist * 2, maxDist * 2, maxDist * 2);
            final List<Entity> blockEntities = new ArrayList<>();
            for (final Entity entity : entities) {
                if (entity instanceof ItemFrame) {
                    blockEntities.add(entity);
                } else if (entity instanceof Painting) {
                    blockEntities.add(entity);
                }
            }

            if (is_X_Direction(player)) {
                for (int yDir = 0; yDir < height; yDir++) {
                    for (int xDir = -width; xDir < width + 1; xDir++) {
                        final Block chBlock = tBlock.getRelative(xDir, yDir, 0);
                        final Location location = chBlock.getLocation();
                        switch (chBlock.getType()) {
                            case SNOW:
                            case AIR:
                                boolean isBlockEntityBlock = false;
                                for (final Entity blockEntity : blockEntities) {
                                    if (blockEntity.getLocation().getBlock().equals(chBlock)) {
                                        isBlockEntityBlock = true;
                                    }
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
            } else {
                for (int yDir = 0; yDir < height; yDir++) {
                    for (int zDir = -width; zDir < width + 1; zDir++) {
                        final Block chBlock = tBlock.getRelative(0, yDir, zDir);
                        final Location location = chBlock.getLocation();
                        switch (chBlock.getType()) {
                            case SNOW:
                            case AIR:
                                boolean isBlockEntityBlock = false;
                                for (final Entity blockEntity : blockEntities) {
                                    if (blockEntity.getLocation().getBlock().equals(chBlock)) {
                                        isBlockEntityBlock = true;
                                    }
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

            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);

            final Player player = hero.getPlayer();

            revertBlocks();

            broadcast(player.getLocation(), "    " + expireText, player.getName());
        }

        private void revertBlocks() {
            for (final Location location : locations) {
                location.getBlock().setType(Material.AIR);
                changedBlocks.remove(location);
            }

            locations.clear();
        }

        private boolean is_X_Direction(final Player player) {
            Vector u = player.getLocation().getDirection();
            u = new Vector(u.getX(), 0.0D, u.getZ()).normalize();
            final Vector v = new Vector(0, 0, -1);
            final double magU = Math.sqrt(Math.pow(u.getX(), 2.0D) + Math.pow(u.getZ(), 2.0D));
            final double magV = Math.sqrt(Math.pow(v.getX(), 2.0D) + Math.pow(v.getZ(), 2.0D));
            double angle = Math.acos(u.dot(v) / (magU * magV));
            angle = angle * 180.0D / Math.PI;
            angle = Math.abs(angle - 180.0D);

            return (angle <= 45.0D) || (angle > 135.0D);
        }
    }
}
