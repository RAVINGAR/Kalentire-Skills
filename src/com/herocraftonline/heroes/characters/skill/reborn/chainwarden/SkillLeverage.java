package com.herocraftonline.heroes.characters.skill.reborn.chainwarden;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class SkillLeverage extends PassiveSkill {

    public SkillLeverage(Heroes plugin) {
        super(plugin, "Leverage");
        setDescription("You gain $1% increased weapon damage for every target you have hooked with chains, up to a maximum of $2%.");
        setTypes(SkillType.ABILITY_PROPERTY_FIRE, SkillType.DAMAGING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    public String getDescription(Hero hero) {
        double increasePerHook = SkillConfigManager.getUseSetting(hero, this, "percent-increase-per-hook", 0.1, false);
        double maxIncrease = SkillConfigManager.getUseSetting(hero, this, "maximum-damage-increase", 0.4, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(increasePerHook * 100))
                .replace("$2", Util.decFormat.format(maxIncrease * 100));
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("percent-increase-per-hook", 0.1);
        config.set("maximum-damage-increase", 0.4);
        return config;
    }

    public class SkillHeroListener implements Listener {
        private Skill skill;

        public SkillHeroListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (event.getDamage() <= 0 || !(event.getDamager() instanceof Hero) || !(event.getEntity() instanceof LivingEntity))
                return;

            Hero hero = (Hero) event.getDamager();
            if (!hero.hasEffect(SkillHookshot.ownerEffectName))
                return;
            if (!hero.canUseSkill(skill))
                return;

            SkillHookshot.HookOwnerEffect ownerEffect = (SkillHookshot.HookOwnerEffect) hero.getEffect(SkillHookshot.ownerEffectName);
            if (ownerEffect == null)
                return;

            int count = ownerEffect.getCurrentHookCount();
            if (count < 1)
                return;

            double increasePerHook = SkillConfigManager.getUseSetting(hero, skill, "percent-increase-per-hook", 0.1, false);
            double maxIncrease = SkillConfigManager.getUseSetting(hero, skill, "maximum-damage-increase", 0.4, false);

            double modifier = Math.min(maxIncrease, increasePerHook * count);
            event.setDamage(event.getDamage() * (1.0 + modifier));
        }
    }
}
