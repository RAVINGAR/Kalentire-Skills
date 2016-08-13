package com.herocraftonline.heroes.characters.skill.public1;

import com.google.common.util.concurrent.ExecutionError;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.nms.physics.NMSPhysics;
import com.herocraftonline.heroes.nms.physics.RayCastFlag;
import com.herocraftonline.heroes.nms.physics.RayCastHit;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class SkillBaseMissile extends ActiveSkill {

    private static NMSPhysics physics = NMSHandler.getInterface().getNMSPhysics();

    public SkillBaseMissile(Heroes plugin, String name) {
        super(plugin, name);
    }

    public class Missile {

        public static final double MIN_RADIUS = 0.1;
        public static final double MIN_MAX_SPEED = 0.05;

        public static final boolean DEFAULT_COLLIDE_WITH_BLOCK = true;
        public static final boolean DEFAULT_BLOCK_PROTECTS_ENTITY = true;
        public static final boolean DEFAULT_COLLIDE_WITH_ENTITY = true;

        public static final long DEFAULT_ENTITY_IGNORE_TICKS = 5;

        private final Hero shooter;
        private final Optional<String> tag;

        private double radius;
        private double maxSpeed;

        private World world;
        private Vector lastPosition = new Vector();
        private Vector position = new Vector();
        private Vector velocity = new Vector();

        private EnumSet<RayCastFlag> rayCastFlags;
        private final TObjectLongMap<UUID> ignoredEntities;

        private long ticksLived = 0;
        private long deathTick;
        private boolean isFinalTick = false;

        private MissileRunnable missileRunnable;

        public Missile(Hero shooter, long shooterIgnoreTicks, String tag,
                       double radius, double maxSpeed, long deathTick,
                       Vector origin, Vector launchVelocity,
                       Set<RayCastFlag> rayCastFlags) {

            this.shooter = checkNotNull(shooter);
            this.world = shooter.getPlayer().getWorld();
            this.tag = Optional.of(tag);

            setRadius(radius);
            setMaxSpeed(maxSpeed);
            setDeathTick(deathTick);

            setPosition(origin);
            setVelocity(launchVelocity);

            this.rayCastFlags = EnumSet.copyOf(rayCastFlags);
            ignoredEntities = new TObjectLongHashMap<>();

            if (shooterIgnoreTicks > 0) {
                ignoredEntities.put(shooter.getPlayer().getUniqueId(), shooterIgnoreTicks);
            }

            missileRunnable = new MissileRunnable();
        }

        public final Hero getShooter() {
            return shooter;
        }

        public final Optional<String> getTag() {
            return tag;
        }

        public final World getWorld() {
            return world;
        }

        public final void setWorld(World world) {
            this.world = checkNotNull(world);
        }

        public final Vector getPosition() {
            return position.clone();
        }

        public final void getPosition(Vector position) {
            position.copy(this.position);
        }

        public final void setPosition(Vector position) {
            this.position.copy(position);
        }

        protected final Vector getLastPosition() {
            return lastPosition.clone();
        }

        protected final void getLastPosition(Vector lastPosition) {
            lastPosition.copy(this.lastPosition);
        }

        public final Location getLocation() {
            return position.toLocation(world).setDirection(velocity);
        }

        public final void setLocation(Location location) {
            setWorld(location.getWorld());
            setPosition(location.toVector());
        }

        public final void setLocationAndDirection(Location location, double speed) {
            setLocation(location);
            setVelocity(location.getDirection().multiply(speed));
        }

        public final void setLocationAndDirection(Location location) {
            setLocationAndDirection(location, getSpeed());
        }

        public final Vector getVelocity() {
            return velocity;
        }

        public final void getVelocity(Vector velocity) {
            velocity.copy(this.velocity);
        }

        public final void setVelocity(Vector velocity) {
            this.velocity.copy(velocity);
            clampSpeed();
        }

        public final void addForce(Vector force) {
            velocity.add(force);
            clampSpeed();
        }

        private void clampSpeed() {
            if (getSpeedSquared() > NumberConversions.square(maxSpeed)) {
                velocity.normalize().multiply(maxSpeed);
            }
        }

        public final double getSpeedSquared() {
            return velocity.lengthSquared();
        }

        public final double getSpeed() {
            return velocity.length();
        }

        public final double getRadius() {
            return radius;
        }

        public final void setRadius(double radius) {
            radius = Math.abs(radius);
            this.radius = radius >= MIN_RADIUS ? radius : MIN_RADIUS;
        }

        public final double getMaxSpeed() {
            return maxSpeed;
        }

        public final void setMaxSpeed(double maxSpeed) {
            this.maxSpeed = maxSpeed >= MIN_MAX_SPEED ? maxSpeed : MIN_MAX_SPEED;
        }

        public final long getTicksLived() {
            return ticksLived;
        }

        public final long getDeathTick() {
            return deathTick;
        }

        public final void setDeathTick(long deathTick) {
            if (!isFinalTick) {
                this.deathTick = deathTick;
            }
        }

        public final long getRemainingLife() {
            return deathTick - ticksLived;
        }

        public final void setRemainingLife(long ticks) {
            setDeathTick(ticksLived + ticks);
        }

        public final boolean isFinalTick() {
            return isFinalTick;
        }

        public final void fireMissile() {
            if (isActive()) {
                missileRunnable.startMissile();
            }
        }

        public final boolean isActive() {
            return missileRunnable != null;
        }

        public final boolean isAlive() {
            return isActive() && missileRunnable.isStarted();
        }

        public final void kill() {
            deathTick = ticksLived;
            isFinalTick = true;
        }

        protected void awake() { }

        protected void start() { }

        protected void preTick() { }

        protected void tick() { }

        protected void postTick() { }

        protected void onFinalTick() { }

        protected boolean collideWithBlock(Block block, Vector point, BlockFace face) { return DEFAULT_COLLIDE_WITH_BLOCK; }

        protected boolean blockProtectsEntity(Block block, Entity entity, Vector point, BlockFace face) { return DEFAULT_BLOCK_PROTECTS_ENTITY; }

        protected boolean collideWithEntity(Entity entity) { return DEFAULT_COLLIDE_WITH_ENTITY; }

        protected void onEntityHit(Entity entity, Vector hitOrigin, Vector hitForce) { }

        protected long onEntityPassed(Entity entity, Vector passOrigin, Vector passForce) { return DEFAULT_ENTITY_IGNORE_TICKS; }

        protected void onBlockHit(Block block, Vector hitPoint, BlockFace hitFace, Vector hitForce) { }

        protected void onBlockPassed(Block block, Vector passPoint, BlockFace passFace, Vector passForce) { }

        private final class MissileRunnable extends BukkitRunnable {

            private boolean isStarted = false;

            public boolean isStarted() {
                return isStarted;
            }

            public void startMissile() {
                if (!isStarted) {

                    runTaskTimer(plugin, 1, 1);
                    isStarted = true;

                    try {
                        awake();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }

            @Override
            public void run() {

                if (ticksLived == 0) {
                    try {
                        start();
                    } catch (ExecutionError ex) {
                        ex.printStackTrace();
                    }
                }

                ticksLived++;

                try {
                    preTick();
                } catch (ExecutionError ex) {
                    ex.printStackTrace();
                }

                lastPosition.copy(position);
                position.add(velocity);

                final Vector missileRay = position.clone().subtract(lastPosition);
                final double missileRayLengthSq = missileRay.lengthSquared();

                BlockCollision hitBlock = null;
                List<BlockCollision> passedBlocks = new ArrayList<>();

                // Handle blocks in the missile path and check for collisions
                {
                    Iterator<RayCastHit> rayCastHitBlocks = physics.rayCastBlocksAll(world, lastPosition, position, rayCastFlags);
                    while (hitBlock == null && rayCastHitBlocks.hasNext()) {

                        RayCastHit rayCastHitBlock = rayCastHitBlocks.next();

                        Block block = rayCastHitBlock.getBlock(world);
                        Vector hitPoint = rayCastHitBlock.getPoint();
                        BlockFace hitFace = rayCastHitBlock.getFace();

                        boolean collideWithBlock = DEFAULT_COLLIDE_WITH_BLOCK;

                        try {
                            collideWithBlock = collideWithBlock(block, hitPoint, hitFace);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                        double hitDistanceSq = hitPoint.clone().subtract(lastPosition).lengthSquared();
                        BlockCollision blockCollision = new BlockCollision(block, hitPoint, hitFace, hitDistanceSq);

                        if (collideWithBlock) {
                            position.copy(hitPoint);
                            hitBlock = blockCollision;
                        } else {
                            passedBlocks.add(blockCollision);
                        }
                    }
                }

                EntityCollision hitEntity = null;
                List<EntityCollision> passedEntities = new ArrayList<>();

                // Handle entities in the missile path and check for collisions
                {
                    List<Entity> entitiesInPath = physics.getEntitiesInVolume(world, null,
                            physics.createCapsule(lastPosition, position, radius));

                    for (Entity entity : entitiesInPath) {

                        long ignoredUntil = ignoredEntities.get(entity.getUniqueId());
                        if (ignoredUntil <= 0) {

                            if (ignoredUntil <= ticksLived) {
                                ignoredEntities.remove(entity.getUniqueId());
                            }

                            continue;
                        }

                        Vector entityCenter = physics.getEntityAABB(entity).getCenter();
                        double dot = missileRay.dot(entityCenter.clone().subtract(lastPosition));

                        Vector missileHit;
                        if (dot < 0) {
                            continue;
                        } else if (dot == 0) {
                            missileHit = lastPosition.clone();
                        } else if (dot >= missileRayLengthSq) {
                            missileHit = position.clone();
                        } else {
                            missileHit = lastPosition.clone().add(missileRay.clone().multiply(dot / missileRayLengthSq));
                        }

                        Iterator<RayCastHit> rayCastHitBlocks = physics.rayCastBlocksAll(world, missileHit, entityCenter, rayCastFlags);
                        boolean entityHit = true;

                        while (entityHit && rayCastHitBlocks.hasNext()) {

                            RayCastHit rayCastHitBlock = rayCastHitBlocks.next();

                            Block block = rayCastHitBlock.getBlock(world);
                            Vector hitPoint = rayCastHitBlock.getPoint();
                            BlockFace hitFace = rayCastHitBlock.getFace();

                            boolean blockProtectsEntity = DEFAULT_BLOCK_PROTECTS_ENTITY;

                            try {
                                blockProtectsEntity = blockProtectsEntity(block, entity, hitPoint, hitFace);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }

                            if (blockProtectsEntity) {
                                entityHit = false;
                            }
                        }

                        if (entityHit) {

                            double hitDistanceSq = missileRay.clone().multiply(dot / missileRayLengthSq).lengthSquared();

                            boolean collideWithEntity = DEFAULT_COLLIDE_WITH_ENTITY;

                            try {
                                collideWithEntity = collideWithEntity(entity);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }

                            if (collideWithEntity) {
                                if (hitEntity == null || hitDistanceSq < hitEntity.hitDistanceSq) {
                                    hitEntity = new EntityCollision(entity, entityCenter, missileHit, hitDistanceSq);
                                }
                            } else {
                                passedEntities.add(new EntityCollision(entity, entityCenter, missileHit, hitDistanceSq));
                            }
                        }
                    }
                }

                double appliedHitDistanceSq = Long.MAX_VALUE;

                if (hitEntity != null) {
                    appliedHitDistanceSq = Math.min(appliedHitDistanceSq, hitEntity.hitDistanceSq);
                    position.copy(hitEntity.hitOrigin);

                    Vector hitForce = hitEntity.entityCenter.clone().subtract(hitEntity.hitOrigin).add(velocity);

                    try {
                        onEntityHit(hitEntity.entity, hitEntity.hitOrigin, hitForce);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    kill();
                }
                else if (hitBlock != null) {
                    appliedHitDistanceSq = Math.min(appliedHitDistanceSq, hitBlock.hitDistanceSq);

                    try {
                        onBlockHit(hitBlock.block, hitBlock.hitPoint, hitBlock.hitFace, velocity.clone());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    kill();
                }

                for (BlockCollision blockCollision : passedBlocks) {
                    if (hitBlock == null || blockCollision.hitDistanceSq < appliedHitDistanceSq) {
                        try {
                            onBlockPassed(blockCollision.block, blockCollision.hitPoint, blockCollision.hitFace, velocity.clone());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }

                for (EntityCollision entityCollision : passedEntities) {
                    if (hitEntity == null || entityCollision.hitDistanceSq < appliedHitDistanceSq) {

                        Vector hitForce = entityCollision.entityCenter.clone().subtract(entityCollision.hitOrigin).add(velocity);
                        long ignoreTicks = 0;

                        try {
                            ignoreTicks = onEntityPassed(entityCollision.entity, entityCollision.hitOrigin, hitForce);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                        if (ignoreTicks > 0) {
                            ignoredEntities.put(entityCollision.entity.getUniqueId(), ticksLived + ignoreTicks);
                        }
                    }
                }

                try {
                    tick();
                } catch (ExecutionError ex) {
                    ex.printStackTrace();
                }

                try {
                    postTick();
                } catch (ExecutionError ex) {
                    ex.printStackTrace();
                }

                if (isFinalTick || ticksLived >= deathTick) {
                    cancel();
                    missileRunnable = null;

                    try {
                        onFinalTick();
                    } catch (ExecutionError ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    public class HomingMissile extends Missile {

        public static final double HOMING_FORCE_EPSILON = 1E-6;

        private Vector target = new Vector();
        private double homingForce;

        public HomingMissile(Hero shooter, long shooterIgnoreTicks, String tag,
                             double radius, double maxSpeed, long deathTick,
                             Vector origin, Vector launchVelocity,
                             Set<RayCastFlag> rayCastFlags,
                             Vector target, double homingForce) {

            super(shooter, shooterIgnoreTicks, tag, radius, maxSpeed, deathTick, origin, launchVelocity, rayCastFlags);

            setTarget(target);
            setHomingForce(homingForce);
        }

        public final Vector getTarget() {
            return target.clone();
        }

        public final void getTarget(Vector target) {
            target.copy(this.target);
        }

        public final void setTarget(Vector target) {
            this.target.copy(target);
        }

        public final double getHomingForce() {
            return homingForce;
        }

        public final void setHomingForce(double homingForce) {
            this.homingForce = homingForce;
        }

        public final boolean isHoming() {
            return Math.abs(homingForce) >= HOMING_FORCE_EPSILON;
        }

        public final void maximizeHomingForce() {
            homingForce = getMaxSpeed() * 2;
        }

        public final void minimizeHomingForce() {
            homingForce = getMaxSpeed() * -2;
        }

        public final void disableHomingForce() {
            homingForce = 0;
        }

        public final double getDistanceToTarget() {
            return getPosition().distance(target);
        }

        public final double getDistanceToTargetSquared() {
            return getPosition().distanceSquared(target);
        }

        @Override
        protected void preTick() {
            if (isHoming()) {
                addForce(target.clone().subtract(getPosition()).normalize().multiply(homingForce));
            }
        }
    }

    private class BlockCollision {

        public final Block block;
        public final Vector hitPoint;
        public final BlockFace hitFace;
        public final double hitDistanceSq;

        public BlockCollision(Block block, Vector hitPoint, BlockFace hitFace, double hitDistanceSq) {
            this.block = block;
            this.hitPoint = hitPoint;
            this.hitFace = hitFace;
            this.hitDistanceSq = hitDistanceSq;
        }
    }

    private class EntityCollision {

        public final Entity entity;
        public final Vector entityCenter;
        public final Vector hitOrigin;
        public final double hitDistanceSq;

        public EntityCollision(Entity entity, Vector entityCenter, Vector hitOrigin, double hitDistanceSq) {
            this.entity = entity;
            this.entityCenter = entityCenter;
            this.hitDistanceSq = hitDistanceSq;
            this.hitOrigin = hitOrigin;
        }
    }
}
