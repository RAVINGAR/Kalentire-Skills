package com.herocraftonline.heroes.characters.skill.remastered.cleric;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseHeal;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;

public class SkillSacredWord extends SkillBaseHeal {

    public SkillSacredWord(Heroes plugin) {
        super(plugin, "SacredWord");
        setDescription("SacredWord relieves your target, restoring $1 of their health and removing any blind effects " +
                "that they may have. You are only healed for $2 health from this ability.");
        setUsage("/skill sacredword <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill sacredword");
        setTypes(SkillType.ABILITY_PROPERTY_LIGHT, SkillType.DISPELLING, SkillType.HEALING, SkillType.SILENCEABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();

        config.set(SkillSetting.MAX_DISTANCE.node(), 8);
        config.set(SkillSetting.HEALING.node(), 75);
        config.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 1.875);

        return config;
    }

    @Override
    protected void removeEffects(Hero healer, CharacterTemplate targetCT) {
        for (Effect effect : targetCT.getEffects()) {
            if (effect.isType(EffectType.BLIND) && effect.isType(EffectType.HARMFUL)) {
                targetCT.removeEffect(effect);
            }
        }
    }

    protected void applySoundEffects(World world, LivingEntity target) {
        world.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
    }

    protected void applyParticleEffects(World world, LivingEntity target) {
//        world.spigot().playEffect(target.getLocation().add(0, 0.5, 0), // location
//                org.bukkit.Effect.HAPPY_VILLAGER, // effect
//                0, // id
//                0, // data
//                1, 1, 1, // offset
//                1.0f, // speed
//                25, // particle count
//                1); // radius
        world.spawnParticle(Particle.VILLAGER_HAPPY, target.getLocation().add(0, 0.5, 0), 25, 1, 1, 1, 1);
    }
}
