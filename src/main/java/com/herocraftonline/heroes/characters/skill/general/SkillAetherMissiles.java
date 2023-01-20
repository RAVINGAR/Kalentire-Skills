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
import com.herocraftonline.heroes.characters.skill.tools.BasicDamageMissile;
import com.herocraftonline.heroes.characters.skill.tools.BasicMissile;
import com.herocraftonline.heroes.libs.slikey.effectlib.effect.SphereEffect;
import com.herocraftonline.heroes.libs.slikey.effectlib.util.DynamicLocation;
import com.herocraftonline.heroes.util.GeometryUtil;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class SkillAetherMissiles extends ActiveSkill {

    private static final Color FIRE_ORANGE = Color.fromRGB(226, 88, 34);
    private static final Color FIRE_RED = Color.fromRGB(236, 60, 30);
    private static final String toggleableEffectName = "FloatingAetherMissiles";

    public SkillAetherMissiles(final Heroes plugin) {
        super(plugin, "AetherMissiles");
        setDescription("Summons up to $1 missiles that will float and remain inactive around the caster for up to $2 seconds. " +
                "If this ability is cast again within that time, it will unleash each stored missile in a stream. " +
                "Each missile does $3 damage, increased by $4 for each number of missile you previously landed on the same target. " +
                "Total maximum damage for hitting every projectile is $5!");
        setUsage("/skill aethermissiles");
        setArgumentRange(0, 0);
        setIdentifiers("skill aethermissiles");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);

        setToggleableEffectName(toggleableEffectName);
    }

    @Override
    public String getDescription(final Hero hero) {
        final int numProjectiles = SkillConfigManager.getUseSetting(hero, this, "num-projectiles", 4, false);
        final long duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        final double damage = SkillConfigManager.getUseSetting(hero, this, "projectile-damage", 25.0, false);
        final double damageIncreasePerHit = SkillConfigManager.getUseSetting(hero, this, "projectile-damage-increase-per-hit", 10.0, false);

        // I am a retard who does not know how to make an actual formula for this. If you can math it out, feel free...
        double totalMaxPossibleDamage = 0.0;
        for (int i = 0; i < numProjectiles; i++) {
            totalMaxPossibleDamage += damage + (damageIncreasePerHit * i);
        }

        return getDescription()
                .replace("$1", numProjectiles + "")
                .replace("$2", Util.decFormat.format((double) duration / 1000))
                .replace("$3", Util.decFormat.format(damage))
                .replace("$4", Util.decFormat.format(damageIncreasePerHit))
                .replace("$5", Util.decFormat.format(totalMaxPossibleDamage));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DURATION.node(), 15000);
        config.set(SkillSetting.DAMAGE.node(), 25.0);
        config.set("damage-increase-per-hit", 10.0);
        config.set(BasicMissile.PROJECTILE_VELOCITY_NODE, 65.0);
        config.set(BasicDamageMissile.PROJECTILE_PIERCES_ON_HIT_NODE, true);
        config.set(BasicDamageMissile.PROJECTILE_KNOCKS_BACK_ON_HIT_NODE, false);
        config.set(BasicMissile.PROJECTILE_DURATION_TICKS_NODE, 30);
        config.set(BasicMissile.PROJECTILE_SIZE_NODE, 0.25);
        config.set("projectile-launch-delay-ticks", 15);
        config.set("num-projectiles", 5);
        return config;
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero);
        final int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        hero.addEffect(new AetherMissilesEffect(this, player, duration));
        return SkillResult.NORMAL;
    }

    private static class MultiMissileHitEffect extends ExpirableEffect {
        private int hitCount = 1;

        MultiMissileHitEffect(final Skill skill, final String name, final Player applier, final long duration) {
            super(skill, name, applier, duration);
        }

        private int getHitCount() {
            return this.hitCount;
        }

        private void addHit() {
            this.hitCount++;
        }
    }

    private class AetherMissilesEffect extends ExpirableEffect {
        private final List<SphereEffect> missileVisuals = new ArrayList<>();
        private int numProjectiles;
        private double projectileRadius;

        AetherMissilesEffect(final Skill skill, final Player applier, final long duration) {
            super(skill, toggleableEffectName, applier, duration);

            this.types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);

            this.numProjectiles = SkillConfigManager.getUseSetting(hero, skill, "num-projectiles", 4, false);
            this.projectileRadius = SkillConfigManager.getUseSetting(hero, skill, BasicMissile.PROJECTILE_SIZE_NODE, 0.15, false);
            final int projDurationTicks = SkillConfigManager.getUseSetting(hero, skill, BasicMissile.PROJECTILE_DURATION_TICKS_NODE, 30, false);

            final List<Location> missileLocations = GeometryUtil.circle(applier.getLocation().clone().add(new Vector(0, 0.8, 0)), numProjectiles, 1.5);
            if (missileLocations.size() < numProjectiles) {
                Heroes.log(Level.INFO, "AETHER MISSILES IS BROKEN DUE TO A CHANGE IN HEROES, YO");
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
                missileVisual.particle = Particle.SPELL_WITCH;
                missileVisual.particles = 10;
                missileVisual.radiusIncrease = 0;
                effectLib.start(missileVisual);

                missileVisuals.add(missileVisual);
            }
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);

            final int projectileLaunchDelay = SkillConfigManager.getUseSetting(hero, skill, "projectile-launch-delay-ticks", 3, false);

            for (int i = 0; i < numProjectiles; i++) {
                final int finalI = i;
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    final Player player = hero.getPlayer();
                    if (player.isDead() || player.getHealth() <= 0) {
                        return;
                    }
                    final SphereEffect missileVisual = missileVisuals.get(finalI);

                    final Location eyeLocation = hero.getPlayer().getEyeLocation();
                    missileVisual.setLocation(eyeLocation.clone().add(eyeLocation.getDirection()));
                    final AetherMissile missile = new AetherMissile(plugin, skill, hero, projectileRadius, missileVisual);
                    missile.fireMissile();
                    eyeLocation.getWorld().playSound(eyeLocation, Sound.ENTITY_VEX_HURT, 2F, 0.5F);
                }, (long) projectileLaunchDelay * i);
            }
        }
    }

    // Aether missiles is more complicated than most other missiles because we are "passing off" the visuals from the player buff.
    private class AetherMissile extends BasicDamageMissile {
        private final double damageIncreasePerHit;

        AetherMissile(final Heroes plugin, final Skill skill, final Hero hero, final double radius, final SphereEffect visualEffect) {
            super(plugin, skill, hero);

            this.damageIncreasePerHit = SkillConfigManager.getUseSetting(hero, skill, "damage-increase-per-hit", 10.0, false);
            setEntityDetectRadius(radius);
            replaceEffects(visualEffect);
            final Location newMissileLoc = visualEffect.getLocation().clone().setDirection(player.getEyeLocation().getDirection());
            visualEffect.setLocation(newMissileLoc);
        }

        @Override
        protected void onStart() {
            // Override the onStart because we don't want it to reset our already running effect manager.
            this.visualEffect.setLocation(getLocation());
        }

        @Override
        protected void onTick() {
            this.visualEffect.setLocation(getLocation());
        }

        @Override
        protected void onValidTargetFound(final LivingEntity target, final Vector origin, final Vector force) {
            final CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
            final String effectName = getMultiHitEffectName(player);
            if (targetCT.hasEffect(effectName)) {
                final MultiMissileHitEffect multiHitEffect = (MultiMissileHitEffect) targetCT.getEffect(effectName);
                damage += this.damageIncreasePerHit * multiHitEffect.getHitCount();
                multiHitEffect.addHit();
            } else {
                targetCT.addEffect(new MultiMissileHitEffect(skill, effectName, player, 5000));
            }

            addSpellTarget(target, hero);
            damageEntity(target, player, damage, EntityDamageEvent.DamageCause.MAGIC, knockBackOnHit);
        }

        private String getMultiHitEffectName(final Player player) {
            return player.getName() + "-AetherMissileMultiHit";
        }
    }
}