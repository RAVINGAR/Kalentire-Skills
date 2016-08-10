package com.herocraftonline.heroes.characters.skill.skills;

import com.google.common.base.Predicate;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.nms.physics.NMSPhysics;
import com.herocraftonline.heroes.nms.physics.RayCastFlag;
import com.herocraftonline.heroes.nms.physics.RayCastHit;
import com.herocraftonline.heroes.nms.physics.collision.AABB;
import com.herocraftonline.heroes.nms.physics.collision.Capsule;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.Supplier;

public abstract class SkillBaseHomingMissile extends TargettedSkill {

    private static NMSPhysics physics = NMSHandler.getInterface().getNMSPhysics();

    public SkillBaseHomingMissile(Heroes plugin, String name) {
        super(plugin, name);
    }

    protected void fireHomingMissile(final Hero hero, final boolean ignoreHero,
                                     final Supplier<Vector> targetSupplier, final Vector startPosition, final long duration,
                                     final Vector startVelocity, final double homingStrength, final double maxSpeed, final double missileRadius,
                                     final Predicate<Entity> entityFilter, final Predicate<Block> blockFilter,
                                     final EnumSet<RayCastFlag> flags) {

        new BukkitRunnable() {

            private final World world = hero.getPlayer().getWorld();
            private final Set<UUID> handledEntities = new HashSet<>();

            private Vector lastPosition = null;
            private Vector position = startPosition;
            private Vector velocity = startVelocity;

            private Capsule missileCollider = null;

            private long life = duration;

            @Override
            public void run() {

                Vector targetPosition = targetSupplier.get();
                Vector homingForce = targetPosition.clone().subtract(position).normalize().multiply(homingStrength);

                velocity.add(homingForce);
                if (velocity.lengthSquared() > NumberConversions.square(maxSpeed)) {
                    velocity.normalize().multiply(maxSpeed);
                }

                lastPosition.copy(position);
                position.add(velocity);

                Iterator<RayCastHit> hitBlockIterator = physics.rayCastBlocksAll(world, lastPosition, position, flags);
                while (hitBlockIterator.hasNext()) {

                    RayCastHit blockHit = hitBlockIterator.next();
                    Block block = blockHit.getBlock(world);
                    Vector hitPosition = blockHit.getPoint();
                    BlockFace hitFace = blockHit.getFace();

                    if (blockFilter.apply(block)) {

                        try {
                            onBlockHit(hero, block, hitPosition.clone(), velocity.clone(), hitFace);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                        position.copy(hitPosition);
                        life = 0;

                    } else {
                        try {
                            onBlockPassed(hero, block, hitPosition.clone(), velocity.clone(), hitFace);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }

                // TODO: Implement a way to set the capsule orientation... why didn't I add that...
                missileCollider = physics.createCapsule(lastPosition, position, missileRadius);

                List<Entity> possibleTargets = physics.getEntitiesInVolume(world, hero.getPlayer(),
                        missileCollider, flags.contains(RayCastFlag.ENTITY_HIT_SPECTATORS));

                Entity hitEntity = null;
                Vector currentEntityCenter = null;
                Vector hitOrigin = null;
                double currentTargetDistanceSq = Long.MAX_VALUE;

                List<PassedEntity> passedEntities = new LinkedList<>();

                Vector missileRay = missileCollider.getPoint2().subtract(missileCollider.getPoint1());
                double lengthSq = missileRay.lengthSquared();

                for (Entity possibleTarget : possibleTargets) {

                    if (handledEntities.contains(possibleTarget.getUniqueId())) continue;

                    AABB entityAABB = physics.getEntityAABB(possibleTarget);
                    Vector entityCenter = entityAABB.getCenter();
                    double dot = missileRay.dot(entityCenter.clone().subtract(missileCollider.getPoint1()));

                    Vector missileHit;
                    if (dot < 0) {
                        continue;
                    } else if (dot == 0) {
                        missileHit = missileCollider.getPoint1();
                    } else if (dot >= lengthSq) {
                        missileHit = missileCollider.getPoint2();
                    } else {
                        missileHit = missileCollider.getPoint1().add(missileRay.clone().multiply(dot / lengthSq));
                    }

                    double shotLengthSq = missileRay.lengthSquared();

                    if (physics.rayCastBlocks(world, missileHit, entityAABB.getCenter(), blockFilter, flags) == null) {

                        double distanceSq = missileRay.clone().multiply(dot / shotLengthSq).lengthSquared();

                        if (entityFilter.apply(possibleTarget)) {
                            if (distanceSq < currentTargetDistanceSq) {
                                hitEntity = possibleTarget;
                                currentEntityCenter = entityCenter;
                                hitOrigin = missileHit;
                                currentTargetDistanceSq = distanceSq;
                            }
                        } else {
                            passedEntities.add(new PassedEntity(possibleTarget, entityCenter, distanceSq, missileHit));
                        }

                        handledEntities.add(possibleTarget.getUniqueId());
                    }
                }

                for (PassedEntity passedEntity : passedEntities) {
                    if (passedEntity.distanceSq <= currentTargetDistanceSq) {
                        Vector hitForce = passedEntity.entityCenter.clone().subtract(passedEntity.hitOrigin).add(missileRay);

                        try {
                            onEntityPassed(hero, passedEntity.entity, passedEntity.hitOrigin.clone(), hitForce);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }

                if (hitEntity != null) {

                    Vector hitForce = currentEntityCenter.clone().subtract(hitOrigin).add(missileRay);

                    try {
                        onEntityHit(hero, hitEntity, hitOrigin.clone(), hitForce);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    position.copy(hitOrigin);
                    life = 0;
                }

                try {
                    renderMissilePath(world, lastPosition.clone(), position.clone());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                if (--life <= 0) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    protected abstract void onEntityHit(Hero hero, Entity entity, Vector hitOrigin, Vector hitForce);

    protected void onEntityPassed(Hero hero, Entity entity, Vector hitOrigin, Vector hitForce) { }

    protected void onBlockHit(Hero hero, Block block, Vector hitPosition, Vector hitForce, BlockFace hitFace) { }

    protected void onBlockPassed(Hero hero, Block block, Vector hitPosition, Vector hitForce, BlockFace hitFace) { }

    protected abstract void renderMissilePath(World world, Vector start, Vector end);

    private class PassedEntity {

        public final Entity entity;
        public final Vector entityCenter;
        public final double distanceSq;
        public final Vector hitOrigin;

        public PassedEntity(Entity entity, Vector entityCenter, double distanceSq, Vector hitOrigin) {
            this.entity = entity;
            this.entityCenter = entityCenter;
            this.distanceSq = distanceSq;
            this.hitOrigin = hitOrigin;
        }
    }
}
