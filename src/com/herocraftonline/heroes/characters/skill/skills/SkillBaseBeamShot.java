package com.herocraftonline.heroes.characters.skill.skills;

import com.google.common.base.Predicate;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.nms.physics.NMSPhysics;
import com.herocraftonline.heroes.nms.physics.RayCastFlag;
import com.herocraftonline.heroes.nms.physics.RayCastHit;
import com.herocraftonline.heroes.nms.physics.collision.AABB;
import com.herocraftonline.heroes.nms.physics.collision.Capsule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import static org.bukkit.util.NumberConversions.square;

public abstract class SkillBaseBeamShot extends ActiveSkill {

	protected static final String VELOCITY_NODE = "velocity";
	protected static final String PENETRATION_NODE = "penetration";

	private static NMSPhysics physics = NMSHandler.getInterface().getNMSPhysics();

	public SkillBaseBeamShot(Heroes plugin, String name) {
		super(plugin, name);
	}

	protected void fireBeamShot(final Hero hero, final double range, final double radius, final double velocity, final double penetration,
	                            final BeamShotHit hitAction, final Predicate<Block> blockFilter, final EnumSet<RayCastFlag> flags) {

		new BukkitRunnable() {

			private World world;
			private Vector origin;
			private Vector directionNormal;
			private Vector direction;
			private Capsule shot;
			private boolean firstTick = true;
			private boolean finalTick = false;

			private Set<UUID> hits = new HashSet<>();

			{
				world = hero.getPlayer().getWorld();
				origin = hero.getPlayer().getEyeLocation().toVector();
				directionNormal = hero.getPlayer().getEyeLocation().getDirection();

				direction = directionNormal.clone().multiply(velocity);

				Vector shotEnd;

				if (velocity < range) {
					shotEnd = origin.clone().add(direction);
				} else {
					shotEnd = origin.clone().add(directionNormal.clone().multiply(range));
					finalTick = true;
				}

				RayCastHit blockHit = physics.rayCastBlocks(world, origin, shotEnd, blockFilter, flags);
				if (blockHit != null) {
					shotEnd = blockHit.getPoint();
					finalTick = true;
				}

				shot = physics.createCapsule(origin, shotEnd, radius);

				runTaskTimer(plugin, 1, 1);
			}

			@Override
			public void run() {

				List<Entity> possibleTargets = physics.getEntitiesInVolume(world, hero.getPlayer(), shot, new Predicate<Entity>() {
					@Override
					public boolean apply(Entity entity) {
                        return entity instanceof LivingEntity && !hits.contains(entity.getUniqueId());
					}
				}, flags.contains(RayCastFlag.ENTITY_HIT_SPECTATORS));

                SortedSet<PossibleHit> possibleHits = new TreeSet<>();
                for (Entity possibleTarget : possibleTargets) {
                    AABB entityAABB = physics.getEntityAABB(possibleTarget);
                    Vector shotRay = shot.getPoint2().subtract(shot.getPoint1());
                    double lengthSq = shotRay.lengthSquared();
                    double dot = shotRay.dot(entityAABB.getCenter().subtract(shot.getPoint1()));

                    Vector shotPoint;
                    if (dot < 0) {
                        continue;
                    } else if (dot == 0) {
                        shotPoint = shot.getPoint1();
                    } else if (dot >= lengthSq) {
                        shotPoint = shot.getPoint2();
                    } else {
                        shotPoint = shot.getPoint1().add(shotRay.multiply(dot / lengthSq));
                    }

                    double shotLengthSq = shotRay.lengthSquared();

                    if (physics.rayCastBlocks(world, shotPoint, entityAABB.getCenter(), blockFilter, flags) == null) {
                        double distanceSq = shotRay.clone().multiply(dot / shotLengthSq).lengthSquared();
                        possibleHits.add(new PossibleHit((LivingEntity) possibleTarget, distanceSq, shotPoint));
                    }
                }

                for (PossibleHit possibleHit : possibleHits) {
                    hits.add(possibleHit.getTarget().getUniqueId());
                    if (hits.size() > penetration) {
                        hitAction.onFinalHit(hero, possibleHit.getTarget(), origin.toLocation(world), shot);
                        shot = physics.createCapsule(shot.getPoint1(), possibleHit.getShotPoint(), shot.getRadius());

                        finalTick = true;
                        break;
                    } else {
                        hitAction.onHit(hero, possibleHit.getTarget(), origin.toLocation(world), shot);
                    }
                }

				hitAction.onRenderShot(origin.toLocation(world), shot, firstTick, finalTick);
				firstTick = false;

				if (finalTick) {
					cancel();
				} else {
					// Point2 is shotEnd because it was declared as the second vector when creating the original shot.
					Vector newShotEnd = shot.getPoint2().add(direction);

					if (origin.distanceSquared(newShotEnd) > square(range)) {
						newShotEnd = origin.clone().add(directionNormal.clone().multiply(range));
						finalTick = true;
					}

					RayCastHit blockHit = physics.rayCastBlocks(world, shot.getPoint2(), newShotEnd, blockFilter, flags);
					if (blockHit != null) {
						newShotEnd = blockHit.getPoint();
						finalTick = true;
					}

					shot = physics.createCapsule(shot.getPoint2(), newShotEnd, radius);
				}
			}
		};
	}

	protected interface BeamShotHit {
		void onHit(Hero hero, LivingEntity target, Location origin, Capsule shot);
		void onFinalHit(Hero hero, LivingEntity target, Location origin, Capsule shot);
		void onRenderShot(Location origin, Capsule shot, boolean first, boolean last);
	}

	protected abstract class SpiralBeamShotHit implements BeamShotHit {



		@Override
		public void onRenderShot(Location origin, Capsule shot, boolean first, boolean last) {

		}
	}

	private class PossibleHit implements Comparable<PossibleHit> {

		private LivingEntity target;
		private double distanceSq;
        private Vector shotPoint;

		public PossibleHit(LivingEntity target, double distanceSq, Vector shotPoint) {
			this.target = target;
			this.distanceSq = distanceSq;
            this.shotPoint = shotPoint;
		}

		public LivingEntity getTarget() {
			return target;
		}

        public Vector getShotPoint() {
            return shotPoint;
        }

		@Override
		public int compareTo(PossibleHit o) {
			return Double.compare(distanceSq, o.distanceSq);
		}
	}
}
