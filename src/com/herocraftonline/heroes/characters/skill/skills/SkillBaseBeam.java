package com.herocraftonline.heroes.characters.skill.skills;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Optional;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.nms.physics.RayCastFlag;
import com.herocraftonline.heroes.nms.physics.RayCastHit;
import com.herocraftonline.heroes.util.Pair;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.CylinderEffect;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class SkillBaseBeam extends ActiveSkill {

	protected static final String BEAM_MAX_LENGTH_NODE = "beam-max-length";
	protected static final String BEAM_RADIUS_NODE = "beam-radius";

	public SkillBaseBeam(Heroes plugin, String name) {
		super(plugin, name);
	}

	protected final void castBeam(final Hero hero, final Beam beam, final TargetHandler targetHandler) {

		//final List<Entity> possibleTargets = hero.getPlayer().getNearbyEntities(beam.bounds, beam.bounds, beam.bounds);

		// Check post 2 out from here https://www.spigotmc.org/threads/invisible-entity-or-getnearbyentities-from-location.46013/
		final Set<Entity> possibleTargets = getEntitiesInChunks(beam.midPoint().toLocation(hero.getPlayer().getWorld()), beam.chunkRadius);

		Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				final List<Pair<LivingEntity, Beam.PointData>> targets = new ArrayList<>();

				for (Entity possibleTarget : possibleTargets) {

					if (possibleTarget instanceof LivingEntity && !possibleTarget.equals(hero.getPlayer())) {

						final LivingEntity target = (LivingEntity) possibleTarget;
						final Optional<Beam.PointData> pointData = beam.calculatePointData(target.getEyeLocation().toVector());

						if (pointData.isPresent()) {
							Bukkit.getServer().getScheduler().runTask(plugin, new Runnable() {
								@Override
								public void run() {
									targetHandler.handle(hero, target, pointData.get());
								}
							});
						}
					}

					/*if (possibleTarget instanceof LivingEntity && !possibleTarget.equals(hero.getPlayer())) {

						LivingEntity target = (LivingEntity) possibleTarget;
						Optional<Beam.PointData> pointData = beam.calculatePointData(target.getEyeLocation().toVector());

						if (pointData.isPresent()) {
							targets.add(new Pair<>(target, pointData.get()));
						}
					}

					Bukkit.getServer().getScheduler().runTask(plugin, new Runnable() {
						@Override
						public void run() {
							for (Pair<LivingEntity, Beam.PointData> targetPair : targets) {
								targetHandler.handle(hero, targetPair.getLeft(), targetPair.getRight());
							}
						}
					});*/


				}
			}
		});



		// Non async entity targeting
		/*List<Entity> possibleTargets = hero.getPlayer().getNearbyEntities(beam.rangeBounds, beam.rangeBounds, beam.rangeBounds);
		for (Entity possibleTarget : possibleTargets) {
			LivingEntity target;
			if (possibleTarget instanceof LivingEntity
					&& targetFilter.apply(target = (LivingEntity) possibleTarget) && !possibleTarget.equals(hero.getPlayer())) {
				Optional<Beam.PointData> pointData = beam.calculatePointData(target.getEyeLocation().toVector());
				if (pointData.isPresent()) {
					onTargetHit(hero, target, pointData.get());
				}
			}
		}*/
	}

	private static Set<Entity> getEntitiesInChunks(Location l, int chunkRadius) {
		Set<Entity> entities = new HashSet<>();

		// TODO Test which one is more efficient.

		Chunk origin = l.getChunk();
		for (int x = -chunkRadius; x <= chunkRadius; x++) {
			for (int z = -chunkRadius; z <= chunkRadius; z++) {
				for (Entity e : origin.getWorld().getChunkAt(origin.getX() + x, origin.getZ() + z).getEntities()) {
					entities.add(e);
				}
			}
		}

		/*Block b = l.getBlock();
		for (int x = -16 * chunkRadius; x <= 16 * chunkRadius; x += 16) {
			for (int z = -16 * chunkRadius; z <= 16 * chunkRadius; z += 16) {
				for (Entity e : b.getRelative(x, 0, z).getChunk().getEntities()) {
					entities.add(e);
				}
			}
		}
		*/

		return entities;
	}

	protected void renderBeam(Location start, Beam beam, ParticleEffect particle, Color color, int particles, int iterations, float visibleRange, double radiusScale, double startOffset) {
		EffectManager em = new EffectManager(plugin);

		CylinderEffect cyl = new CylinderEffect(em);
		cyl.setLocation(beam.midPoint().toLocation(start.getWorld()).add(beam.getTrajectory().normalize().multiply(startOffset / 2)));
		cyl.asynchronous = true;

		cyl.radius = (float) (beam.radius() * radiusScale);
		cyl.height = (float) (beam.length() - startOffset);
		cyl.particle = particle;
		cyl.color = color;
		cyl.particles = particles;
		cyl.solid = true;
		cyl.rotationX = Math.toRadians(start.getPitch() + 90);
		cyl.rotationY = -Math.toRadians(start.getYaw());
		cyl.angularVelocityX = 0;
		cyl.angularVelocityY = 0;
		cyl.angularVelocityZ = 0;
		cyl.iterations = iterations;
		cyl.visibleRange = visibleRange;

		cyl.start();
		em.disposeOnTermination();
	}

	protected void renderBeam(Location start, Beam beam, ParticleEffect particle, int particles, int iterations, float visibleRange, double radiusScale, double startOffset) {
		renderBeam(start, beam, particle, Color.WHITE, particles, iterations, visibleRange, radiusScale, startOffset);
	}

	protected void renderEyeBeam(Player player, Beam beam, ParticleEffect particle, Color color, int particles, int iterations, float visibleRange, double radiusScale, double eyeOffset) {
		renderBeam(player.getEyeLocation(), beam, particle, color, particles, iterations, visibleRange, radiusScale, eyeOffset);
	}

	protected void renderEyeBeam(Player player, Beam beam, ParticleEffect particle, int particles, int iterations, float visibleRange, double radiusScale, double eyeOffset) {
		renderEyeBeam(player, beam, particle, Color.WHITE, particles, iterations, visibleRange, radiusScale, eyeOffset);
	}

	public interface TargetHandler {
		void handle(Hero hero, LivingEntity target, Beam.PointData pointData);
	}

	protected static Beam createBeam(Vector origin, Vector trajectory, double radius) {
		return new Beam(origin, trajectory, radius);
	}

	protected static Beam createBeam(Vector origin, Vector direction, double length, double radius) {
		return createBeam(origin, origin.add(direction.normalize().multiply(length)), radius);
	}

	protected static Beam createBeam(Location origin, double length, double radius) {
		return createBeam(origin.toVector(), origin.getDirection().multiply(length), radius);
	}

	protected static Beam createObstructedBeam(World world, Vector origin, Vector direction, int maxLength, double radius) {
		Vector start = origin, end = direction.clone().normalize().multiply(maxLength).add(start);
		RayCastHit hit = NMSHandler.getInterface().getNMSPhysics().rayCastBlocks(world, start, end, RayCastFlag.BLOCK_IGNORE_NON_SOLID, RayCastFlag.BLOCK_HIGH_DETAIL);
		Vector trajectory;

		if (hit != null) {
			trajectory = hit.getPoint().subtract(start);
		} else {
			trajectory = end.subtract(start);
		}

		return createBeam(start, trajectory, radius);
	}

	protected static Beam createObstructedBeam(Location origin, int maxLength, double radius) {
		Vector start = origin.toVector(), end = origin.getDirection().multiply(maxLength).add(start);
		RayCastHit hit = NMSHandler.getInterface().getNMSPhysics().rayCastBlocks(origin.getWorld(), start, end, RayCastFlag.BLOCK_IGNORE_NON_SOLID, RayCastFlag.BLOCK_HIGH_DETAIL);
		Vector trajectory;

		if (hit != null) {
			trajectory = hit.getPoint().subtract(start);
		} else {
			trajectory = end.subtract(start);
		}

		return createBeam(start, trajectory, radius);
	}

	/*
		NOTE: It is probably not a good idea to pass in a block iterator without a max distance set.
			  Keep that in mind whoever is working on the internals of this class.
	 */
	private static Block getTargetBlock(BlockIterator blockIterator, Set<Material> transparent) {
		Block block = null;
		while (blockIterator.hasNext()) {
			block = blockIterator.next();
			if (transparent != null) {
				if (!transparent.contains(block.getType())) {
					return block;
				}
			}
			else {
				if (block.getType() == Material.AIR) {
					return block;
				}
			}
		}
		return block;
	}

	/*
		Math involved with this class comes from http://www.flipcode.com/archives/Fast_Point-In-Cylinder_Test.shtml

		See that site for more documentation about the following math.
	 */
	protected static final class Beam {

		private final double ox, oy, oz;        // Beam origin vector
		private final double tx, ty, tz;        // Beam direction vector
		private final double length;            // Length of the beam
		private final double radius;            // Radius of the beam

		private final int chunkRadius;       // Pre-calculated chunk radius for filtering target entities.

		// TODO Should this be predetermined?
		//private final boolean roundedCaps;    // Does this beam have rounded caps (is it a capsule)

		private Beam(double ox, double oy, double oz, double tx, double ty, double tz, double length, double radius) {
			checkArgument(length > 0, "Beam length must be greater than 0");
			checkArgument(radius > 0, "Beam radius must be greater than 0");

			this.ox = ox;
			this.oy = oy;
			this.oz = oz;
			this.tx = tx;
			this.ty = ty;
			this.tz = tz;
			this.length = length;
			this.radius = radius;

			chunkRadius = (int) ((Math.sqrt(tx * tx + tz * tz) / 2 + radius + 16) / 16);
		}

		private Beam(Vector origin, Vector trajectory, double radius) {
			this(origin.getX(), origin.getY(), origin.getZ(), trajectory.getX(), trajectory.getY(), trajectory.getZ(), trajectory.length(), radius);
		}

		// TODO Implement a way to create new beams from existing beams and reuse information from the old beam
		/*public Beam(Location origin, Vector beam, double radius) {
			this(origin.toVector(), beam, radius);
		}

		public Beam withAlteredOrigin(Vector origin) {
			return new Beam(origin.getX(), origin.getY(), origin.getZ(), dx, dy, dz, length, radius);
		}

		public Beam withAlteredOrigin(Location origin) {
			return withAlteredOrigin(origin.toVector());
		}

		public Beam withTranslatedOrigin(Vector v) {
			return withAlteredOrigin(getOrigin().add(v));
		}

		public Beam withAlteredTrajectory(Vector beam) {
			return new Beam(ox, oy, oz, beam.getX(), beam.getY(), beam.getZ(), beam.length(), radius);
		}

		public Beam withAlteredLength(double length) {
			return new Beam(getOrigin(), getDirection(), length, radius);
		}*/

		public double getOriginX() {
			return ox;
		}

		public double getOriginY() {
			return oy;
		}

		public double getOriginZ() {
			return oz;
		}

		public Vector getOrigin() {
			return new Vector(getOriginX(), getOriginY(), getOriginZ());
		}

		public double getTrajectoryX() {
			return tx;
		}

		public double getTrajectoryY() {
			return ty;
		}

		public double getTrajectoryZ() {
			return tz;
		}

		public Vector getTrajectory() {
			return new Vector(getTrajectoryX(), getTrajectoryY(), getTrajectoryZ());
		}

		public Vector midPoint() {
			return getOrigin().midpoint(getOrigin().add(getTrajectory()));
		}

		public double length() {
			return length;
		}

		public double lengthSquared() {
			return length * length;
		}

		public double radius() {
			return radius;
		}

		public double radiusSquared() {
			return radius * radius;
		}

		public Optional<PointData> calculatePointData(Vector point, boolean roundedCap) {
			double pdx, pdy, pdz;       // Vector from origin to point (point distance)
			double dot;                 // Reference to dot product of vector[pd] (point distance) and vector[o] (origin)

			// TODO Determine if caching these here is beneficial
			//double lengthSq = lengthSquared();
			//double radiusSq = radiusSquared();

			pdx = point.getX() - ox;
			pdy = point.getY() - oy;
			pdz = point.getZ() - oz;

			dot = pdx * tx + pdy * ty + pdz * tz;

			// If the dot product is not within the range of the beam shaft...
			if (dot < 0 || dot > lengthSquared()) {
				// ... check if we test for rounded caps...
				if (roundedCap) {
					// ... if so declare a reference point...
					Vector refPoint = getOrigin();
					// ... and a reference distance squared.
					double refDistanceSq;

					// If the distance squared from the refPoint to point is <= radius squared...
					if ((refDistanceSq = refPoint.distanceSquared(point)) <= radiusSquared()) {
						// ... return a point data of location origin cap as the current reference is origin
						return Optional.of(new PointData(point, pdx, pdy, pdz, refDistanceSq, dot, PointLocation.ORIGIN_CAP));
					}
					// If the distance squared from the refPoint to point is <= radius squared...
					else if ((refDistanceSq = refPoint.add(getTrajectory()).distanceSquared(point)) <= lengthSquared()) {
						// ... return a point data of location end cap as the current ref point is the opposite origin.
						return Optional.of(new PointData(point, pdx, pdy, pdz, refDistanceSq, dot, PointLocation.END_CAP));
					}
				}

				// Return absent if no testing for rounded caps, or no passing rounding cap tests.
				return Optional.absent();
			}
			else {
				// ... else test for beam shaft radius (that sounds so... dirty...)
				// This is a fancy way to check if the point is within the radius of the cylinder without trigonometric functions
				double dsq = (pdx * pdx + pdy * pdy + pdz * pdz) - dot * dot / lengthSquared();

				// if distance squared from beam is <= radius squared...
				if (dsq <= radiusSquared()) {
					// ... return a point data of location beam shaft.
					return Optional.of(new PointData(point, pdx, pdy, pdz, dsq, dot, PointLocation.BEAM_SHAFT));
				}
				else {
					// ... else return absent.
					return Optional.absent();
				}
			}
		}

		public Optional<PointData> calculatePointData(Vector point) {
			return calculatePointData(point, false);
		}

		public final class PointData {

			private final double px, py, pz;            // The point tested
			private final double pdx, pdy, pdz;         // The vector from the beam origin to the point
			private final double distanceFromBeamSq;    // Distance squared from the line that represents the beam
			private final double dotProduct;            // The dot product between the beam and the point
			private final PointLocation pointLocation;       // Section of the beam the point resides.

			private PointData(Vector point, double pdx, double pdy, double pdz,
			                  double distanceFromBeamSq, double dotProduct, PointLocation pointLocation) {
				px = point.getX();
				py = point.getY();
				pz = point.getZ();
				this.pdx = pdx;
				this.pdy = pdy;
				this.pdz = pdz;
				this.distanceFromBeamSq = distanceFromBeamSq;
				this.dotProduct = dotProduct;
				this.pointLocation = pointLocation;
			}

			public Beam getBeam() {
				return Beam.this;
			}

			public double getPointX() {
				return px;
			}

			public double getPointY() {
				return py;
			}

			public double getPointZ() {
				return pz;
			}

			public Vector getPoint() {
				return new Vector(getPointX(), getPointY(), getPointZ());
			}

			// TODO Direction I feel is a bad name for the relative vector from origin to point, change in the future
			public double getDirectionX() {
				return pdx;
			}

			// TODO Direction I feel is a bad name for the relative vector from origin to point, change in the future
			public double getDirectionY() {
				return pdy;
			}

			// TODO Direction I feel is a bad name for the relative vector from origin to point, change in the future
			public double getDirectionZ() {
				return pdz;
			}

			// TODO Direction I feel is a bad name for the relative vector from origin to point, change in the future
			public Vector getDirection() {
				return new Vector(getDirectionX(), getDirectionY(), getDirectionZ());
			}

			public double getDistanceFromBeamSquared() {
				return distanceFromBeamSq;
			}

			public double calculateDistanceFromBeam() {
				return Math.sqrt(getDistanceFromBeamSquared());
			}

			public double dotProduct() {
				return dotProduct;
			}

			public Vector calculateVectorAlongBeam() {
				return getTrajectory().multiply(dotProduct() / lengthSquared());
			}

			public double calculateDistanceAlongBeam() {
				return length() * (dotProduct() / lengthSquared());
			}

			public Vector calculateClosestPointOnBeam() {
				switch (getPointLocation()) {
					case BEAM_SHAFT:
						return getOrigin().add(calculateVectorAlongBeam());
						/*Vector result = new Vector(dx, dy, dz).normalize().multiply(calculateDistanceAlongBeam());
						result.setX(result.getX() + ox);
						result.setY(result.getY() + oy);
						result.setZ(result.getZ() + oz);
						return result;*/
					case ORIGIN_CAP:
						return getOrigin();
					case END_CAP:
						return getOrigin().add(getTrajectory());
					default:
						throw new RuntimeException("This shouldn't happen");
				}
			}

			public Vector calculateVectorFromBeam() {
				return getPoint().subtract(calculateClosestPointOnBeam());
			}

			public PointLocation getPointLocation() { return pointLocation; }
		}

		//TODO PointLocation is a terrible name but I cant think of anything better atm
		public enum PointLocation {
			BEAM_SHAFT,
			ORIGIN_CAP,
			END_CAP
		}
	}
}
