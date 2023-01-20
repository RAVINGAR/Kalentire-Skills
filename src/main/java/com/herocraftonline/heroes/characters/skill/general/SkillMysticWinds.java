package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.tools.Missile;
import com.herocraftonline.heroes.libs.slikey.effectlib.Effect;
import com.herocraftonline.heroes.libs.slikey.effectlib.effect.SphereEffect;
import com.herocraftonline.heroes.libs.slikey.effectlib.util.DynamicLocation;
import com.herocraftonline.heroes.util.GeometryUtil;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.LinkedList;
import java.util.List;

public class SkillMysticWinds extends ActiveSkill {
    private static final String SPORE_EFFECT = "FloatingMysticWinds";

    public SkillMysticWinds(final Heroes plugin) {
        super(plugin, "MysticWinds");
        setDescription("Summon $1 healing spores that will float and remain inactive around the caster for up to $2 seconds. " +
                "If this ability is cast again within that time, it will unleash each stored spore. " +
                "Each spore will restore for $3 health");
        setUsage("/skill mysticwinds");
        setArgumentRange(0, 0);
        setIdentifiers("skill mysticwinds");
        setTypes(SkillType.ABILITY_PROPERTY_EARTH, SkillType.SILENCEABLE, SkillType.HEALING);
    }

    @Override
    public String getDescription(final Hero hero) {
        final int numProjectiles = SkillConfigManager.getUseSetting(hero, this, "num-projectiles", 4, false);
        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 15000, false);
        final double heal = SkillConfigManager.getUseSetting(hero, this, "projectile-heal", 25.0, false);

        return getDescription()
                .replace("$1", numProjectiles + "")
                .replace("$2", Util.decFormat.format((double) duration / 1000))
                .replace("$3", Util.decFormat.format(heal));

    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DURATION.node(), 15000);
        config.set("projectile-heal", 25.0);
        config.set("projectile-velocity", 65.0);
        config.set("projectile-max-ticks-lived", 30);
        config.set("projectile-radius", 0.25);
        config.set("projectile-launch-delay-ticks", 15);
        config.set("num-projectiles", 5);
        return config;
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero);
        if (hero.hasEffect(SPORE_EFFECT)) {
            final HealingSporesEffect effect = (HealingSporesEffect) hero.getEffect(SPORE_EFFECT);
            effect.fireOrb(hero);
        } else {
            final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 15000, false);
            hero.addEffect(new HealingSporesEffect(this, player, duration));
        }

        return SkillResult.NORMAL;
    }

    private class HealingSporesEffect extends ExpirableEffect {
        private final LinkedList<SphereEffect> missileVisuals = new LinkedList<>();

        private double projectileRadius;

        HealingSporesEffect(final Skill skill, final Player applier, final long duration) {
            super(skill, SPORE_EFFECT, applier, duration);
            this.types.add(EffectType.HEALING);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);

            final int numProjectiles = SkillConfigManager.getUseSetting(hero, skill, "num-projectiles", 4, false);
            this.projectileRadius = SkillConfigManager.getUseSetting(hero, skill, "projectile-radius", 0.15, false);
            final int projDurationTicks = SkillConfigManager.getUseSetting(hero, skill, "projectile-max-ticks-lived", 30, false);

            final List<Location> missileLocations = GeometryUtil.circle(applier.getLocation().clone().add(new Vector(0, 0.8, 0)), numProjectiles, 1.5);
            if (missileLocations.size() < numProjectiles) {
                return;
            }
            for (int i = 0; i < numProjectiles; i++) {
                final SphereEffect missileVisual = new SphereEffect(effectLib);
                final DynamicLocation dynamicLoc = new DynamicLocation(applier);
                final Location missileLocation = missileLocations.get(i);
                dynamicLoc.addOffset(missileLocation.toVector().subtract(applier.getLocation().toVector()));
                missileVisual.setDynamicOrigin(dynamicLoc);
                missileVisual.iterations = (int) (getDuration() / 50) + projDurationTicks;
                missileVisual.radius = this.projectileRadius;
                missileVisual.particle = Particle.GLOW;
                missileVisual.particles = 15;
                missileVisual.radiusIncrease = 0;
                effectLib.start(missileVisual);
                missileVisuals.add(missileVisual);
            }
        }

        public void fireOrb(final Hero hero) {
            final int projectileLaunchDelay = SkillConfigManager.getUseSetting(hero, skill, "projectile-launch-delay-ticks", 3, false);
            if (missileVisuals.size() > 0) {
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    final Player player = hero.getPlayer();
                    if (player.isDead() || player.getHealth() <= 0) {
                        return;
                    }
                    final SphereEffect missileVisual = missileVisuals.removeFirst();

                    final Location eyeLocation = hero.getPlayer().getEyeLocation();
                    final Vector eyeOffset = eyeLocation.getDirection().add(new Vector(0, -1, 0));
                    missileVisual.setLocation(eyeLocation.clone().add(eyeOffset));
                    final AetherMissile missile = new AetherMissile(hero, skill, projectileRadius, missileVisual);
                    missile.fireMissile();

                }, projectileLaunchDelay);
            }
            if (missileVisuals.isEmpty()) {
                removeFromHero(hero);
            }
        }


        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            missileVisuals.forEach(Effect::cancel);
        }
    }

    private class AetherMissile extends Missile {

        private final SphereEffect visualEffect;
        private final Hero hero;
        private final Player player;
        private final Skill skill;

        private final double projectileHeal;

        AetherMissile(final Hero hero, final Skill skill, final double radius, final SphereEffect visualEffect) {
            this.hero = hero;
            this.skill = skill;
            this.player = hero.getPlayer();
            this.visualEffect = visualEffect;

            final double projectileSpeed = SkillConfigManager.getUseSetting(hero, skill, "projectile-velocity", 20.0, false);
            this.projectileHeal = SkillConfigManager.getScaledUseSettingDouble(hero, skill, "projectile-heal", 25.0, false);
            final int durationTicks = SkillConfigManager.getScaledUseSettingInt(hero, skill, "projectile-max-duration", 2000, false) / 50;

            setNoGravity();
            setEntityDetectRadius(radius);
            setRemainingLife(durationTicks);

            final Vector playerDirection = player.getEyeLocation().getDirection().normalize();
            final Location missileLoc = visualEffect.getLocation().clone().setDirection(playerDirection);
            visualEffect.setLocation(missileLoc);

            this.setLocationAndSpeed(missileLoc, projectileSpeed);
        }

        private void updateVisualLocation() {
            this.visualEffect.setLocation(getLocation());
        }

        @Override
        protected void onStart() {
            updateVisualLocation();
        }

        @Override
        protected void onTick() {
            updateVisualLocation();
        }

        @Override
        protected void onFinalTick() {
            visualEffect.cancel();
        }

        @Override
        protected boolean onCollideWithEntity(final Entity entity) {
            return entity instanceof LivingEntity && !entity.equals(player);
        }

        @Override
        protected void onEntityHit(final Entity entity, final Vector hitOrigin, final Vector hitForce) {
            final LivingEntity target = (LivingEntity) entity;
            if (hero.isAlliedTo(target)) {
                final CharacterTemplate targetCharacter = plugin.getCharacterManager().getCharacter(target);
                if (targetCharacter.tryHeal(hero, skill, this.projectileHeal)) {
                    final Location location = target.getLocation();
                    location.getWorld().spawnParticle(Particle.GLOW, location, 15, 0.05, 0.05, 0.05);
                    location.getWorld().spawnParticle(Particle.COMPOSTER, location, 10, 0.05, 0.15, 0.05);
                }
            }
        }
    }
}