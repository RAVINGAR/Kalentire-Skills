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
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.CylinderEffect;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.Color;
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
			private Location originLocation;
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
				originLocation = origin.toLocation(world).setDirection(directionNormal);

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
                        hitAction.onFinalHit(hero, possibleHit.getTarget(), originLocation.clone(), shot);
                        shot = physics.createCapsule(shot.getPoint1(), possibleHit.getShotPoint(), shot.getRadius());

                        finalTick = true;
                        break;
                    } else {
                        hitAction.onHit(hero, possibleHit.getTarget(), originLocation.clone(), shot);
                    }
                }

				hitAction.onRenderShot(originLocation.clone(), shot, firstTick, finalTick);
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

	protected void renderBeamShotFrame(Location origin, Capsule shot, ParticleEffect particle, Color color,
                                       int particles, int iterations, float visibleRange, double radiusScale, double startOffset) {
		boolean render = false;
		Vector originV = origin.toVector();

		Vector start = null, end = null;

		if (originV.distanceSquared(shot.getPoint1()) >= square(startOffset)) {
			start = shot.getPoint1();
			end = shot.getPoint2();
			render = true;
		} else if (originV.distanceSquared(shot.getPoint2()) >= square(startOffset)) {
			start = shot.getPoint1().add(shot.getPoint2().subtract(shot.getPoint1()).normalize());
			end = shot.getPoint2();
			render = true;
		}

		if (render) {
			EffectManager em = new EffectManager(plugin);

			CylinderEffect cyl = new CylinderEffect(em);
			cyl.setLocation(start.clone().add(end.clone().subtract(start).multiply(0.5)).toLocation(origin.getWorld()));
			cyl.asynchronous = true;

			cyl.radius = (float) (shot.getRadius() * radiusScale);
			cyl.height = (float) start.distance(end);
			cyl.particle = particle;
			cyl.color = color;
			cyl.particles = particles;
			cyl.solid = true;
			cyl.rotationX = Math.toRadians(origin.getPitch() + 90);
			cyl.rotationY = -Math.toRadians(origin.getYaw());
			cyl.angularVelocityX = 0;
			cyl.angularVelocityY = 0;
			cyl.angularVelocityZ = 0;
			cyl.iterations = iterations;
			cyl.visibleRange = visibleRange;

			cyl.start();
			em.disposeOnTermination();
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
