package com.herocraftonline.heroes.characters.skill.remastered.dragoon;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.CharacterDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
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

public class SkillGentleLanding extends PassiveSkill {

    public SkillGentleLanding(Heroes plugin) {
        super(plugin, "GentleLanding");
        setDescription("You fall gracefully and receive $1% fall damage.");
        setTypes(SkillType.ABILITY_PROPERTY_EARTH, SkillType.BUFFING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double fallDamageMultiplier = SkillConfigManager.getUseSettingDouble(hero, this, "damage-multiplier", false);
        return getDescription().replace("$1", Util.decFormat.format(fallDamageMultiplier * 100.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.APPLY_TEXT.node(), "");
        config.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        config.set("damage-multiplier", 0.9);
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

            if (event.getCause() != DamageCause.FALL)
                return; // Only handle Fall damage

            Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (hero.hasEffectType(EffectType.SAFEFALL))
                return; // Skip, they are safe from falling already

            if (!skill.hasPassive(hero))
                return; // Handle only for those with this passive

            double fallDamageMultiplier = SkillConfigManager.getUseSettingDouble(hero, skill, "damage-multiplier", false);
            event.setDamage(fallDamageMultiplier * event.getDamage());
        }
    }
}
