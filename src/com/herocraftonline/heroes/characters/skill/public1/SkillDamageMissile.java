package com.herocraftonline.heroes.characters.skill.public1;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.nms.physics.NMSPhysics;
import com.herocraftonline.heroes.nms.physics.RayCastFlag;
import com.herocraftonline.heroes.nms.physics.RayCastHit;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.CylinderEffect;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.herocraftonline.heroes.characters.skill.SkillType.*;
import static com.herocraftonline.heroes.characters.skill.SkillType.SILENCEABLE;
import static com.herocraftonline.heroes.characters.skill.SkillType.UNINTERRUPTIBLE;

public class SkillDamageMissile extends SkillBaseMissile {

    private static final double EXPLOSION_SCALER = 0.2;
    private static final double BASE_HOMING_FORCE = 0.2;

    private static NMSPhysics physics = NMSHandler.getInterface().getNMSPhysics();

    public SkillDamageMissile(Heroes plugin) {
        super(plugin, "DamageHomingMissile");
        setDescription("Damage stuff with homing missile");
        setUsage("/skill DamageHomingMissile");
        setIdentifiers("skill " + getName());
        setTypes(DAMAGING, NO_SELF_TARGETTING, UNINTERRUPTIBLE, SILENCEABLE);
        setArgumentRange(0, 0);
    }

    @Override
    public String getDescription(Hero hero) {
        return super.getDescription();
    }

    @Override
    public SkillResult use(Hero hero, String[] strings) {

        broadcastExecuteText(hero);

        Player player = hero.getPlayer();
        World world = player.getWorld();

        Vector baseLaunchVelocity = new Vector(0, 2, 0);
        Vector originOffset = new Vector(0, 0.2, 0);

        new BukkitRunnable() {

            private long tickLife = 0;
            private long missilesFired = 0;

            @Override
            public void run() {

                if (tickLife % 4 == 0) {
                    Vector start = player.getEyeLocation().toVector();
                    Vector end = player.getEyeLocation().getDirection().multiply(100).add(start);

                    final RayCastHit hitBlock = physics.rayCastBlocks(world, start, end, false, true, true);

                    if (hitBlock != null) {
                        end = hitBlock.getPoint();
                    }

                    final List<LivingEntity> targets = physics.getEntitiesInVolume(world, player,
                            physics.createCapsule(start, end, 2.5), entity -> entity instanceof LivingEntity, false)
                            .stream()
                            .map(entity -> (LivingEntity) entity)
                            .collect(Collectors.toList());

                    Vector launchVelocity = baseLaunchVelocity.clone().add(Vector.getRandom().setY(0).subtract(new Vector(0.5, 0, 0.5)).multiply(4));

                    new ScatterMissile(hero, 0, "Scatter Missile " + (missilesFired + 1),
                            0.5, 1.5, 100,
                            start.clone().add(originOffset), launchVelocity,
                            EnumSet.of(RayCastFlag.BLOCK_HIGH_DETAIL, RayCastFlag.BLOCK_IGNORE_NON_SOLID),
                            end, 0,
                            targets.isEmpty() ? null : targets.get(new Random().nextInt(targets.size())))
                            .fireMissile();

                    if (++missilesFired >= 10) {
                        cancel();
                    }
                }

                tickLife++;
            }
        }.runTaskTimer(plugin, 0, 1);

        return SkillResult.NORMAL;
    }

    private class ScatterMissile extends HomingMissile {

        private boolean hitSomething = false;
        private Entity targetEntity;

        public ScatterMissile(Hero shooter, long shooterIgnoreTicks, String tag,
                              double radius, double maxSpeed, long deathTick,
                              Vector origin, Vector launchVelocity,
                              Set<RayCastFlag> rayCastFlags,
                              Vector target, double homingForce,
                              Entity targetEntity) {

            super(shooter, shooterIgnoreTicks, tag, radius, maxSpeed, deathTick, origin, launchVelocity, rayCastFlags, target, homingForce);

            this.targetEntity = targetEntity;
        }

        public Entity getTargetEntity() {
            return targetEntity;
        }

        public void setTargetEntity(Entity entity) {
            targetEntity = entity;
        }

        @Override
        protected void preTick() {

            if (targetEntity != null) {
                if (targetEntity.isValid() && targetEntity.getWorld() == getWorld()) {
                    setTarget(physics.getEntityAABB(targetEntity).getCenter());
                }
            }
            else {
                Player player = getShooter().getPlayer();
                World world = getWorld();

                Vector start = player.getEyeLocation().toVector();
                Vector end = player.getEyeLocation().getDirection().multiply(100).add(start);

                final RayCastHit hitBlock = physics.rayCastBlocks(world, start, end, false, true, true);

                if (hitBlock != null) {
                    end = hitBlock.getPoint();
                }

                final List<LivingEntity> targets = physics.getEntitiesInVolume(world, player,
                        physics.createCapsule(start, end, 2.5), entity -> entity instanceof LivingEntity, false)
                        .stream()
                        .map(entity -> (LivingEntity) entity)
                        .collect(Collectors.toList());

                targetEntity = targets.isEmpty() ? null : targets.get(new Random().nextInt(targets.size()));

                if (targetEntity != null) {
                    setTarget(physics.getEntityAABB(targetEntity).getCenter());
                } else {
                    setTarget(end);
                }
            }

            scaleTheHomingForce();
            super.preTick();
        }

        @Override
        protected void start() {
            getWorld().playSound(getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1f);
        }

        @Override
        protected void tick() {

            // Particles and sound
            {
                EffectManager em = new EffectManager(plugin);

                Vector start = getLastPosition();
                Vector end = getPosition();

                Location loc = start.clone().add(end.clone().subtract(start).multiply(0.5)).toLocation(getWorld());
                loc.setDirection(end.clone().subtract(start));

                getWorld().playSound(loc, Sound.ENTITY_FIREWORK_BLAST_FAR, 1, 1);

                CylinderEffect cyl = new CylinderEffect(em);
                cyl.setLocation(loc);
                cyl.asynchronous = true;

                cyl.radius = (float) (getRadius() * 0.5);
                cyl.height = (float) (start.distance(end));
                cyl.particle = ParticleEffect.REDSTONE;
                cyl.particles = 20;
                cyl.solid = true;
                cyl.rotationX = Math.toRadians(loc.getPitch() + 90);
                cyl.rotationY = -Math.toRadians(loc.getYaw());
                cyl.angularVelocityX = 0;
                cyl.angularVelocityY = 0;
                cyl.angularVelocityZ = 0;
                cyl.iterations = 1;
                cyl.visibleRange = 100;

                cyl.start();
                em.disposeOnTermination();
            }
        }

        @Override
        protected boolean collideWithEntity(Entity entity) {
            return entity instanceof LivingEntity && entity != getShooter().getPlayer();
        }

        @Override
        protected void onFinalTick() {
            if (!hitSomething) {
                Vector position = getPosition();
                getWorld().createExplosion(position.getX(), position.getY(), position.getZ(),
                        (float) (getSpeed() * EXPLOSION_SCALER), false, false);
            }
        }

        @Override
        protected void onEntityHit(Entity entity, Vector hitOrigin, Vector hitForce) {
            getWorld().createExplosion(hitOrigin.getX(), hitOrigin.getY(), hitOrigin.getZ(),
                    (float) (hitForce.length() * EXPLOSION_SCALER), false, false);
            hitSomething = true;
        }

        @Override
        protected void onBlockHit(Block block, Vector hitPoint, BlockFace hitFace, Vector hitForce) {
            getWorld().createExplosion(hitPoint.getX(), hitPoint.getY(), hitPoint.getZ(),
                    (float) (hitForce.length() * EXPLOSION_SCALER), false, false);
            hitSomething = true;
        }

        private void scaleTheHomingForce() {

            double distance = getDistanceToTarget();
            double baseHomingForce = getMaxSpeed() / 4;
            double maxHomingForce = getMaxSpeed() * 0.8;

            if (distance > 10) {
                setHomingForce(getMaxSpeed() / 4);
            } else if (distance < 1E-6) {
                setHomingForce(maxHomingForce);
            } else {
                setHomingForce(((1 / distance) * (maxHomingForce - baseHomingForce)) + baseHomingForce);
            }
        }
    }
}
