package com.herocraftonline.heroes.characters.skill.reborn.dragoon;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.ncp.NCPFunction;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.logging.Level;

public class SkillTumble extends PassiveSkill {

    public SkillTumble(Heroes plugin) {
        super(plugin, "Tumble");
        setDescription("$1");
        setEffectTypes(EffectType.BENEFICIAL, EffectType.PHYSICAL);
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        String description = "";

        double distance = SkillConfigManager.getUseSetting(hero, this, "base-distance", 3, false);
        double perLevel = SkillConfigManager.getUseSetting(hero, this, "distance-increase-per-level", 0.16, false);
        double perDex = SkillConfigManager.getUseSetting(hero, this, "distance-increase-per-dexterity-level", 0.16, false);
        distance += hero.getAttributeValue(AttributeType.DEXTERITY) * perDex;
        distance += hero.getHeroLevel() * perLevel;

        if (distance == 3)
            description = "You aren't very good at breaking your fall, and will take full fall damage when falling down a block height greater than 3.";
        else if (distance > 0 && distance < 3)
            description = "You are terrible at bracing yourself, and will take " + Util.decFormat.format(3 - distance) + " additional blocks of fall damage when falling down a block height greater than 3!";
        else if (distance < 0)
            description = "You are extremely terrible at bracing yourself, and will take an additional " + Util.decFormat.format(3 + (distance * -1)) + " blocks of fall damage when falling down a block height greater than 3!";
        else
            description = "You are adept at bracing yourself, and will only take fall damage when falling down a block height greater than " + Util.decFormat.format(distance) + "!";

        return getDescription()
                .replace("$1", description);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("base-distance", 0);
        config.set("distance-increase-per-level", 0.16);
        config.set("distance-increase-per-dexterity-level", 0.16);
        config.set("ncp-exemption-duration", 0);
        return config;
    }

    public class SkillEntityListener implements Listener {

        private Skill skill;

        SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void onEntityDamage(final EntityDamageEvent event) {
            if (!(event.getEntity() instanceof Player) || event.getCause() != DamageCause.FALL) {
                return;
            }

            Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (!hero.hasEffect("Tumble") || hero.hasEffectType(EffectType.SAFEFALL)) {
                return;
            }

            double distance = SkillConfigManager.getUseSetting(hero, skill, "base-distance", 3, false);
            double perLevel = SkillConfigManager.getUseSetting(hero, skill, "distance-increase-per-level", 0.16, false);
            double perDex = SkillConfigManager.getUseSetting(hero, skill, "distance-increase-per-dexterity-level", 0.16, false);
            distance += hero.getAttributeValue(AttributeType.DEXTERITY) * perDex;
            distance += hero.getHeroLevel() * perLevel;

            double fallDistance = event.getDamage();
            fallDistance -= distance;

            final double fallDamage = fallDistance;

            NCPUtils.applyExemptions(event.getEntity(), new NCPFunction() {
                @Override
                public void execute() {
                    if (fallDamage <= 0) {
                        event.setCancelled(true);
                    } else {
                        event.setDamage(fallDamage);
                    }
                }
            }, Lists.newArrayList("MOVING"), SkillConfigManager.getUseSetting(hero, skill, "ncp-exemption-duration", 0, false));
        }
    }
}
