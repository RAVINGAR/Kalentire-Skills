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

				List<Entity> targets = physics.getEntitiesInVolume(world, hero.getPlayer(), shot, new Predicate<Entity>() {
					@Override
					public boolean apply(Entity entity) {
						if (entity instanceof LivingEntity && !hits.contains(entity.getUniqueId())) {
							AABB entityAABB = physics.getEntityAABB(entity);
							Vector shotRay = shot.getPoint2().subtract(shot.getPoint1());
							double lengthSq = shotRay.lengthSquared();
							double dot = shotRay.dot(entityAABB.getCenter().subtract(shot.getPoint1()));

							Vector shotPoint;
							if (dot <= 0) {
								shotPoint = shot.getPoint1();
							} else if (dot > lengthSq) {
								shotPoint = shot.getPoint2();
							} else {
								shotPoint = shot.getPoint1().add(shotRay.multiply(dot / lengthSq));
							}

							return physics.rayCastBlocks(world, shotPoint, entityAABB.getCenter(), blockFilter, flags) == null;
						}

						return false;
					}
				}, flags.contains(RayCastFlag.ENTITY_HIT_SPECTATORS));

				Collections.sort(targets, new Comparator<Entity>() {
					@Override
					public int compare(Entity o1, Entity o2) {
						return Double.compare(physics.getEntityAABB(o1).getCenter().distanceSquared(origin),
								physics.getEntityAABB(o2).getCenter().distanceSquared(origin));
					}
				});

				for (Entity target : targets) {

					// TODO I COULD TOTALLY MAKE IT SO ARROWS COULD STOP ITS PATH WHEN FIRED INTO IT

					AABB entityAABB = physics.getEntityAABB(target);
					Vector shotRay = shot.getPoint2().subtract(shot.getPoint1());
					double lengthSq = shotRay.lengthSquared();
					double dot = shotRay.dot(entityAABB.getCenter().subtract(shot.getPoint1()));

					if (dot > 0) {
						hits.add(target.getUniqueId());
						if (hits.size() > penetration) {
							hitAction.onFinalHit(hero, (LivingEntity) target, origin.toLocation(world), shot);

							if (dot < lengthSq) {
								Vector renderShotEnd = shot.getPoint1().add(shotRay.multiply(dot / lengthSq));
								if (origin.distanceSquared(renderShotEnd) > square(range)) {
									renderShotEnd = origin.clone().add(directionNormal.clone().multiply(range));
								}
								shot = physics.createCapsule(shot.getPoint1(), renderShotEnd, shot.getRadius());
							}

							finalTick = true;
							break;
						} else {
							hitAction.onHit(hero, (LivingEntity) target, origin.toLocation(world), shot);
						}
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
}
