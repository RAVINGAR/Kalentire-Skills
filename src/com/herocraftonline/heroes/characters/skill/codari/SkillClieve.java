package com.herocraftonline.heroes.characters.skill.codari;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.standard.StandardBleedEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseWeaponImbue;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;

import java.util.EnumSet;

public class SkillClieve extends SkillBaseWeaponImbue {

    // TODO Find a unified place for this for multipul skills
    private static final EnumSet<Material> shovels = EnumSet.of(Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL, Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL);

    private static final String BASE_EXECUTE_HEALTH_PERCENTAGE_NODE = "base-execute-health-percentage";
    private static final double DEFAULT_BASE_EXECUTE_HEALTH_PERCENTAGE = 0.05;

    private static final String ADDED_EXECUTE_HEALTH_PERCENTAGE_PER_BLEED_STACK_NODE = "added-execute-health-percentage-per-bleed-stack";
    private static final double DEFAULT_ADDED_EXECUTE_HEALTH_PERCENTAGE_PER_BLEED_STACK = 0.02;

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

        node.set(BASE_EXECUTE_HEALTH_PERCENTAGE_NODE, DEFAULT_BASE_EXECUTE_HEALTH_PERCENTAGE);
        node.set(ADDED_EXECUTE_HEALTH_PERCENTAGE_PER_BLEED_STACK_NODE, DEFAULT_ADDED_EXECUTE_HEALTH_PERCENTAGE_PER_BLEED_STACK);

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

            LivingEntity target = (LivingEntity) weaponDamageEvent.getEntity();
            CharacterTemplate targetCharacter = plugin.getCharacterManager().getCharacter(target);

            double basePercentage = SkillConfigManager.getUseSetting(hero, this,
                    BASE_EXECUTE_HEALTH_PERCENTAGE_NODE, DEFAULT_BASE_EXECUTE_HEALTH_PERCENTAGE, false);
            if (basePercentage < 0) {
                basePercentage = 0;
            }

            double addedPercentageBerBleedStack = SkillConfigManager.getUseSetting(hero, this,
                    ADDED_EXECUTE_HEALTH_PERCENTAGE_PER_BLEED_STACK_NODE, DEFAULT_ADDED_EXECUTE_HEALTH_PERCENTAGE_PER_BLEED_STACK, false);
            if (addedPercentageBerBleedStack < 0) {
                addedPercentageBerBleedStack = 0;
            }

            double healthPercentageResult = basePercentage + (addedPercentageBerBleedStack + targetCharacter.getEffectStackCount(StandardBleedEffect.NAME));

            if (target.getHealth() / target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() <= healthPercentageResult) {
                // Execute

            } else {
                // Non Execute

            }
        }
    }
}
