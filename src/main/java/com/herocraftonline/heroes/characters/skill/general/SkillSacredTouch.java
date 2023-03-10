package com.herocraftonline.heroes.characters.skill.general;

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

public class SkillSacredTouch extends SkillBaseHeal {

    public SkillSacredTouch(Heroes plugin) {
        super(plugin, "SacredTouch");
        setDescription("Apply a Sacred Touch to the target, restoring $1 of their health and extinguishing any fire effects present. You are only healed for $2 health from this ability.");
        setUsage("/skill sacredtouch <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill sacredtouch");
        setTypes(SkillType.ABILITY_PROPERTY_LIGHT, SkillType.DISPELLING, SkillType.HEALING, SkillType.SILENCEABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 10);
        node.set(SkillSetting.HEALING.node(), 150);
        node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 3.75);

        return node;
    }

    @Override
    protected void removeEffects(Hero healer, CharacterTemplate targetCT) {
        for (Effect effect : targetCT.getEffects()) {
            if (effect.isType(EffectType.DISPELLABLE) && effect.isType(EffectType.HARMFUL)) {
                if (effect.isType(EffectType.FIRE)) {
                    targetCT.removeEffect(effect);
                    targetCT.getEntity().getWorld().playSound(targetCT.getEntity().getLocation(), Sound.ENTITY_GENERIC_BURN, 1.6F, 1.3F);
                }
            }
        }
        targetCT.getEntity().setFireTicks(0);
    }

    protected void applySoundEffects(World world, LivingEntity target) {
        world.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
    }

    protected void applyParticleEffects(World world, LivingEntity target) {
//        world.spigot().playEffect(target.getLocation().add(0, 0.5, 0), // location
//                org.bukkit.Effect.FIREWORKS_SPARK, // effect
//                0, // id
//                0, // data
//                1, 1, 1, // offset
//                1.0f, // speed
//                25, // particle count
//                1); // radius
        world.spawnParticle(Particle.FIREWORKS_SPARK, target.getLocation().add(0, 0.5, 0), 25, 1, 1, 1, 1);
    }
}
