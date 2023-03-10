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
import com.herocraftonline.heroes.util.Util;
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
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.material.Vine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SkillRampartVine extends ActiveSkill {

    private static final int VINE_NORTH = 0x4;
    private static final int VINE_EAST = 0x8;
    private static final int VINE_WEST = 0x2;
    private static final int VINE_SOUTH = 0x1;
    private static final Set<Location> changedBlocks = new HashSet<>();
    private String applyText;
    private String expireText;

    public SkillRampartVine(final Heroes plugin) {
        super(plugin, "RampartVine");
        setDescription("Create an a series of ramparting vines at your target location.");
        setUsage("/skill rampartvine");
        setArgumentRange(0, 0);
        setIdentifiers("skill rampartvine");
        setTypes(SkillType.ABILITY_PROPERTY_EARTH, SkillType.SILENCEABLE, SkillType.BLOCK_CREATING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillBlockListener(), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 12);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), 0.2);
        node.set(SkillSetting.DURATION.node(), 15000);
        node.set("max-growth-distance", 30);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% creates an overgrowth of vines!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero%'s vines have withered.");

        return node;
    }

    @Override
    public String getDescription(final Hero hero) {

        return getDescription();
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% creates an overgrowth of vines!").replace("%hero%", "$1").replace("$hero$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero%'s vines have withered.").replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        int maxDist = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 12, false);
        final double maxDistIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT, 0.75, false);
        maxDist += (int) (hero.getAttributeValue(AttributeType.INTELLECT) * maxDistIncrease);

        final List<Block> lastBlocks = player.getLastTwoTargetBlocks((Set<Material>) null, maxDist);

        if (lastBlocks.size() < 2) {
            return SkillResult.INVALID_TARGET;
        }

        // Must place on solid block.
        if (Util.transparentBlocks.contains(lastBlocks.get(1).getType())) {
            return SkillResult.INVALID_TARGET;
        }

        // Can only grow on empty blocks.
        final Block placementBlock = lastBlocks.get(0);
        switch (placementBlock.getType()) {
            case SNOW:
            case VINE:
            case AIR:
                break;
            default:
                return SkillResult.INVALID_TARGET;
        }

        // Check the first block below the target block to ensure we can grow at least a little bit.
        switch (placementBlock.getRelative(BlockFace.DOWN).getType()) {
            case SNOW:
            case VINE:
            case AIR:
                break;
            default:
                return SkillResult.INVALID_TARGET;
        }

        final int maxGrowthDistance = SkillConfigManager.getUseSetting(hero, this, "max-growth-distance", 30, false);
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);

        final BlockFace placementFace = lastBlocks.get(0).getFace(lastBlocks.get(1));
        final RampartVineEffect oEffect = new RampartVineEffect(this, player, duration, placementBlock, placementFace, maxGrowthDistance);
        hero.addEffect(oEffect);

        return SkillResult.NORMAL;
    }

    public static class SkillBlockListener implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onBlockBreak(final BlockBreakEvent event) {
            if (changedBlocks.contains(event.getBlock().getLocation())) {
                event.setCancelled(true);
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onBlockSpread(final BlockSpreadEvent event) {
            final Block sourceBlock = event.getSource();
            final Location sourceLocation = sourceBlock.getLocation();

            if (sourceBlock.getType() == Material.VINE && changedBlocks.contains(sourceLocation)) {
                event.setCancelled(true);
            }
        }
    }

    public class RampartVineEffect extends ExpirableEffect {
        private final Block targetBlock;
        private final BlockFace targetFace;
        private final int maxGrowth;
        private final List<Location> locations = new ArrayList<>();

        public RampartVineEffect(final Skill skill, final Player applier, final long duration, final Block targetBlock, final BlockFace targetFace, final int maxGrowth) {
            super(skill, "RampartedVine", applier, duration);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.EARTH);

            this.targetBlock = targetBlock;
            this.targetFace = targetFace;
            this.maxGrowth = maxGrowth;
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);

            final Player player = hero.getPlayer();

            growVines();

            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);

            final Player player = hero.getPlayer();

            revertBlocks();

            broadcast(player.getLocation(), "    " + expireText, player.getName());
        }

        private void growVines() {
            boolean breakLoop = false;
            Block workingBlock = targetBlock;
            Location location = workingBlock.getLocation();
            location.getWorld().playSound(location, Sound.BLOCK_GRASS_HIT, 0.8F, 1.0F);

            for (int i = 0; i < maxGrowth; i++) {
                switch (workingBlock.getType()) {
                    case SNOW:
                    case AIR:
                        changedBlocks.add(location);
                        locations.add(location);

                        byte data = 0;
                        if (targetFace == BlockFace.WEST) {
                            data |= VINE_WEST;
                        } else if (targetFace == BlockFace.NORTH) {
                            data |= VINE_NORTH;
                        } else if (targetFace == BlockFace.SOUTH) {
                            data |= VINE_SOUTH;
                        } else if (targetFace == BlockFace.EAST) {
                            data |= VINE_EAST;
                        }

                        //FIXME Check if this works
//                        workingBlock.setTypeIdAndData(Material.VINE.getId(), data, false);
                        workingBlock.setType(Material.VINE);
                        if (workingBlock.getState().getData() instanceof Vine) {
                            final Vine vine = (Vine) workingBlock.getState().getData();
                            vine.putOnFace(targetFace);
                            workingBlock.getState().setData(vine);
                        }

                        break;
                    case VINE:
                        break;      // Leave vines alone, and let the spell continue even with them.
                    default:
                        breakLoop = true;
                        break;
                }
                if (breakLoop) {
                    break;
                } else {
                    workingBlock = workingBlock.getRelative(BlockFace.DOWN);
                    location = workingBlock.getLocation();
                }
            }
        }

        private void revertBlocks() {
            for (final Location location : locations) {
                location.getBlock().setType(Material.AIR);
                changedBlocks.remove(location);
            }

            locations.clear();
        }
    }
}
