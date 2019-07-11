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
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillBloodRitual extends TargettedSkill {

    public SkillBloodRitual(Heroes plugin) {
        super(plugin, "BloodRitual");
        setDescription("Perform a Ritual of Blood with your target, restoring $1% of your target's health per level of Blood Union. " +
                "Removes all Blood Union on use.");
        setUsage("/skill bloodritual <target>");
        setIdentifiers("skill bloodritual");
        setArgumentRange(0, 1);
        setTypes(SkillType.HEALING, SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_DARK);
    }

    public String getDescription(Hero hero) {
        double healthMultiplier = SkillConfigManager.getScaledUseSettingDouble(hero, this, "blood-union-health-multiplier", 0.1, false);
        return getDescription()
                .replace("$1", Util.decFormat.format(healthMultiplier * 100));
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 12);
        config.set("blood-union-health-multiplier", 0.059);
        config.set("blood-union-health-multiplier-increase-per-wisdom", 0.0);
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

        if (!hero.hasEffect(BloodUnionEffect.unionEffectName)) {
            player.sendMessage("    " + ChatComponents.GENERIC_SKILL + "You must have at least 1 Blood Union to use this ability!");
            return SkillResult.FAIL;
        }

        // Get Blood Union Level
        BloodUnionEffect buEffect = (BloodUnionEffect) hero.getEffect(BloodUnionEffect.unionEffectName);
        int bloodUnionLevel = buEffect.getBloodUnionLevel();
        if (bloodUnionLevel < 1) {
            player.sendMessage("    " + ChatComponents.GENERIC_SKILL + "You must have at least 1 Blood Union to use this ability!");
            return SkillResult.FAIL;
        }

        // Increase healing based on wisdom and blood union level
        double healthMultiplier = SkillConfigManager.getScaledUseSettingDouble(hero, this, "blood-union-health-multiplier", 0.1, false);
        double healAmount = healthMultiplier * bloodUnionLevel * target.getMaxHealth();

        // Ensure they can be healed.
        if (!targetHero.tryHeal(targetHero, this, healAmount))
            return SkillResult.CANCELLED;

        broadcastExecuteText(hero, target);

        buEffect.setBloodUnionLevel(0);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5F, 1.0F);
        target.getWorld().spigot().playEffect(target.getLocation(), Effect.MOBSPAWNER_FLAMES, 1, 1, 0F, 1F, 0F, 50F, 30, 10);
//        player.getWorld().spawnParticle(Particle.FLAME, target.getLocation(), 30, 0, 1, 0, 50);

        return SkillResult.NORMAL;
    }
}