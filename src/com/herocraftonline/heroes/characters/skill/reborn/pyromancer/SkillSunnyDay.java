package com.herocraftonline.heroes.characters.skill.reborn.pyromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

import java.util.HashSet;

public class SkillSunnyDay extends ActiveSkill {

    public SkillSunnyDay(Heroes plugin) {
        super(plugin, "SunnyDay");
        setDescription("Clear the weather for a sunny day!");
        setUsage("/skill sunnyday");
        setIdentifiers("skill sunnyday");
        setArgumentRange(0, 0);

//        Bukkit.getServer().getPluginManager().registerEvents(new SkillListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        //config.set(SkillSetting.DURATION.node(), 5 * 60 * 1000);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        World world = player.getWorld();
        // player.sendMessage("    " + ChatComponents.GENERIC_SKILL + "Weather Duration: " + world.getWeatherDuration());
        if (!world.hasStorm()) {
            player.sendMessage("    " + ChatComponents.GENERIC_SKILL + "It's not storming right now!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        //player.getWorld().setWeatherDuration(0);

        broadcastExecuteText(hero);
        world.setStorm(false);

        return SkillResult.NORMAL;
    }

//    private class SunnyDayState {
//        public boolean wasRainingOnCast;
//        public long timeWasCast;
//
//    }
//
//    private class SunnyDayEffect extends ExpirableEffect {
//        public SunnyDayEffect(Skill skill, Player applier, long duration) {
//            super(skill, "SunnyDay", applier, duration);
//        }
//
//        @Override
//        public void applyToHero(Hero hero) {
//            super.applyToHero(hero);
//
//
//        }
//    }

//    private class SkillListener implements Listener {
//        private final Skill skill;
//
//        SkillListener(Skill skill) {
//            this.skill = skill;
//        }
//
//        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
//        public void onWeatherChange(WeatherChangeEvent event) {
//            boolean raining = event.toWeatherState();
//            if (raining)
//                event.setCancelled(true);
//        }
//
//        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
//        public void onThunderChange(ThunderChangeEvent event) {
//
//            boolean storm = event.toThunderState();
//            if (storm)
//                event.setCancelled(true);
//        }
//    }
}