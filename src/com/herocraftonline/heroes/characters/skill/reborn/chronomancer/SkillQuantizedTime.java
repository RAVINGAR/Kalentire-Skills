package com.herocraftonline.heroes.characters.skill.reborn.chronomancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.HeroRegainManaEvent;
import com.herocraftonline.heroes.api.events.HeroRegainStaminaEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillQuantizedTime extends PassiveSkill{
    private final double healingConfig = 5;
    private final long periodConfig = 10000;

    public SkillQuantizedTime(Heroes plugin) {
        super(plugin, "QuantizedTime");
        setDescription("You regain $1 health, $3 mana and $4 stamina every $2 seconds.");
        setEffectTypes(EffectType.TEMPORAL, EffectType.BENEFICIAL, EffectType.HEALING);
    }

    @Override
    public String getDescription(Hero hero) {
        // i have no idea why i can't put periodConfig here
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 10000, false);
        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING, healingConfig, false);
        int manaRestore = SkillConfigManager.getUseSetting(hero, this, "mana-restore", 10, false);
        int staminaRestore = SkillConfigManager.getUseSetting(hero, this, "stamina-restore", 1, false);

        return super.getDescription()
                .replace("$1", Util.decFormat.format(healing))
                .replace("$2", Util.decFormat.format(period / 1000.0))
                .replace("$3", Util.decFormat.format(manaRestore))
                .replace("$4", Util.decFormat.format(staminaRestore));

    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.HEALING.node(), healingConfig);
        config.set(SkillSetting.PERIOD.node(), periodConfig);
        config.set("mana-restore", 10);
        config.set("stamina-restore", 10);
        return config;
    }

    @Override
    public void apply(Hero hero) {
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 10000, false);
        QuantizedTimeEffect effect = new QuantizedTimeEffect(this, hero.getPlayer(), period);
        hero.addEffect(effect);
    }

    public class QuantizedTimeEffect extends PeriodicEffect {
        private double healing;
        private long period;
        private int manaRestore;
        private int staminaRestore;

        QuantizedTimeEffect(Skill skill, Player player, long period) {
            super(skill, skill.getName(), player, period);

            types.add(EffectType.INTERNAL);
            setPersistent(true);
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            this.healing = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.HEALING, healingConfig, false);
            this.period = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.PERIOD, 10000, false);
            this.manaRestore = SkillConfigManager.getUseSetting(hero, skill, "mana-restore", 10, false);
            this.staminaRestore = SkillConfigManager.getUseSetting(hero, skill, "stamina-restore", 10, false);
        }

        @Override
        public void tickHero(Hero hero) {
            super.tickHero(hero);
            double maxHealth = hero.getPlayer().getMaxHealth();
            double curHealth = hero.getPlayer().getHealth();
            if (curHealth < maxHealth) {
                hero.tryHeal(null, null, healing);
            }

            if (hero.getMana() < hero.getMaxMana()) {
                HeroRegainManaEvent hrmEvent = new HeroRegainManaEvent(hero, manaRestore, skill);
                plugin.getServer().getPluginManager().callEvent(hrmEvent);
                if (!hrmEvent.isCancelled()) {
                    hero.setMana(hrmEvent.getDelta() + hero.getMana());
                    /*
                    if (hero.isVerboseMana())
                        hero.getPlayer().sendMessage(ChatComponents.Bars.mana(hero.getMana(), hero.getMaxMana(), false));
                     */
                }
            }

            HeroRegainStaminaEvent hrsEvent = new HeroRegainStaminaEvent(hero, staminaRestore, skill);
            plugin.getServer().getPluginManager().callEvent(hrsEvent);
            if (!hrsEvent.isCancelled()) {
                hero.setStamina(hrsEvent.getDelta() + hero.getStamina());
            }
        }
    }
}
