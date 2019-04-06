package com.herocraftonline.heroes.characters.skill.reborn.hookwarrior;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.tools.BasicMissile;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.LineEffect;
import de.slikey.effectlib.util.DynamicLocation;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class SkillHook extends ActiveSkill {

    public SkillHook(Heroes plugin) {
        super(plugin, "Hook");
        setDescription("You throw a hook and chain out in front of you. " +
                "The hook will latch onto whatever it hits for $1 seconds. " +
                "Enemies will be dealt $2 damage on hit.");
        setUsage("/skill hook");
        setArgumentRange(0, 0);
        setIdentifiers("skill hook");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DAMAGING, SkillType.MULTI_GRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 1, false);

        return getDescription()
                .replace("$1", Util.decFormat.format((double) duration / 1000))
                .replace("$2", Util.decFormat.format(damage));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 40.0);
        config.set("projectile-size", 0.25);
        config.set("projectile-velocity", 20.0);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {

        broadcastExecuteText(hero);

        double projSize = SkillConfigManager.getUseSetting(hero, this, "projectile-size", 0.25, false);
        double projVelocity = SkillConfigManager.getUseSetting(hero, this, "projectile-velocity", 20.0, false);

        HookProjectile missile = new HookProjectile(plugin, this, hero, projSize, projVelocity);
        missile.fireMissile();

        return SkillResult.NORMAL;
    }

    private class HookProjectile extends BasicMissile {
        private final long hookedDuration;
        private final double maxHookDistance;

        HookProjectile(Plugin plugin, Skill skill, Hero hero, double projectileSize, double projVelocity) {
            super(plugin, skill, hero, projectileSize, projVelocity);

            setNoGravity();
            setRemainingLife(30);
            this.damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 50.0, false);
            this.maxHookDistance = SkillConfigManager.getUseSetting(hero, skill, "max-hook-distance", 20.0, false);
            this.hookedDuration = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DURATION, 10000, false);

            this.visualEffect = getHookVisual(this.effectManager, player, getLocation());
        }

        @Override
        protected void onTick() {
            this.visualEffect.setTargetLocation(getLocation());
        }

        @Override
        protected void onEntityHit(Entity entity, Vector hitOrigin, Vector hitForce) {
            if (!(entity instanceof LivingEntity))
                return;

            LivingEntity target = (LivingEntity)entity;
            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
            HookedEffect hookedEffect = new HookedEffect(skill, player, hookedDuration, maxHookDistance);

            // If we're an ally, make the effect beneficial and add it to them.
            if (hero.isAlliedTo(target)) {
                hookedEffect.types.add(EffectType.BENEFICIAL);
                targetCT.addEffect(hookedEffect);
                return;
            }

            // We're dealing with an enemy now
            if (!damageCheck(this.player, target))
                return;

            skill.addSpellTarget(target, this.hero);
            skill.damageEntity(target, this.player, this.damage, DamageCause.ENTITY_ATTACK);

            hookedEffect.types.add(EffectType.HARMFUL);
            targetCT.addEffect(hookedEffect);
        }
    }

    public class HookedEffect extends PeriodicExpirableEffect {
        private final double maxDistSquared;
        private EffectManager effectManager;

        HookedEffect(Skill skill, Player applier, long duration, double maxDist) {
            super(skill, applier.getName() + "-Hooked", applier, 1000, duration, null, null);
            this.maxDistSquared = maxDist * maxDist;
            types.add(EffectType.PHYSICAL);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            this.effectManager = new EffectManager(plugin);
            LineEffect effect = getHookVisual(this.effectManager, applier, hero.getPlayer());
            this.effectManager.start(effect);
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);

            this.effectManager = new EffectManager(plugin);
            LineEffect effect = getHookVisual(this.effectManager, applier, monster.getEntity());
            this.effectManager.start(effect);
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            this.effectManager.dispose();
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            this.effectManager.dispose();
        }

        @Override
        public void tickHero(Hero hero) {
            if (hero.getEntity().getLocation().distanceSquared(applier.getLocation()) > maxDistSquared)
                hero.removeEffect(this);
        }

        @Override
        public void tickMonster(Monster monster) {
            if (monster.getEntity().getLocation().distanceSquared(applier.getLocation()) > maxDistSquared)
                monster.removeEffect(this);
        }
    }

    private LineEffect getHookVisual(EffectManager effectManager, Player owner, LivingEntity target) {
        LineEffect effect = getBaseHookVisual(effectManager);

        DynamicLocation dynamicOwnerLoc = new DynamicLocation(owner);
        effect.setDynamicOrigin(dynamicOwnerLoc);

        DynamicLocation dynamicTargetLoc = new DynamicLocation(target);
        dynamicOwnerLoc.addOffset(new Vector(0, -0.5, 0));
        effect.setDynamicTarget(dynamicTargetLoc);

        return effect;
    }

    private LineEffect getHookVisual(EffectManager effectManager, Player owner, Location targetLoc) {
        LineEffect effect = getBaseHookVisual(effectManager);

        DynamicLocation dynamicOwnerLoc = new DynamicLocation(owner);
        effect.setDynamicOrigin(dynamicOwnerLoc);
        effect.setLocation(targetLoc);

        return effect;
    }

    @NotNull
    private LineEffect getBaseHookVisual(EffectManager effectManager) {
        LineEffect effect = new LineEffect(effectManager);
        effect.particle = Particle.CRIT_MAGIC;
        effect.particles = 5;
//        effect.particleSize = 0.5f;
//        effect.speed = 2.0f;
        effect.iterations = 9999;
        return effect;
    }
}
