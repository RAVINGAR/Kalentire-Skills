package com.herocraftonline.heroes.characters.skill.skills;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.util.BlockIterator;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillIteratorTest extends ActiveSkill {
    private static Set<Location> changedBlocks = new HashSet<Location>();

    public SkillIteratorTest(Heroes plugin) {
        super(plugin, "IteratorTest");
        setDescription("Delf's block test");
        setUsage("/skill iteratortest <Test>");
        setArgumentRange(1, 1);
        setIdentifiers("skill iteratortest");
        setTypes(SkillType.BLOCK_CREATING);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 8);
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(5000));

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);

        int type = Integer.parseInt(args[0]);

        ShieldWallEffect swEffect = new ShieldWallEffect(this, player, duration, type);
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
        private List<Location> locations = new ArrayList<Location>();
        private int type = 1;

        public ShieldWallEffect(Skill skill, Player applier, long duration, int type) {
            super(skill, "IteratorTest", applier, duration);

            this.type = type;
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            Player player = hero.getPlayer();

            int distance = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.MAX_DISTANCE, 6, false);

            Block b;
            BlockIterator iter = null;
            try {
                iter = new BlockIterator(player, distance);
            }
            catch (IllegalStateException e) {
                Messaging.send(player, "Errors yo.");
            }

            while (iter.hasNext()) {
                b = iter.next();
                if (type == 1) {
                    if (Util.transparentBlocks.contains(b.getType()) && (Util.transparentBlocks.contains(b.getRelative(BlockFace.UP).getType()) || Util.transparentBlocks.contains(b.getRelative(BlockFace.DOWN).getType()))) {
                        Location loc = b.getLocation();
                        changedBlocks.add(loc);
                        locations.add(loc);
                        b.setType(Material.GOLD_BLOCK);
                    }
                    else {
                        break;
                    }
                }
                else if (type == 2) {
                    if (Util.transparentBlocks.contains(b.getType()) && (Util.transparentBlocks.contains(b.getRelative(BlockFace.UP).getType()))) {
                        Location loc = b.getLocation();
                        changedBlocks.add(loc);
                        locations.add(loc);
                        b.setType(Material.GOLD_BLOCK);
                    }
                    else {
                        break;
                    }
                }
                else if (type == 3) {
                    if (Util.transparentBlocks.contains(b.getType())) {
                        Location loc = b.getLocation();
                        changedBlocks.add(loc);
                        locations.add(loc);
                        b.setType(Material.GOLD_BLOCK);
                    }
                    else {
                        break;
                    }
                }
                else if (type == 4) {
                    if (Util.transparentBlocks.contains(b.getRelative(BlockFace.UP).getType())) {
                        Location loc = b.getLocation();
                        changedBlocks.add(loc);
                        locations.add(loc);
                        b.setType(Material.GOLD_BLOCK);
                    }
                    else {
                        break;
                    }
                }
                else if (type == 5) {
                    if (Util.transparentBlocks.contains(b.getRelative(BlockFace.DOWN).getType())) {
                        Location loc = b.getLocation();
                        changedBlocks.add(loc);
                        locations.add(loc);
                        b.setType(Material.GOLD_BLOCK);
                    }
                    else {
                        break;
                    }
                }
                else if (type == 6) {
                    Block tBlock = player.getTargetBlock(null, distance);
                    Location loc = tBlock.getLocation();
                    changedBlocks.add(loc);
                    locations.add(loc);
                    tBlock.setType(Material.GOLD_BLOCK);
                }
                else {
                    Location loc = b.getLocation();
                    changedBlocks.add(loc);
                    locations.add(loc);
                    b.setType(Material.GOLD_BLOCK);
                }
            }
        }

        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            revertBlocks();
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