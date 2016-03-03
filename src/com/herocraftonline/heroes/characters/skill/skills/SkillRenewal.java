package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillRenewal extends SkillBaseHeal {

    public SkillRenewal(Heroes plugin) {
        super(plugin, "Renewal");
        setDescription("You restore $1 health to your target. You are only healed for $2 health from this ability.");
        setUsage("/skill renewal <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill renewal");
        setTypes(SkillType.HEALING, SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_LIGHT);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 5);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_WISDOM.node(), 0.15);
        node.set(SkillSetting.HEALING.node(), 100);
        node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 2.25);

        return node;
    }
    
    protected void applySoundEffects(World world, LivingEntity target) {
        world.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 0.9f);
    }

    protected void applyParticleEffects(World world, LivingEntity target) {
        world.spigot().playEffect(target.getLocation(), // location
                org.bukkit.Effect.HAPPY_VILLAGER, // effect
                0, // id
                0, // data
                1, 1, 1, // offset
                1.0f, // speed
                25, // particle count
                1); // radius
    }

    @Override
    protected void removeEffects(Hero hero) {
        /*for (Effect effect : hero.getEffects()) {
            if (effect.isType(EffectType.DISPELLABLE) && effect.isType(EffectType.HARMFUL)) {
                if (effect.isType(EffectType.DISEASE)) {
                    hero.removeEffect(effect);
                }
            }
        }*/
    }
}