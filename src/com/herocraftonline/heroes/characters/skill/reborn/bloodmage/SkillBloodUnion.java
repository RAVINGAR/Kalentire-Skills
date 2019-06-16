package com.herocraftonline.heroes.characters.skill.reborn.bloodmage;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.SkillUseEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.BloodUnionEffect;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.chat.ChatComponents;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class SkillBloodUnion extends PassiveSkill {

    public SkillBloodUnion(Heroes plugin) {
        super(plugin, "BloodUnion");
        setDescription("Your damaging abilities form a Blood Union with your opponents. " +
                "Blood Union allows you to use certain abilities, and also increases the effectiveness of others. " +
                "Maximum Blood Union is $1. " +
                "Blood Union resets upon switching from monsters to players, and will expire completely if not increased after $2 second(s).");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.APPLY_TEXT.node(), "");
        config.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        config.set(SkillSetting.PERIOD.node(), 25000);
        config.set("max-blood-union", 6);
        return config;
    }

    @Override
    public String getDescription(Hero hero) {
        double period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 25000, false) / 1000.0;
        int maxBloodUnion = SkillConfigManager.getUseSetting(hero, this, "max-blood-union", 4, false);

        return getDescription()
                .replace("$1", maxBloodUnion + "")
                .replace("$2", period + "");
    }

    @Override
    public void apply(Hero hero) {
        int bloodUnionResetPeriod = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 25000, false);
        int maxBloodUnion = SkillConfigManager.getUseSetting(hero, this, "max-blood-union", 6, false);
        hero.addEffect(new BloodUnionEffect(this, bloodUnionResetPeriod, maxBloodUnion));
    }

    @Override
    public String getPassiveEffectName() {
        return BloodUnionEffect.unionEffectName;
    }
}