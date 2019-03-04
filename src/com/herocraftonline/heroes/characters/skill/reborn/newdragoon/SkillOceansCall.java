package com.herocraftonline.heroes.characters.skill.reborn.newdragoon;

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


import java.util.ArrayList;
import java.util.List;

public class SkillOceansCall extends ActiveSkill {

    String applyText;

    List<Block> circleBlocks = new ArrayList<>();
    List<Material> oldBlocks = new ArrayList<>();

    public SkillOceansCall(Heroes plugin) {
        super(plugin, "OceansCall");
        setUsage("/skill oceanscall");
        setIdentifiers("skill oceanscall");
        setArgumentRange(0, 0);
        setDescription("Spawn an ocean...");
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

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL
                + "%hero% used OceansCall!")
                .replace("%hero%", "$2");
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

    public void CreateSphere(int radius, Player player) {
        List<Location> blockLocations = Util.getCircleLocationList(player.getLocation(), radius, radius, false, true, 0);
        World world = player.getWorld();

        for(Location loc: blockLocations) {
            Block block = world.getBlockAt(loc);
            oldBlocks.add(block.getType());

            block.setType(Material.WATER);
            circleBlocks.add(block);
        }
        blockLocations.clear();
    }

    public void RemoveSphere(Player player) {
        World world = player.getWorld();

        int i = 0;
        for(Block b : circleBlocks) {
            Block block = world.getBlockAt(b.getLocation());
            block.setType(oldBlocks.get(i));
            i++;
        }

        this.circleBlocks.clear();
        this.oldBlocks.clear();
    }

    public class OceanExpireEffect extends ExpirableEffect {

        private final int radius;
        private final Player applier;
        private SkillBlockListener listener = new SkillBlockListener();

        OceanExpireEffect(Skill skill, Player applier, long duration, int radius) {
            super(skill, "OceanExpireEffect", applier, duration);

            this.radius = radius;
            this.applier = applier;
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            Bukkit.getServer().getPluginManager().registerEvents(listener, plugin);

            applier.sendMessage("Creating sphere!");

            CreateSphere(radius, applier);
        }

        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            applier.sendMessage("Removing Sphere!");

            RemoveSphere(applier);
        }

        public class SkillBlockListener implements Listener {

            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void onBlockFromTo(BlockFromToEvent event) {
                Block fromBlock = event.getBlock();
                Block toBlock = event.getToBlock();
                if (circleBlocks.contains(toBlock) || circleBlocks.contains(fromBlock))
                    event.setCancelled(true);
            }
        }

    }

}
