package com.herocraftonline.heroes.characters.skill.reborn.shared;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
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

public class SkillTumble extends PassiveSkill {
    private static final int BASE_FALL_DISTANCE = 3;

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

        int distance = BASE_FALL_DISTANCE + SkillConfigManager.getScaledUseSettingInt(hero, this, "extra-distance", false);

        if (distance == BASE_FALL_DISTANCE) {
            description = "You aren't very good at breaking your fall, and will take full fall damage when falling down a block height greater than 3.";
        } else if (distance > 0 && distance < BASE_FALL_DISTANCE) {
            description = "You are terrible at bracing yourself, and will take " + Util.decFormat.format(BASE_FALL_DISTANCE - distance) +
                    " additional blocks of fall damage when falling down a block height greater than 3!";
        } else if (distance < 0) {
            description = "You are extremely terrible at bracing yourself, and will take an additional " +
                    Util.decFormat.format(BASE_FALL_DISTANCE + (distance * -1)) +
                    " blocks of fall damage when falling down a block height greater than 3!";
        } else {
            description = "You are adept at bracing yourself, and will only take fall damage when falling down a block height greater than " + Util.decFormat.format(distance) + "!";
        }

        return getDescription()
                .replace("$1", description);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("extra-distance", 0);
        config.set("extra-distance-per-level", 0.16);
        config.set("extra-distance-per-dexterity-level", 0.16);
        config.set("ncp-exemption-duration", 0);
        return config;
    }

    public class SkillEntityListener implements Listener {

        private Skill skill;

        SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        // Do not change priority. We need LOWEST so that we apply damage changes before Heroes scales it to "hero" numbers.
        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onEntityDamage(final EntityDamageEvent event) {
            if (!(event.getEntity() instanceof Player) || event.getCause() != DamageCause.FALL) {
                return;
            }

            Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (!hero.hasEffect(skill.getName()) || hero.hasEffectType(EffectType.SAFEFALL)) {
                return;
            }

            int damageReduction = SkillConfigManager.getScaledUseSettingInt(hero, skill, "extra-distance", false);

            double fallDistance = event.getDamage();
            fallDistance -= damageReduction;

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
