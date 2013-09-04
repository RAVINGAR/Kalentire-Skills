package com.herocraftonline.heroes.characters.skill.skills;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.material.Vine;

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

public class SkillOvergrowth extends ActiveSkill {

    private String applyText;
    private String expireText;

    private final BlockFace[] faces = { BlockFace.NORTH, BlockFace.NORTH_EAST, BlockFace.EAST, BlockFace.SOUTH_EAST, BlockFace.SOUTH, BlockFace.SOUTH_WEST, BlockFace.WEST, BlockFace.NORTH_WEST };
    private static Set<Location> changedBlocks = new HashSet<Location>();

    public SkillOvergrowth(Heroes plugin) {
        super(plugin, "Overgrowth");
        setDescription("Create an overgrowth of vines at your target location.");
        setUsage("/skill earthwall");
        setArgumentRange(0, 0);
        setIdentifiers("skill overgrowth");
        setTypes(SkillType.ABILITY_PROPERTY_EARTH, SkillType.SILENCABLE, SkillType.BLOCK_CREATING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillBlockListener(), plugin);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(12));
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), 0.2);
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(15000));
        node.set("max-growth-distance", Integer.valueOf(30));
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%hero% creates an overgrowth of vines!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%hero%'s vines have withered.");

        return node;
    }

    public String getDescription(Hero hero) {

        return getDescription();
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%hero% creates an overgrowth of vines!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%hero%'s vines have withered.").replace("%hero%", "$1");
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int maxDist = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 12, false);
        double maxDistIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT, 0.2, false);
        maxDist += (int) (hero.getAttributeValue(AttributeType.INTELLECT) * maxDistIncrease);

        int maxGrowthDistance = SkillConfigManager.getUseSetting(hero, this, "max-growth-distance", 30, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);

        Block tBlock = player.getTargetBlock(null, maxDist);

        BlockFace targetFace = faces[Math.round(player.getEyeLocation().getYaw() / 45f) & 0x7];

        Block placementBlock = tBlock.getRelative(targetFace);

        Heroes.log(Level.INFO, "Delf Debug: Overgrowth: targetface = " + targetFace.toString() + "Relative block: " + placementBlock.getType().toString());

        if (placementBlock.getType() != Material.AIR) {
            return SkillResult.INVALID_TARGET;
        }

        OvergrowthEffect oEffect = new OvergrowthEffect(this, player, duration, placementBlock, targetFace, maxGrowthDistance);
        hero.addEffect(oEffect);

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

    public class OvergrowthEffect extends ExpirableEffect {
        private final Block targetBlock;
        private final BlockFace targetFace;
        private final int maxGrowth;
        private List<Location> locations = new ArrayList<Location>();

        public OvergrowthEffect(Skill skill, Player applier, long duration, Block targetBlock, BlockFace targetFace, int maxGrowth) {
            super(skill, "Overgrowth", applier, duration);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.MAGIC);

            this.targetBlock = targetBlock;
            this.targetFace = targetFace;
            this.maxGrowth = maxGrowth;
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            Player player = hero.getPlayer();

            growVines();

            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            Player player = hero.getPlayer();

            revertBlocks();

            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }

        private void growVines() {
            boolean breakLoop = false;
            Block workingBlock = targetBlock;
            for (int i = 0; i < maxGrowth; i++) {
                Location location = workingBlock.getLocation();
                switch (targetBlock.getType()) {
                    case WATER:
                    case LAVA:
                    case SNOW:
                    case AIR:
                        changedBlocks.add(location);
                        locations.add(location);
                        workingBlock.setType(Material.VINE);
                        ((Vine) workingBlock).putOnFace(targetFace);

                        location.getWorld().playSound(location, Sound.DIG_GRASS, 0.8F, 1.0F);

                        break;
                    default:
                        breakLoop = true;
                        break;
                }
                if (breakLoop)
                    break;
                else
                    workingBlock = workingBlock.getRelative(BlockFace.DOWN);
            }
        }

        private void revertBlocks() {
            for (Location location : locations) {
                location.getBlock().setType(Material.AIR);
                changedBlocks.remove(location);
            }

            locations.clear();
        }
    }
}
