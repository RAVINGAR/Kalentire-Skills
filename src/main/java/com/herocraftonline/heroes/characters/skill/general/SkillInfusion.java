package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.BloodUnionEffect;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillInfusion extends TargettedSkill {

    public SkillInfusion(Heroes plugin) {
        super(plugin, "Infusion");
        setDescription("Infuse your target with life, restoring $1 of their health and negating their bleeding. " +
                "Healing is improved by $2% per level of Blood Union. " +
                "This ability costs $3 health and $4 mana to use.");
        setUsage("/skill infusion <target>");
        setIdentifiers("skill infusion");
        setArgumentRange(0, 1);
        setTypes(SkillType.HEALING, SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_MAGICAL);
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
        config.set(SkillSetting.MAX_DISTANCE.node(), 8.0);
        config.set("heal-increase-percent-per-blood-union", 0.03);
        config.set(SkillSetting.HEALING.node(), 75.0);
        config.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 0.0);
        config.set(SkillSetting.HEALTH_COST.node(), 35.0);
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
            if (player.equals(targetHero.getPlayer())) {
                player.sendMessage("    " + ChatComponents.GENERIC_SKILL + "You are already at full health.");
            } else {
                player.sendMessage("    " + ChatComponents.GENERIC_SKILL + "Target is already at full health.");
            }

            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        broadcastExecuteText(hero, target);

        //TODO make sure to account for scaledhealing (see other SkillInfusion in pack1), probably by adding it to the new scaled config getter (for HEALING node only)
        double healing = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.HEALING, false);

        // Get Blood Union Level
        int bloodUnionLevel = 0;
        if (hero.hasEffect(BloodUnionEffect.unionEffectName)) {
            BloodUnionEffect buEffect = (BloodUnionEffect) hero.getEffect(BloodUnionEffect.unionEffectName);
            bloodUnionLevel = buEffect.getBloodUnionLevel();
        }

        // Increase healing based on blood union level
        double healIncrease = SkillConfigManager.getUseSetting(hero, this, "heal-increase-percent-per-blood-union", 0.03, false);
        healing *= 1 + healIncrease * bloodUnionLevel;

        if (!targetHero.tryHeal(hero, this, healing))
            return SkillResult.CANCELLED;

        // If we were able to heal them, also remove bleeds
        for (Effect effect : targetHero.getEffects()) {
            if (effect.isType(EffectType.BLEED) && effect.isType(EffectType.HARMFUL)) {
                targetHero.removeEffect(effect);
            }
        }
        return SkillResult.NORMAL;
    }
}