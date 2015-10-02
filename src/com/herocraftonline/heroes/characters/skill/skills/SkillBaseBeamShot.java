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

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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
			private Vector direction;
			private Capsule shot;
			private boolean finalTick = false;

			private Set<UUID> hits = new HashSet<>();

			{
				world = hero.getPlayer().getWorld();
				origin = hero.getPlayer().getEyeLocation().toVector();
				direction = hero.getPlayer().getEyeLocation().getDirection().multiply(velocity);

				Vector shotEnd;

				if (velocity < range) {
					shotEnd = origin.clone().add(direction);
				} else {
					shotEnd = origin.clone().add(hero.getPlayer().getEyeLocation().getDirection().multiply(range));
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

				for (Entity target : physics.getEntitiesInVolume(world, hero.getPlayer(), shot, new Predicate<Entity>() {
					@Override
					public boolean apply(Entity entity) {
						if (entity instanceof LivingEntity && !hits.contains(entity.getUniqueId())) {
							AABB entityAABB = physics.getEntityAABB(entity);
							Vector shotRay = shot.getPoint2().subtract(shot.getPoint1());
							double lengthSq = shotRay.lengthSquared();
							double dot = shotRay.dot(entityAABB.getCenter());

							Vector shotPoint;
							if (dot <= 0) {
								shotPoint = shot.getPoint1();
							} else if (dot > lengthSq) {
								shotPoint = shot.getPoint2();
							} else {
								shotPoint = shotRay.multiply(dot / lengthSq);
							}

							return physics.rayCastBlocks(world, shotPoint, entityAABB.getCenter(), blockFilter, flags) == null;
						}

						return false;
					}
				}, flags.contains(RayCastFlag.ENTITY_HIT_SPECTATORS))) {

					// TODO I COULD TOTALLY MAKE IT SO ARROWS COULD STOP ITS PATH WHEN FIRED INTO IT

					hits.add(target.getUniqueId());
					if (hits.size() > penetration) {
						hitAction.onFinalHit(hero, (LivingEntity) target, origin.toLocation(world), shot);

						AABB entityAABB = physics.getEntityAABB(target);
						Vector shotRay = shot.getPoint2().subtract(shot.getPoint1());
						double lengthSq = shotRay.lengthSquared();
						double dot = shotRay.dot(entityAABB.getCenter());

						Vector newEndPoint;
						if (dot < lengthSq) {
							newEndPoint = shotRay.multiply(dot / lengthSq);
						} else if (dot <= 0) {
							newEndPoint = shot.getPoint1();
						} else {
							newEndPoint = shot.getPoint2();
						}

						// For final rendering
						shot = physics.createCapsule(shot.getPoint1(), newEndPoint, shot.getRadius());

						finalTick = true;
						break;
					} else {
						hitAction.onHit(hero, (LivingEntity) target, origin.toLocation(world), shot);
					}
				}

				hitAction.onRenderShot(origin.toLocation(world), shot);

				if (finalTick) {
					cancel();
				} else {
					// Point2 is shotEnd because it was declared as the second vector when creating the original shot.
					Vector newShotEnd = shot.getPoint2().add(direction);

					if (origin.distanceSquared(newShotEnd) > square(range)) {
						newShotEnd = origin.clone().add(hero.getPlayer().getEyeLocation().getDirection().multiply(range));
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
		void onRenderShot(Location origin, Capsule shot);
	}
}
