package com.herocraftonline.heroes.characters.skill.reborn.bloodmage;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.BloodUnionEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillBloodGift extends TargettedSkill {

    public SkillBloodGift(Heroes plugin) {
        super(plugin, "BloodGift");
        setDescription("You gift an ally with your own blood, restoring $1 of their health. " +
                "Healing is increased by $4% per level of Blood Union. " +
                "This ability cannot be used on yourself, and costs $2 health and $3 mana to use.");
        setUsage("/skill bloodgift <target>");
        setIdentifiers("skill bloodgift");
        setArgumentRange(0, 1);
        setTypes(SkillType.HEALING, SkillType.SILENCEABLE, SkillType.NO_SELF_TARGETTING, SkillType.DEFENSIVE_NAME_TARGETTING_ENABLED,
                SkillType.ABILITY_PROPERTY_DARK, SkillType.ABILITY_PROPERTY_LIGHT);
    }

    public String getDescription(Hero hero) {
        double healthCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST, 85.0, false);
        int manacost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 110, false);

        double healing = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.HEALING, false);
        double healIncreasePerBloodUnion = SkillConfigManager.getUseSetting(hero, this, "heal-increase-percent-per-blood-union", 0.04, false) * 100;

        return getDescription()
                .replace("$1", Util.decFormat.format(healing))
                .replace("$2", Util.decFormat.format(healIncreasePerBloodUnion))
                .replace("$3", Util.decFormat.format(healthCost))
                .replace("$4", manacost + "");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 10.0);
        config.set("health-increase-percent-per-blood-union", 0.04);
        config.set(SkillSetting.HEALING.node(), 130.0);
        config.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 0.0);
        config.set(SkillSetting.HEALTH_COST.node(), 85.0);
        config.set(SkillSetting.MANA.node(), 110);
        return config;
    }

    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        if (!(target instanceof Player)) {
            return SkillResult.INVALID_TARGET;
        }

        Player player = hero.getPlayer();
        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);

        // Check to see if they are at full health
        double targetHealth = target.getHealth();
        if (targetHealth >= target.getMaxHealth()) {
            player.sendMessage("    " + ChatComponents.GENERIC_SKILL + "Target is already at full health.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        broadcastExecuteText(hero, target);

        double healing = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.HEALING, false);
        healing = getScaledHealing(hero, healing);

        // Get Blood Union Level
        int bloodUnionLevel = 0;
        if (hero.hasEffect(BloodUnionEffect.unionEffectName)) {
            BloodUnionEffect buEffect = (BloodUnionEffect) hero.getEffect(BloodUnionEffect.unionEffectName);
            bloodUnionLevel = buEffect.getBloodUnionLevel();
        }

        // Increase healing based on blood union level
        double healIncrease = SkillConfigManager.getUseSetting(hero, this, "health-increase-percent-per-blood-union", 0.04, false);
        healing *= 1 + healIncrease * bloodUnionLevel;

        if (!targetHero.tryHeal(hero, this, healing))
            return SkillResult.CANCELLED;

        return SkillResult.NORMAL;
    }
}