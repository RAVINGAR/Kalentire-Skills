package com.herocraftonline.heroes.characters.skill.skills;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.util.Vector;

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
import com.herocraftonline.heroes.util.Messaging;

public class SkillEarthWall extends ActiveSkill {

    private String applyText;
    private String expireText;
    private static Set<Location> changedBlocks = new HashSet<Location>();

    public SkillEarthWall(Heroes plugin) {
        super(plugin, "Earthwall");
        setDescription("Create a wall of Earth up to $1 blocks in front of you.");
        setUsage("/skill earthwall");
        setArgumentRange(0, 0);
        setIdentifiers("skill earthwall");
        setTypes(SkillType.ABILITY_PROPERTY_EARTH, SkillType.SILENCABLE, SkillType.BLOCK_CREATING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillBlockListener(), plugin);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("height", Integer.valueOf(4));
        node.set("width", Integer.valueOf(2));
        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(12));
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), 0.2);
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(5000));
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%hero% conjures a wall of earth!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%hero%'s wall has crumbled");
        node.set("block-type", "DIRT");

        return node;
    }

    public String getDescription(Hero hero) {
        //int height = SkillConfigManager.getUseSetting(hero, this, "height", 3, false) * 2;
        //int width = SkillConfigManager.getUseSetting(hero, this, "width", 2, false) * 2;
        int maxDist = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 12, false);
        double maxDistIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT, 0.2, false);
        maxDist += (int) (hero.getAttributeValue(AttributeType.INTELLECT) * maxDistIncrease);

        //String type = SkillConfigManager.getUseSetting(hero, this, "block-type", "DIRT");

        return getDescription().replace("$1", maxDist + "");//.replace("$2", width + "").replace("$3", height + "");
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%hero% conjures a wall of earth!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%hero%'s wall has crumbled").replace("%hero%", "$1");
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int height = SkillConfigManager.getUseSetting(hero, this, "height", 4, false);
        int width = SkillConfigManager.getUseSetting(hero, this, "width", 2, false);

        int maxDist = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 12, false);
        double maxDistIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT, 0.2, false);
        maxDist += (int) (hero.getAttributeValue(AttributeType.INTELLECT) * maxDistIncrease);

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        Material setter = Material.valueOf(SkillConfigManager.getUseSetting(hero, this, "block-type", "DIRT"));

        Block tBlock = player.getTargetBlock(null, maxDist);

        ShieldWallEffect swEffect = new ShieldWallEffect(this, player, duration, tBlock, width, height, setter);
        hero.addEffect(swEffect);

        return SkillResult.NORMAL;
    }

    public class SkillBlockListener implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onBlockBreak(BlockBreakEvent event) {
            if (changedBlocks.contains(event.getBlock().getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    public class ShieldWallEffect extends ExpirableEffect {
        private final Block tBlock;
        private final int width;
        private final int height;
        private Material setter;
        private List<Location> locations = new ArrayList<Location>();

        public ShieldWallEffect(Skill skill, Player applier, long duration, Block tBlock, int width, int height, Material setter) {
            super(skill, "EarthWall", applier, duration);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.MAGIC);

            this.tBlock = tBlock;
            this.width = width;
            this.height = height;
            this.setter = setter;
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            Player player = hero.getPlayer();
            if (is_X_Direction(player)) {
                for (int yDir = 0; yDir < height; yDir++) {
                    for (int xDir = -width; xDir < width + 1; xDir++) {
                        Block chBlock = tBlock.getRelative(xDir, yDir, 0);
                        attemptToChangeBlock(chBlock.getLocation());
                    }
                }
            }
            else {
                for (int yDir = 0; yDir < height; yDir++) {
                    for (int zDir = -width; zDir < width + 1; zDir++) {
                        Block chBlock = tBlock.getRelative(0, yDir, zDir);
                        attemptToChangeBlock(chBlock.getLocation());
                    }
                }
            }

            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            Player player = hero.getPlayer();

            revertBlocks();

            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }

        private void attemptToChangeBlock(Location location) {
            Block block = location.getBlock();
            switch (block.getType()) {
                case SNOW:
                case AIR:
                    changedBlocks.add(location);
                    locations.add(location);
                    location.getBlock().setType(setter);
                    break;
                default:
                    break;
            }
        }

        private void revertBlocks() {
            for (Location location : locations) {
                location.getBlock().setType(Material.AIR);
                changedBlocks.remove(location);
            }

            locations.clear();
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
    }
}
