package com.herocraftonline.heroes.characters.skill.reborn.chainwarden;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.reborn.chainwarden.SkillHook.InvalidHookTargetReason;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.Arrays;

public class SkillHemorrhage extends TargettedSkill {
    public static final String skillName = "Hemorrhage";

    public SkillHemorrhage(Heroes plugin) {
        super(plugin, "Hemorrhage");
        this.setDescription("You violently wrench a single hook out of a target, dealing $1 physical damage and interrupting their casting. " +
                "You must first hook a target in order to use this ability on them.");
        this.setUsage("/skill hemorrhage");
        this.setIdentifiers("skill hemorrhage");
        this.setArgumentRange(0, 1);
        this.setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.INTERRUPTING);
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 60.0, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        return getDescription()
                .replace("$1", Util.decFormat.format(damage));
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.ON_INTERRUPT_FORCE_COOLDOWN.node(), 1000);
        config.set(SkillSetting.MAX_DISTANCE.node(), 30);
        config.set(SkillSetting.DAMAGE.node(), 80);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 0.0);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        // This is necessary for compatibility with AoE versions of this skill.
        boolean shouldBroadcast = args == null || args.length == 0 || Arrays.stream(args).noneMatch(x -> x.equalsIgnoreCase("NoBroadcast"));

        InvalidHookTargetReason invalidHookTargetReason = SkillHook.tryUseHook(plugin, hero, target, true);
        if (invalidHookTargetReason != InvalidHookTargetReason.VALID_TARGET) {
            if (shouldBroadcast) {
                SkillHook.broadcastInvalidHookTargetText(hero, invalidHookTargetReason);
            }
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        if (shouldBroadcast)
            broadcastExecuteText(hero, target);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 80, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);


        // do damage
        addSpellTarget(target, hero);
        damageEntity(target, player, damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK, false);
        target.setVelocity(new Vector(0, 0, 0));

        // display removal of the hook
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SQUID_HURT, 0.4F, 1.0F);
        // 1.0F is default
        float exitParticleDisplaySpeed = 1.5F;
        player.getWorld().spawnParticle(Particle.REDSTONE, target.getEyeLocation(), 15, 0.25F, 0.15F, 0.4F, exitParticleDisplaySpeed);

        return SkillResult.NORMAL;
    }
}
