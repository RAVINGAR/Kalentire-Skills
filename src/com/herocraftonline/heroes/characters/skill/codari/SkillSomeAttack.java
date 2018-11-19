package com.herocraftonline.heroes.characters.skill.codari;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.standard.SlownessEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseWeaponImbue;
import com.herocraftonline.heroes.nms.physics.NMSPhysics;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.EnumSet;

public class SkillSomeAttack extends SkillBaseWeaponImbue {

    // TODO Find a unified place for this for multipul skills
    private static final EnumSet<Material> shovels = EnumSet.of(Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL, Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL);

    private static final String PERCENT_DAMAGE_INCREASE_NODE = "percent-damage-increase";
    private static final double DEFAULT_PERCENT_DAMAGE_INCREASE = 0.1;

    private static final String PRIMARY_ATTACK_KNOCKBACK_FORCE_NODE = "primary-attack-knockback-force";
    private static final double DEFAULT_PRIMARY_ATTACK_KNOCKBACK_FORCE = 2.5;

    private static final String SECONDARY_ATTACK_SLOWNESS_DURATION_NODE = "secondary-attack-slowness-duration";
    private static final int DEFAULT_SECONDARY_ATTACK_SLOWNESS_DURATION = 5000;

    private static final String SECONDARY_ATTACK_SLOWNESS_STRENGTH_NODE = "secondary-attack-slowness-strength";
    private static final int DEFAULT_SECONDARY_ATTACK_SLOWNESS_STRENGTH = 1;

    public SkillSomeAttack(Heroes plugin) {
        super(plugin, "SomeAttack");
        setDescription("Deal $1% increased damage with the next basic attack. Activating this skill has a stamina cost of $2" +
                "and a cooldown of $3 second(s).");

        setUsage("/skill " + getName());
        setArgumentRange(0, 0);
        setIdentifiers("skill " + getName());

        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.AGGRESSIVE, SkillType.DAMAGING, SkillType.FORCE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {

        ConfigurationSection node = super.getDefaultConfig();

        node.set(PERCENT_DAMAGE_INCREASE_NODE, DEFAULT_PERCENT_DAMAGE_INCREASE);
        node.set(PRIMARY_ATTACK_KNOCKBACK_FORCE_NODE, DEFAULT_PRIMARY_ATTACK_KNOCKBACK_FORCE);
        node.set(SECONDARY_ATTACK_SLOWNESS_DURATION_NODE, DEFAULT_SECONDARY_ATTACK_SLOWNESS_DURATION);
        node.set(SECONDARY_ATTACK_SLOWNESS_STRENGTH_NODE, DEFAULT_SECONDARY_ATTACK_SLOWNESS_STRENGTH);

        return node;
    }

    @Override
    public String getDescription(Hero hero) {

//        double percentDamageIncrease = SkillConfigManager.getUseSetting(hero, this, PERCENT_DAMAGE_INCREASE_NODE, DEFAULT_PERCENT_DAMAGE_INCREASE, false);
//        if (percentDamageIncrease < 0) {
//            percentDamageIncrease = 0;
//        }
//
//        int staminaCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA, DEFAULT_STAMINA_COST, true);
//        if (staminaCost < 0) {
//            staminaCost = 0;
//        }
//
//        int cooldown = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, DEFAULT_COOLDOWN, true);
//        if (cooldown < 0) {
//            cooldown = 0;
//        }
//
//        String percentDamageIncreaseParam = Util.smallDecFormat.format(percentDamageIncrease * 100);
//        String manaCostParam = Integer.toString(staminaCost);
//        String cooldownParam = Util.smallDecFormat.format(cooldown / 1000.0);
//
//        return Messaging.parameterizeMessage(getDescription(), percentDamageIncreaseParam, manaCostParam, cooldownParam);

        return getDescription();
    }

    @Override
    public boolean canApply(Hero hero, String[] args, Material weapon, boolean projectile) {
        return shovels.contains(weapon);
    }

    @Override
    protected void apply(Hero hero, String[] args, WeaponDamageEvent e) {

        SkillExtraPointyStickAttack thatExtraAttackSkill = (SkillExtraPointyStickAttack) plugin.getSkillManager().getSkill(SkillExtraPointyStickAttack.NAME);

        double percentDamageIncrease = SkillConfigManager.getUseSetting(hero, this, PERCENT_DAMAGE_INCREASE_NODE, DEFAULT_PERCENT_DAMAGE_INCREASE, false);
        if (percentDamageIncrease < 0) {
            percentDamageIncrease = 0;
        }

        double extraDamage = e.getDamage() * percentDamageIncrease;
        e.setDamage(e.getDamage() + extraDamage);

        Player player = hero.getPlayer();
        Vector playerCenter = NMSPhysics.instance().getEntityAABB(player).getCenter();

        LivingEntity target = (LivingEntity) e.getEntity();
        CharacterTemplate targetCharacter = plugin.getCharacterManager().getCharacter(target);
        Vector targetCenter = NMSPhysics.instance().getEntityAABB(target).getCenter();

        if (thatExtraAttackSkill != null && thatExtraAttackSkill.getHitRange() != SkillExtraPointyStickAttack.HitRange.NO_HIT) {
            // Secondary attack
            int secondaryAttackSlownessDuration = SkillConfigManager.getUseSetting(hero, this, SECONDARY_ATTACK_SLOWNESS_DURATION_NODE, DEFAULT_SECONDARY_ATTACK_SLOWNESS_DURATION, false);
            int secondaryAttackSlownessStrength = SkillConfigManager.getUseSetting(hero, this, SECONDARY_ATTACK_SLOWNESS_STRENGTH_NODE, DEFAULT_SECONDARY_ATTACK_SLOWNESS_STRENGTH, false);

            if (secondaryAttackSlownessDuration > 0 && secondaryAttackSlownessStrength > 0) {
                SlownessEffect.addDuration(targetCharacter, this, player, secondaryAttackSlownessDuration, secondaryAttackSlownessStrength);
            }
        } else {
            // Primary Attack
            double primaryAttackKnockback = SkillConfigManager.getUseSetting(hero, this, PRIMARY_ATTACK_KNOCKBACK_FORCE_NODE, DEFAULT_PRIMARY_ATTACK_KNOCKBACK_FORCE, false);
            if (primaryAttackKnockback > 0) {
                target.setVelocity(target.getVelocity().add(targetCenter.clone().subtract(playerCenter).normalize().multiply(primaryAttackKnockback)));
            }
        }

    }
}
