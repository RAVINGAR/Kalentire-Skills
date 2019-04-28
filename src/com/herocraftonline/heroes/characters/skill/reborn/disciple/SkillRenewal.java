package com.herocraftonline.heroes.characters.skill.reborn.disciple;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseHeal;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;

public class SkillRenewal extends SkillBaseHeal {

    public SkillRenewal(Heroes plugin) {
        super(plugin, "Renewal");
        setDescription("You restore $1 health to your target. You are only healed for $2 health from this ability.");
        setUsage("/skill renewal <target>");
        setIdentifiers("skill renewal");
        setArgumentRange(0, 1);
        setTypes(SkillType.HEALING, SkillType.NAME_TARGETTING_ENABLED, SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_LIGHT);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 5);
        config.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_WISDOM.node(), 0.0);
        config.set(SkillSetting.HEALING.node(), 100);
        config.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 0.0);
        return config;
    }
    
    protected void applySoundEffects(World world, LivingEntity target) {
        world.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 0.9f);
    }

    protected void applyParticleEffects(World world, LivingEntity target) {
//        world.spigot().playEffect(target.getLocation(), // location
//                org.bukkit.Effect.HAPPY_VILLAGER, // effect
//                0, // id
//                0, // data
//                1, 1, 1, // offset
//                1.0f, // speed
//                25, // particle count
//                1); // radius
        world.spawnParticle(Particle.VILLAGER_HAPPY, target.getLocation(), 25, 1, 1, 1, 1);
    }

    @Override
    protected void removeEffects(Hero hero, CharacterTemplate targetCT) {
        // No effects are removed by this Skill.
    }
}