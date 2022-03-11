package com.herocraftonline.heroes.characters.skill.reborn;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;


import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SkillOceansCall extends ActiveSkill {

    private Set<Location> globallyManagedLocations = new HashSet<Location>();

    public SkillOceansCall(Heroes plugin) {
        super(plugin, "OceansCall");
        setUsage("/skill oceanscall");
        setIdentifiers("skill oceanscall");
        setArgumentRange(0, 0);
        setDescription("Spawn an ocean...");

        Bukkit.getServer().getPluginManager().registerEvents(new SkillBlockListener(), plugin);
    }

    public String getDescription(Hero hero) {
        return getDescription();
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection cs = super.getDefaultConfig();

        cs.set(SkillSetting.RADIUS.node(), 10);
        cs.set(SkillSetting.DURATION.node(), 60000);

        return cs;
    }

    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 10, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 60000, false);

        ExpirableEffect effect = new OceanExpireEffect(this, player, duration, radius);

        hero.addEffect(effect);

        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    public class OceanExpireEffect extends ExpirableEffect {

        private Set<Block> changedBlocks = new HashSet<Block>();
        private final int radius;

        OceanExpireEffect(Skill skill, Player applier, long duration, int radius) {
            super(skill, "OceanExpireEffect", applier, duration);

            this.radius = radius;
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            applier.sendMessage("Creating sphere!");

            CreateSphere(radius, applier);
        }

        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            applier.sendMessage("Removing Sphere!");

            RemoveSphere(applier);
        }

        public void CreateSphere(int radius, Player player) {
            List<Location> blockLocations = Util.getCircleLocationList(player.getLocation(), radius, radius, false, true, 0);
            World world = player.getWorld();

            for (Location loc : blockLocations) {
                Block block = world.getBlockAt(loc);

                if (block.isEmpty()) {
                    block.setType(Material.WATER);
                    changedBlocks.add(block);
                }
                globallyManagedLocations.add(block.getLocation());
            }
            blockLocations.clear();
        }

        public void RemoveSphere(Player player) {
            World world = player.getWorld();

            for (Block block : changedBlocks) {
                if (block.getType() == Material.WATER) {
                    block.setType(Material.AIR);
                }
                globallyManagedLocations.remove(block.getLocation());
            }
            changedBlocks.clear();
        }
    }

    public class SkillBlockListener implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onBlockPlace(BlockPlaceEvent event) {
            Block block = event.getBlock();
            if (block != null && globallyManagedLocations.contains(block.getLocation())) {
                event.setCancelled(true);
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onBlockBreak(BlockBreakEvent event) {
            Block block = event.getBlock();
            if (block != null && globallyManagedLocations.contains(block.getLocation())) {
                event.setCancelled(true);
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEntityChangeBlock(EntityChangeBlockEvent event) {
            Block block = event.getBlock();
            if (globallyManagedLocations.contains(block.getLocation()))
                event.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onBlockFromTo(BlockFromToEvent event) {
            Block fromBlock = event.getBlock();
            Block toBlock = event.getToBlock();
            if (globallyManagedLocations.contains(toBlock.getLocation()) || globallyManagedLocations.contains(fromBlock.getLocation()))
                event.setCancelled(true);
        }
    }
}
