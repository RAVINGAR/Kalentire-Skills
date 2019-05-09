package com.herocraftonline.heroes.characters.skill.reborn.chronomancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillQuantizedTime extends PassiveSkill{
    private final double healingConfig = 5;
    private final long periodConfig = 10000;

    public SkillQuantizedTime(Heroes plugin) {
        super(plugin, "QuantizedTime");
        setDescription("You regain $1 health every $2 seconds.");
        setEffectTypes(EffectType.BENEFICIAL, EffectType.HEALING);
    }

    @Override
    public String getDescription(Hero hero) {
        // i have no idea why i can't put periodConfig here
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 10000, false);
        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING, healingConfig, false);

        return super.getDescription()
                .replace("$1", Util.decFormat.format(healing))
                .replace("$2", Util.decFormat.format(period / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.HEALING.node(), healingConfig);
        config.set(SkillSetting.PERIOD.node(), periodConfig);
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

        QuantizedTimeEffect(Skill skill, Player player, long period) {
            super(skill, skill.getName(), player, period);

            types.add(EffectType.INTERNAL);
            setPersistent(true);
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            this.healing = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.HEALING, healingConfig, false);
            this.period = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.PERIOD, 10000, false);
        }

        @Override
        public void tickHero(Hero hero) {
            super.tickHero(hero);
            double maxHealth = hero.getPlayer().getMaxHealth();
            double curHealth = hero.getPlayer().getHealth();
            if (curHealth != maxHealth)
                hero.tryHeal(null, null, healing);
        }
    }
}
