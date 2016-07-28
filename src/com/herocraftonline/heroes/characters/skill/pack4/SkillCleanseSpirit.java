package com.herocraftonline.heroes.characters.skill.pack4;

import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Util;

public class SkillCleanseSpirit extends SkillBaseHeal {

    public SkillCleanseSpirit(Heroes plugin) {
        super(plugin, "CleanseSpirit");
        setDescription("Cleanse the spirit of the target, restoring $1 of their health and removing $2 random debuff(s) that does not impede movement. You are only healed for $3 health from this ability.");
        setUsage("/skill cleansespirit <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill cleansespirit");
        setTypes(SkillType.ABILITY_PROPERTY_LIGHT, SkillType.DISPELLING, SkillType.HEALING, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING.node(), 125, false);
        double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 2.0, false);
        healing += (hero.getAttributeValue(AttributeType.WISDOM) * healingIncrease);
        
        int effectRemovals = SkillConfigManager.getUseSetting(hero, this, "max-effect-removals", 1, false);
        
        String formattedHealing = Util.decFormat.format(healing);
        String formattedSelfHealing = Util.decFormat.format(healing * Heroes.properties.selfHeal);

        return getDescription().replace("$1", formattedHealing).replace("$2", effectRemovals + "").replace("$3", formattedSelfHealing);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 10);
        node.set(SkillSetting.HEALING.node(), 150);
        node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 3.75);
        node.set("max-effect-removals", 1);

        return node;
    }
    
    protected void applySoundEffects(World world, LivingEntity target) {
        world.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 0.7f);
    }

    protected void applyParticleEffects(World world, LivingEntity target) {
        world.spigot().playEffect(target.getLocation().add(0, 0.5, 0), // location
                org.bukkit.Effect.HAPPY_VILLAGER, // effect
                0, // id
                0, // data
                0.5F, 0.5F, 0.5F, // offset
                1.0f, // speed
                25, // particle count
                16); // radius
    }

    @Override
    protected void removeEffects(Hero hero) {
        for (Effect effect : hero.getEffects()) {
            if (effect.isType(EffectType.DISPELLABLE) && effect.isType(EffectType.HARMFUL)) {
                if (effect.isType(EffectType.FIRE)) {
                    hero.removeEffect(effect);
                    hero.getPlayer().getWorld().spigot().playEffect(hero.getPlayer().getLocation().add(0, 0.3, 0), org.bukkit.Effect.EXTINGUISH, 0, 0, 0, 0.4F, 0, 0.7F, 35, 16);
                    hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENTITY_GENERIC_BURN, 0.5F, 1.0F);
                }
            }
        }
        
        int maxRemovals = SkillConfigManager.getUseSetting(hero, this, "max-effect-removals", 1, false);

        for (Effect effect : hero.getEffects()) {
            // This combined with checking for DISPELLABLE and HARMFUL is so huge I'd rather split the lines. Disallow dispelling movement impediment, don't want the class countering itself.
            boolean isMovementImpeding = effect.isType(EffectType.SLOW) || effect.isType(EffectType.VELOCITY_DECREASING) ||
                    effect.isType(EffectType.WALK_SPEED_DECREASING) || effect.isType(EffectType.ROOT);
            if (!isMovementImpeding && effect.isType(EffectType.DISPELLABLE) && effect.isType(EffectType.HARMFUL )) {
                hero.removeEffect(effect);
                // Just in case it's fire
                if (effect.isType(EffectType.FIRE)) {
                    hero.getPlayer().setFireTicks(0);
                }
                maxRemovals--;
                if (maxRemovals == 0) {
                    break;
                }
            }
        }
    }
}
