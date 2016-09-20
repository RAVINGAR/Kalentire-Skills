package com.herocraftonline.heroes.characters.skill.pack1;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ExecutionError;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.nms.physics.NMSPhysics;
import com.herocraftonline.heroes.nms.physics.RayCastFlag;
import com.herocraftonline.heroes.nms.physics.RayCastHit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.util.*;

import static java.util.Objects.requireNonNull;

public abstract class SkillBaseMissile extends ActiveSkill {

    private static NMSPhysics physics = NMSHandler.getInterface().getNMSPhysics();

    public SkillBaseMissile(Heroes plugin, String name) {
        super(plugin, name);
    }

    public static class Missile {

        public static final double MIN_RADIUS = 0.05;
        public static final double MAX_RADIUS = 2;
        public static final double DEFAULT_RADIUS = 0.2;

        private static final double MIN_SPEED = 0;
        private static final double DEFAULT_SPEED = 1.5;

        public static final long DEFAULT_DEATH_TICK = Long.MAX_VALUE;

        private static final boolean DEFAULT_COLLIDE_WITH_BLOCK_RESULT = true;
        private static final boolean DEFAULT_BLOCK_PROTECTS_ENTITY_RESULT = true;
        private static final boolean DEFAULT_COLLIDE_WITH_ENTITY_RESULT = true;

        public static final ImmutableSet<RayCastFlag> DEFAULT_RAY_CAST_FLAGS =
                Sets.immutableEnumSet(RayCastFlag.BLOCK_HIGH_DETAIL);

        private final ProjectileSource shooter;
        private final Optional<String> tag;

        private double radius = DEFAULT_RADIUS;

        private World world;
        private Vector lastPosition = new Vector();
        private Vector position = new Vector();
        private Vector velocity = new Vector();

        private long ticksLived = 0;
        private long deathTick = DEFAULT_DEATH_TICK;
        private boolean isFinalTick = false;

        private EnumSet<RayCastFlag> rayCastFlags = EnumSet.copyOf(DEFAULT_RAY_CAST_FLAGS);
        private Map<UUID, Long> entityIgnoreTicks = new HashMap<>();

        private MissileRunnable missileRunnable = new MissileRunnable();

        private Missile(ProjectileSource shooter, String tag) {

            this.shooter = requireNonNull(shooter, "shooter is null");
            this.tag = Optional.ofNullable(tag);
        }

        public Missile(ProjectileSource shooter, World world, Vector origin, Vector velocity, String tag) {

            this(shooter, tag);

            setWorld(world);
            setPosition(origin);
            setVelocity(velocity);
        }

        public Missile(ProjectileSource shooter, Location location, double speed, String tag) {

            this(shooter, tag);

            setLocationAndDirection(location, speed);
        }

        public Missile(ProjectileSource shooter, Location location, String tag) {
            this(shooter, location, DEFAULT_SPEED, tag);
        }

        public Missile(ProjectileSource shooter, World world, Vector origin, Vector velocity) {
            this(shooter, world, origin, velocity, null);
        }

        public Missile(ProjectileSource shooter, Location location, double speed) {
            this(shooter, location, speed, null);
        }

        public Missile(ProjectileSource shooter, Location location) {
            this(shooter, location, DEFAULT_SPEED);
        }

        public Missile(LivingEntity shooter, double speed, String tag) {
            this(shooter, shooter != null ? shooter.getEyeLocation() : null, speed, tag);
        }

        public Missile(LivingEntity shooter, String tag) {
            this(shooter, DEFAULT_SPEED, tag);
        }

        public Missile(LivingEntity shooter, double speed) {
            this(shooter, shooter != null ? shooter.getEyeLocation() : null, speed);
        }

        public Missile(LivingEntity shooter) {
            this(shooter, DEFAULT_SPEED);
        }

        public Missile(LivingEntity shooter, Vector origin, Vector velocity, String tag) {
            this(shooter, shooter != null ? shooter.getWorld() : null, origin, velocity, tag);
        }

        public Missile(LivingEntity shooter, Vector origin, Vector velocity) {
            this(shooter, shooter != null ? shooter.getWorld() : null, origin, velocity, null);
        }

        public ProjectileSource getShooter() {
            return shooter;
        }

        public Optional<String> getTag() {
            return tag;
        }

        public final World getWorld() {
            return world;
        }

        public final void setWorld(World world) {
            this.world = requireNonNull(world, "world is null");
        }

        public final double getRadius() {
            return radius;
        }

        public final void setRadius(double radius) {
            this.radius = radius > MAX_RADIUS ? MAX_RADIUS : radius < MIN_RADIUS ? MIN_RADIUS : radius;
        }

        public final Vector getPosition() {
            return position.clone();
        }

        public final void getPosition(Vector position) {
            position.copy(this.position);
        }

        public final void setPosition(Vector position) {
            this.position.copy(requireNonNull(position, "position is null"));
        }

        protected final Vector getLastPosition() {
            return lastPosition.clone();
        }

        protected final void getLastPosition(Vector lastPosition) {
            requireNonNull(lastPosition, "lastPosition is null").copy(this.lastPosition);
        }

        public final Vector getVelocity() {
            return velocity;
        }

        public final void setVelocity(Vector velocity) {
            velocity.copy(requireNonNull(velocity));
        }

        public final Location getLocation() {
            return position.toLocation(world).setDirection(velocity);
        }

        public final void setLocation(Location location) {
            requireNonNull(location, "location is null");
            setWorld(location.getWorld());
            setPosition(location.toVector());
        }

        public final void setLocationAndDirection(Location location, double speed) {
            setLocation(location);
            velocity.copy(location.getDirection().multiply(speed));
        }

        public final void setLocationAndDirection(Location location) {
            setLocationAndDirection(location, getSpeed());
        }

        public final double getSpeedSquared() {
            return velocity.lengthSquared();
        }

        public final double getSpeed() {
            return velocity.length();
        }

        public final void setSpeed(double speed) {
            speed = speed < MIN_SPEED ? MIN_SPEED : speed;
            velocity.normalize().multiply(speed);
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
                missileRunnable.fireMissile();
            }
        }

        public final boolean isActive() {
            return missileRunnable != null;
        }

        public final boolean isAlive() {
            return isActive() && missileRunnable.isFired();
        }

        public final void kill() {
            deathTick = ticksLived;
            isFinalTick = true;
        }

        public EnumSet<RayCastFlag> getRayCastFlags() {
            return rayCastFlags;
        }

        public long getEntityIgnoreTicks(Entity entity) {
            return entityIgnoreTicks.getOrDefault(requireNonNull(entity, "entity is null").getUniqueId(), 0L);
        }

        public void setEntityIgnoreTicks(Entity entity, long ignoreTicks) {
            requireNonNull(entity, "entity is null");
            if (ignoreTicks > 0) {
                entityIgnoreTicks.put(entity.getUniqueId(), ignoreTicks);
            } else {
                entityIgnoreTicks.remove(entity.getUniqueId());
            }
        }

        protected void awake() {
        }

        protected void start() {
        }

        protected void tick() {
        }

        protected void finalTick() {
        }

        protected boolean collideWithBlock(Block block, Vector point, BlockFace face) {
            return DEFAULT_COLLIDE_WITH_BLOCK_RESULT;
        }

        protected boolean blockProtectsEntity(Block block, Entity entity, Vector point, BlockFace face) {
            return DEFAULT_BLOCK_PROTECTS_ENTITY_RESULT;
        }

        protected boolean collideWithEntity(Entity entity) {
            return DEFAULT_COLLIDE_WITH_ENTITY_RESULT;
        }

        protected void onEntityHit(Entity entity, Vector hitOrigin, Vector hitForce) {
        }

        protected void onEntityPassed(Entity entity, Vector passOrigin, Vector passForce) {
        }

        protected void onBlockHit(Block block, Vector hitPoint, BlockFace hitFace, Vector hitForce) {
        }

        protected void onBlockPassed(Block block, Vector passPoint, BlockFace passFace, Vector passForce) {
        }

        private final class MissileRunnable extends BukkitRunnable {

            private boolean isFired = false;

            public boolean isFired() {
                return isFired;
            }

            public void fireMissile() {
                if (!isFired) {

                    runTaskTimer(Heroes.getInstance(), 1, 1);
                    isFired = true;

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

                        boolean collideWithBlock = DEFAULT_COLLIDE_WITH_BLOCK_RESULT;

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

                        long ignoreTicks = entityIgnoreTicks.getOrDefault(entity.getUniqueId(), 0L);

                        if (ignoreTicks > 0) {

                            ignoreTicks--;
                            entityIgnoreTicks.put(entity.getUniqueId(), ignoreTicks);

                            continue;
                        } else {
                            entityIgnoreTicks.remove(entity.getUniqueId());
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

                            boolean blockProtectsEntity = DEFAULT_BLOCK_PROTECTS_ENTITY_RESULT;

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

                            boolean collideWithEntity = DEFAULT_COLLIDE_WITH_ENTITY_RESULT;

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
                } else if (hitBlock != null) {
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

                        try {
                            onEntityPassed(entityCollision.entity, entityCollision.hitOrigin, hitForce);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }

                try {
                    tick();
                } catch (ExecutionError ex) {
                    ex.printStackTrace();
                }

                if (isFinalTick || ticksLived >= deathTick) {
                    cancel();
                    missileRunnable = null;

                    try {
                        finalTick();
                    } catch (ExecutionError ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    public static class PhysicsMissile extends Missile {

        public static final double MIN_GRAVITY = 0;
        public static final double MAX_GRAVITY = 2.5;
        public static final double DEFAULT_GRAVITY = 0.098;

        public static final double MIN_MASS = 1E-6;
        public static final double DEFAULT_MASS = 1;

        public static final double MIN_DRAG = 0;
        public static final double DEFAULT_DRAG = 1;

        private final Vector addedForce = new Vector();
        private final Vector gravityForce = new Vector();
        private final Vector dragForce = new Vector();

        private double gravity = DEFAULT_GRAVITY;
        private double mass = DEFAULT_MASS;
        private double drag = DEFAULT_DRAG;

        public PhysicsMissile(ProjectileSource shooter, World world, Vector origin, Vector velocity, String tag) {
            super(shooter, world, origin, velocity, tag);
        }

        public PhysicsMissile(ProjectileSource shooter, Location location, double speed, String tag) {
            super(shooter, location, speed, tag);
        }

        public PhysicsMissile(ProjectileSource shooter, Location location, String tag) {
            super(shooter, location, tag);
        }

        public PhysicsMissile(ProjectileSource shooter, World world, Vector origin, Vector velocity) {
            super(shooter, world, origin, velocity);
        }

        public PhysicsMissile(ProjectileSource shooter, Location location, double speed) {
            super(shooter, location, speed);
        }

        public PhysicsMissile(ProjectileSource shooter, Location location) {
            super(shooter, location);
        }

        public PhysicsMissile(LivingEntity shooter, double speed, String tag) {
            super(shooter, speed, tag);
        }

        public PhysicsMissile(LivingEntity shooter, String tag) {
            super(shooter, tag);
        }

        public PhysicsMissile(LivingEntity shooter, double speed) {
            super(shooter, speed);
        }

        public PhysicsMissile(LivingEntity shooter) {
            super(shooter);
        }

        public PhysicsMissile(LivingEntity shooter, Vector origin, Vector velocity, String tag) {
            super(shooter, origin, velocity, tag);
        }

        public PhysicsMissile(LivingEntity shooter, Vector origin, Vector velocity) {
            super(shooter, origin, velocity);
        }

        public double getGravity() {
            return gravity;
        }

        public void setGravity(double gravity) {
            this.gravity = gravity > MAX_GRAVITY ? MAX_GRAVITY : gravity < MIN_GRAVITY ? MIN_GRAVITY : gravity;
        }

        public double getMass() {
            return mass;
        }

        public void setMass(double mass) {
            this.mass = mass < MIN_MASS ? MIN_MASS : mass;
        }

        public double getDrag() {
            return drag;
        }

        public void setDrag(double drag) {
            this.drag = drag < MIN_DRAG ? MIN_DRAG : drag;
        }

        public void addForce(Vector force) {
            addedForce.add(force);
        }

        @Override
        protected void tick() {

            gravityForce.setY(-gravity * mass);
            addedForce.add(gravityForce);

            dragForce.copy(addedForce)
                    .multiply(dragForce)                    // Square the velocity
                    .multiply(drag)                         // Apply drag
                    .multiply(calcCrossSectionalArea())     // Apply area
                    .multiply(0.5);                         // Divide by 2

            getVelocity().add(divide(addedForce.subtract(dragForce), mass));

            addedForce.zero(); // Zero out the added force vector to prepare it for the next match
        }

        private Vector divide(Vector vector, double value) {
            vector.setX(vector.getX() / value);
            vector.setY(vector.getY() / value);
            vector.setZ(vector.getZ() / value);
            return vector;
        }

        private double calcCrossSectionalArea() {
            return NumberConversions.square(getRadius()) * Math.PI;
        }
    }

    private static class BlockCollision {

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

    private static class EntityCollision {

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
