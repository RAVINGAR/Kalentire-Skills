package com.herocraftonline.heroes.characters.skill.remastered.pyromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.CharacterDamageEvent;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

/**
 * Created By MysticMight 2021
 */

public class SkillLavaSkin extends PassiveSkill {

    public SkillLavaSkin(Heroes plugin) {
        super(plugin, "LavaSkin");
        setDescription("Take $1% reduced damage from fire.");
        setTypes(SkillType.ABILITY_PROPERTY_FIRE, SkillType.BUFFING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double fireMultiplier = SkillConfigManager.getUseSettingDouble(hero, this, "fire-multiplier", false);
        double lavaMultiplier = SkillConfigManager.getUseSettingDouble(hero, this, "lava-multiplier", false);
        double skillMultiplier = SkillConfigManager.getUseSettingDouble(hero, this, "skill-multiplier", false);
        //todo do we want to use the other multipliers?

        return getDescription().replace("$1", Util.decFormat.format((1 - fireMultiplier) * 100.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.APPLY_TEXT.node(), "");
        config.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        config.set("fire-multiplier", 0.9);
        config.set("skill-multiplier", 0.9);
        config.set("lava-multiplier", 0.9);
        return config;
    }

    public class SkillHeroListener implements Listener {
        private final PassiveSkill skill;

        public SkillHeroListener(PassiveSkill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEnvironmentDamage(CharacterDamageEvent event) {
            if (!(event.getEntity() instanceof Player) || event.getDamage() == 0)
                return;

            // Only handle Fire or Lava damage
            if (event.getCause() != DamageCause.FIRE && event.getCause() != DamageCause.FIRE_TICK
                    && event.getCause() != DamageCause.LAVA)
                return;

            final Player player = (Player) event.getEntity();
            final Hero hero = plugin.getCharacterManager().getHero(player);
            if (!skill.hasPassive(hero))
                return;

            double fireMultiplier = SkillConfigManager.getUseSettingDouble(hero, skill, "fire-multiplier", false);
            double lavaMultiplier = SkillConfigManager.getUseSettingDouble(hero, skill, "lava-multiplier", false);

            double damageMultiplier = event.getCause() == DamageCause.LAVA ? lavaMultiplier : fireMultiplier;
            event.setDamage(damageMultiplier * event.getDamage());
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {
            if (!(event.getEntity() instanceof Player) || event.getDamage() == 0)
                return;

            final Skill dSkill = event.getSkill();
            if (!dSkill.isType(SkillType.ABILITY_PROPERTY_FIRE))
                return;

            final Player player = (Player) event.getEntity();
            final Hero hero = plugin.getCharacterManager().getHero(player);
            if (!this.skill.hasPassive(hero))
                return;

            double skillMultiplier = SkillConfigManager.getUseSettingDouble(hero, skill, "skill-multiplier", false);
            event.setDamage(skillMultiplier * event.getDamage());
        }
    }
}
