package com.herocraftonline.heroes.characters.skill.codari;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.standard.BleedingEffect;
import com.herocraftonline.heroes.characters.effects.standard.DeepWoundEffect;
import com.herocraftonline.heroes.characters.effects.standard.SlownessEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseWeaponImbue;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;

import java.util.EnumSet;

public class SkillClieve extends SkillBaseWeaponImbue {

    // TODO Find a unified place for this for multipul skills
    private static final EnumSet<Material> shovels = EnumSet.of(Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL, Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL);

    private static final String PRIMARY_ATTACK_BASE_EXECUTE_HEALTH_PERCENTAGE_NODE = "primary-attack-base-execute-health-percentage";
    private static final double DEFAULT_PRIMARY_ATTACK_BASE_EXECUTE_HEALTH_PERCENTAGE = 0.05;

    private static final String PRIMARY_ATTACK_ADDED_EXECUTE_HEALTH_PERCENTAGE_PER_BLEEDING_STACK_NODE = "primary-attack-added-execute-health-percentage-per-bleeding-stack";
    private static final double DEFAULT_PRIMARY_ATTACK_ADDED_EXECUTE_HEALTH_PERCENTAGE_PER_BLEEDING_STACK = 0.02;

    private static final String SECONDARY_ATTACK_DEEP_WOUND_DURATION_NODE = "secondary-attack-deep-wound-duration";
    private static final int DEFAULT_SECONDARY_ATTACK_DEEP_WOUND_DURATION = 4000;

    public SkillClieve(Heroes plugin) {
        super(plugin, "Clieve");
        setDescription("Stuff");

        setUsage("/skill " + getName());
        setArgumentRange(0, 0);
        setIdentifiers("skill " + getName());
    }

    @Override
    public ConfigurationSection getDefaultConfig() {

        ConfigurationSection node = super.getDefaultConfig();

        node.set(PRIMARY_ATTACK_BASE_EXECUTE_HEALTH_PERCENTAGE_NODE, DEFAULT_PRIMARY_ATTACK_BASE_EXECUTE_HEALTH_PERCENTAGE);
        node.set(PRIMARY_ATTACK_ADDED_EXECUTE_HEALTH_PERCENTAGE_PER_BLEEDING_STACK_NODE, DEFAULT_PRIMARY_ATTACK_ADDED_EXECUTE_HEALTH_PERCENTAGE_PER_BLEEDING_STACK);
        node.set(SECONDARY_ATTACK_DEEP_WOUND_DURATION_NODE, DEFAULT_SECONDARY_ATTACK_DEEP_WOUND_DURATION);

        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public boolean canApply(Hero hero, String[] args, Material weapon, boolean projectile) {
        return shovels.contains(weapon);
    }

    @Override
    protected void apply(Hero hero, String[] strings, WeaponDamageEvent weaponDamageEvent) {

        if (weaponDamageEvent.getEntity() instanceof LivingEntity) {

            SkillExtraPointyStickAttack thatExtraAttackSkill = (SkillExtraPointyStickAttack) plugin.getSkillManager().getSkill(SkillExtraPointyStickAttack.NAME);

            LivingEntity target = (LivingEntity) weaponDamageEvent.getEntity();
            CharacterTemplate targetCharacter = plugin.getCharacterManager().getCharacter(target);

            if (thatExtraAttackSkill != null && thatExtraAttackSkill.getHitRange() != SkillExtraPointyStickAttack.HitRange.NO_HIT) {
                // Secondary Attack
                int secondaryAttackDeepWoundDuration = SkillConfigManager.getUseSetting(hero, this, SECONDARY_ATTACK_DEEP_WOUND_DURATION_NODE, DEFAULT_SECONDARY_ATTACK_DEEP_WOUND_DURATION, false);
                if (secondaryAttackDeepWoundDuration > 0) {
                    DeepWoundEffect.addDuration(targetCharacter, this, hero.getPlayer(), secondaryAttackDeepWoundDuration);
                }
            } else {
                // Primary Attack
                double primaryAttackbaseExecutePercentage = SkillConfigManager.getUseSetting(hero, this,
                        PRIMARY_ATTACK_BASE_EXECUTE_HEALTH_PERCENTAGE_NODE, DEFAULT_PRIMARY_ATTACK_BASE_EXECUTE_HEALTH_PERCENTAGE, false);
                if (primaryAttackbaseExecutePercentage < 0) {
                    primaryAttackbaseExecutePercentage = 0;
                }

                double primaryAttackAddedExecutePercentageBerBleedingStack = SkillConfigManager.getUseSetting(hero, this,
                        PRIMARY_ATTACK_ADDED_EXECUTE_HEALTH_PERCENTAGE_PER_BLEEDING_STACK_NODE, DEFAULT_PRIMARY_ATTACK_ADDED_EXECUTE_HEALTH_PERCENTAGE_PER_BLEEDING_STACK, false);
                if (primaryAttackAddedExecutePercentageBerBleedingStack < 0) {
                    primaryAttackAddedExecutePercentageBerBleedingStack = 0;
                }

                double healthPercentageResult = primaryAttackbaseExecutePercentage + (primaryAttackAddedExecutePercentageBerBleedingStack + BleedingEffect.getStackCount(targetCharacter));

                if (target.getHealth() / target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() <= healthPercentageResult) {
                    // Execute
                    weaponDamageEvent.setCancelled(true);
                    target.setHealth(0);
                }
            }
        }
    }
}
