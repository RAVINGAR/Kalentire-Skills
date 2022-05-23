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

public class SkillAbsolution extends SkillBaseHeal {

    public SkillAbsolution(Heroes plugin) {
        super(plugin, "Absolution");
        setDescription("You restore $1 health to your target and remove DARK effects. Only heals for $2 if self targetted.");
        setUsage("/skill absolution <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill absolution");
        setTypes(SkillType.HEALING, SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_LIGHT);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 5);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), 0.15);
        node.set(SkillSetting.HEALING.node(), 125);
        node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 2.0);

        return node;
    }
    
    protected void applySoundEffects(World world, LivingEntity target) {
        world.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 0.7f);
    }

    protected void applyParticleEffects(World world, LivingEntity target) {
//        world.spigot().playEffect(target.getLocation().add(0, 0.5, 0), // location
//                org.bukkit.Effect.FIREWORKS_SPARK, // effect
//                0, // id
//                0, // data
//                0.5F, 0.5F, 0.5F, // offset
//                1.0f, // speed
//                25, // particle count
//                16); // radius
        world.spawnParticle(Particle.FIREWORKS_SPARK, target.getLocation().add(0, 0.5, 0), 25, 0.5, 0.5, 0.5, 1);
    }

    @Override
    protected void removeEffects(Hero healer, CharacterTemplate targetCT) {
        for (Effect effect : targetCT.getEffects()) {
            if (effect.isType(EffectType.DISPELLABLE) && effect.isType(EffectType.HARMFUL)) {
                if (effect.isType(EffectType.DARK)) {
                	//hero.getPlayer().getWorld().spigot().playEffect(hero.getPlayer().getLocation().add(0, 0.3, 0), org.bukkit.Effect.HAPPY_VILLAGER, 0, 0, 0.5F, 0.5F, 0.5F, 0.0F, 16, 16);
                    targetCT.getEntity().getWorld().spawnParticle(Particle.VILLAGER_ANGRY, targetCT.getEntity().getLocation().add(0, 0.3, 0), 16, 0.5, 0.5, 0.5, 0);
                    targetCT.removeEffect(effect);
                }
            }
        }
    }
}
