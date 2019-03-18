package com.herocraftonline.heroes.characters.skill.reborn.arcanist;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.tools.Missile;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.SphereEffect;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

public class SkillAetherMissiles extends ActiveSkill {

    private static final Color FIRE_ORANGE = Color.fromRGB(226, 88, 34);
    private static final Color FIRE_RED = Color.fromRGB(236, 60, 30);

    public SkillAetherMissiles(Heroes plugin) {
        super(plugin, "AetherMissiles");
        setDescription("TBD");
        setUsage("/skill aethermissiles");
        setArgumentRange(0, 0);
        setIdentifiers("skill aethermissiles");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 80, false);
        return getDescription().replace("%1", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DURATION.node(), 15000);
        config.set("projectile-damage", 25.0);
        config.set("projectile-velocity", 20.0);
        config.set("projectile-per-hit-multiplier", 1.25);
        config.set("projectile-max-duration", 1500);
        config.set("projectile-radius", 0.5);
        config.set("projectile-launch-delay-ticks", 3);
        config.set("num-projectiles", 4);

        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 15000, false);
        hero.addEffect(new AetherMissilesEffect(this, player, duration));

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    class AetherMissilesEffect extends ExpirableEffect {

        private EffectManager effectManager;
        private int numProjectiles;
        private double projectileRadius;

        public AetherMissilesEffect(Skill skill, Player applier, long duration) {
            super(skill, "FloatingAetherMissiles", applier, duration);

            this.types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            this.effectManager = new EffectManager(plugin);

            this.numProjectiles = SkillConfigManager.getUseSetting(hero, skill, "num-projectiles", 4, false);
            this.projectileRadius = SkillConfigManager.getUseSetting(hero, skill, "projectile-radius", 0.5, false);

            for (int i = 0; i < numProjectiles; i++) {
                SphereEffect missileVisual = new SphereEffect(effectManager);
                missileVisual.iterations = (int) (getDuration() / 50);
                missileVisual.radius = this.projectileRadius;
                missileVisual.particle = Particle.SPELL_WITCH;
                missileVisual.radiusIncrease = 0;

                missileVisual.setLocation(applier.getEyeLocation().add(new Vector(0, 0.5, 0)));
                effectManager.start(missileVisual);
            }
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            double velocity = SkillConfigManager.getUseSetting(hero, skill, "projectile-velocity", 20.0, false);
            int projectileLaunchDelay = SkillConfigManager.getUseSetting(hero, skill, "projectile-launch-delay-ticks", 3, false);

//            for (int i = 0; i < numProjectiles; i++) {
//                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
//                    public void run() {
//                        AetherMissile missile = new AetherMissile(hero, skill, projectileRadius);
//                        missile.fireMissile();
//                    }
//                }, projectileLaunchDelay * i);
//            }
//            this.effectManager.disposeOnTermination();
        }
    }

//    interface MissileDeathCallback {
//        void onMissileDeath(Missile missile);
//    }

//    class AetherMissile extends Missile {
//
//        private final Hero hero;
//        private final Player player;
//        private final Skill skill;
//
//        private final int initialDurationTicks;
//        private final double projectileDamage;
//
//        private double defaultSpeed;
//
//        AetherMissile(Hero hero, Skill skill, double radius) {
//            this.hero = hero;
//            this.skill = skill;
//            this.player = hero.getPlayer();
//
//            this.projectileDamage = SkillConfigManager.getUseSetting(hero, skill, "projectile-damage", 25.0, false);
//            this.initialDurationTicks = SkillConfigManager.getUseSetting(hero, skill, "projectile-max-duration", 2000, false) / 50;
//
//            setNoGravity();
//            setEntityDetectRadius(radius);
//            setRemainingLife(this.initialDurationTicks);
//
//            Location playerLoc = player.getEyeLocation();
//            Vector direction = playerLoc.getDirection().normalize();
//            Vector offset = direction.multiply(1.25);
//            Location missileLoc = playerLoc.add(offset);
//
//            this.setLocationAndSpeed(missileLoc, 1.5D);
//        }
//
//        private void updateVisualLocation() {
//
//        }
//
//        protected void onStart() {
//            this.defaultSpeed = getVelocity().length();
//            updateVisualLocation();
//        }
//
//        protected void onTick() {
//            updateVisualLocation();
//        }
//
//        protected void onFinalTick() {
//            vEffectManager.dispose();
//        }
//
//        protected boolean onCollideWithEntity(Entity entity) {
//            if (!(entity instanceof LivingEntity) || entity == player || !damageCheck(player, (LivingEntity) entity))
//                return false;
//            return true;
//        }
//
//        protected void onEntityHit(Entity entity, Vector hitOrigin, Vector hitForce) {
//            LivingEntity target = (LivingEntity) entity;
//
//            addSpellTarget(target, hero);
//            damageEntity(target, player, this.projectileDamage, EntityDamageEvent.DamageCause.MAGIC);
//        }
//    }
}