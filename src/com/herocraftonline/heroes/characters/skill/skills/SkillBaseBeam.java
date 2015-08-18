package com.herocraftonline.heroes.characters.skill.skills;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Set;

public abstract class SkillBaseBeam extends ActiveSkill {

	public SkillBaseBeam(Heroes plugin, String name) {
		super(plugin, name);
	}

	protected final void castBeam(final Hero hero, final Beam beam) {
		final Location midPoint = beam.calculateMidpoint().toLocation(hero.getPlayer().getWorld());
		final List<Entity> possibleTargets = hero.getPlayer().getWorld().getEntities();

		Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				for (Entity possibleTarget : possibleTargets) {
					final LivingEntity target;
					if (possibleTarget instanceof LivingEntity
							&& !possibleTarget.equals(hero.getPlayer()) && isValidTarget(target = (LivingEntity) possibleTarget)

							// TODO I would like to determine if this check helps in any way.
							&& target.getLocation().distanceSquared(midPoint) <= beam.midpointRadiusSq) {

						final Optional<Beam.PointData> pointData = beam.calculatePointData(target.getEyeLocation().toVector());
						if (pointData.isPresent()) {
							Bukkit.getServer().getScheduler().runTask(plugin, new Runnable() {
								@Override
								public void run() {
									onTargetHit(hero, target, pointData.get());
								}
							});
						}
					}
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

	protected boolean isValidTarget(LivingEntity target) { return true; }

	protected abstract void onTargetHit(Hero hero, LivingEntity target, Beam.PointData pointData);

	/*
		Math involved with this class comes from http://www.flipcode.com/archives/Fast_Point-In-Cylinder_Test.shtml

		See that site for more documentation about the following math.
	 */
	protected static final class Beam {

		private final double ox, oy, oz;        // Beam origin vector
		private final double dx, dy, dz;        // Beam direction vector
		private final double lengthSq;          // Pre-calculated length squared of the beam
		private final double radiusSq;          // Pre-calculated radius squared of the beam
		private final double midpointRadiusSq;  // Pre-calculated range bounds (for use with `getNearbyEntities()`)

		private Beam(double ox, double oy, double oz, double dx, double dy, double dz, double length, double radius) {
			checkArgument(length > 0, "Beam length must be greater than 0");
			checkArgument(radius > 0, "Beam radius must be greater than 0");

			this.ox = ox;
			this.oy = oy;
			this.oz = oz;
			this.dx = dx;
			this.dy = dy;
			this.dz = dz;
			this.lengthSq = length * length;
			this.radiusSq = radius * radius;

			double midpointRadius = length / 2 + radius;
			midpointRadiusSq = midpointRadius * midpointRadius;
		}

		public Beam(Vector origin, Vector beam, double radius) {
			this(origin.getX(), origin.getY(), origin.getZ(), beam.getX(), beam.getY(), beam.getZ(), beam.length(), radius);
		}

		public Beam(Location origin, Vector beam, double radius) {
			this(origin.toVector(), beam, radius);
		}

		public Beam(Vector origin, Vector direction, double length, double radius) {
			this(origin, direction.clone().normalize().multiply(length), radius);
		}

		public Beam(Location origin, Vector direction, double length, double radius) {
			this(origin.toVector(), direction, length, radius);
		}

		public Beam(Location origin, double length, double radius) {
			this(origin, origin.getDirection().multiply(length), radius);
		}

		public Beam(LivingEntity origin, double length, double radius) {
			this(origin.getEyeLocation(), length, radius);
		}

		public Beam(LivingEntity origin, Set<Material> transparentBlocks, int maxLength, double radius) {
			this(origin.getEyeLocation(), origin.getTargetBlock(transparentBlocks, maxLength).getLocation().add(0.5, 0.5, 0.5).distance(origin.getEyeLocation()), radius);
		}

		public Beam withAlteredOrigin(Vector origin) {
			return new Beam(origin.getX(), origin.getY(), origin.getZ(), dx, dy, dz, lengthSq, radiusSq);
		}

		public Beam withAlteredOrigin(Location origin) {
			return withAlteredOrigin(origin.toVector());
		}

		public Beam withTranslatedOrigin(Vector v) {
			return withAlteredOrigin(getOrigin().add(v));
		}

		public Beam withAlteredTrajectory(Vector beam) {
			return new Beam(ox, oy, oz, beam.getX(), beam.getY(), beam.getZ(), beam.lengthSquared(), radiusSq);
		}

		public Beam withAlteredLength(double length) {
			return withAlteredTrajectory(getDirection().normalize().multiply(length));
		}

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

		public double getDirectionX() {
			return dx;
		}

		public double getDirectionY() {
			return dy;
		}

		public double getDirectionZ() {
			return dz;
		}

		public Vector getDirection() {
			return new Vector(getDirectionX(), getDirectionY(), getDirectionZ());
		}

		public Vector calculateMidpoint() {
			return getOrigin().add(getDirection().multiply(0.5));
		}

		public double getLengthSquared() {
			return lengthSq;
		}

		public double calculateLength() {
			return Math.sqrt(getLengthSquared());
		}

		public double getRadiusSquared() {
			return radiusSq;
		}

		public double calculateRadius() {
			return Math.sqrt(getRadiusSquared());
		}

		public Optional<PointData> calculatePointData(Vector point, boolean roundedCap) {
			double pdx, pdy, pdz;       // Vector from origin to point (point distance)
			double dot;                 // Reference to dot product of vector[pd] (point distance) and vector[o] (origin)

			pdx = point.getX() - ox;
			pdy = point.getY() - oy;
			pdz = point.getZ() - oz;

			dot = pdx * dx + pdy * dy + pdz * dz;

			// If the dot product is not within the range of the beam shaft...
			if (dot < 0 || dot > lengthSq) {
				// ... check if we test for rounded caps...
				if (roundedCap) {
					// ... if so declare a reference point...
					Vector refPoint = getOrigin();
					// ... and a reference distance squared.
					double redDistanceSq;

					// If the distance squared from the refPoint to point is <= radius squared...
					if ((redDistanceSq = refPoint.distanceSquared(point)) <= radiusSq) {
						// ... eturn a point data of location origin cap as the current reference is origin
						return Optional.of(new PointData(point, pdx, pdy, pdz, redDistanceSq, dot, PointLocation.ORIGIN_CAP));
					}
					// If the distance squared from the refPoint to point is <= radius squared...
					else if ((redDistanceSq = refPoint.add(getDirection()).distanceSquared(point)) <= radiusSq) {
						// ... return a point data of location end cap as the current ref point is the opposit origin.
						return Optional.of(new PointData(point, pdx, pdy, pdz, redDistanceSq, dot, PointLocation.END_CAP));
					}
				}

				// Return absent if no testing for rounded caps, or no passing rounding cap tests.
				return Optional.absent();
			}
			else {
				// ... else test for beam shaft radius (that sounds so... dirty...)
				// This is a fancy way to check if the point is within the radius of the cylinder without trigonometric functions
				double dsq = (pdx * pdx + pdy * pdy + pdz * pdz) - dot * dot / lengthSq;

				// if distance squared from beam is <= radius squared...
				if (dsq <= radiusSq) {
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

			public double getDirectionX() {
				return pdx;
			}

			public double getDirectionY() {
				return pdy;
			}

			public double getDirectionZ() {
				return pdz;
			}

			public Vector getDirection() {
				return new Vector(getDirectionX(), getDirectionY(), getDirectionZ());
			}

			public double getDistanceFromBeamSquared() {
				return distanceFromBeamSq;
			}

			public double calculateDistanceFromBeam() {
				return Math.sqrt(getDistanceFromBeamSquared());
			}

			public double getDotProduct() {
				return dotProduct;
			}

			public Vector calculateVectorAlongBeam() {
				return getDirection().multiply(getDotProduct() / getLengthSquared());
			}

			public double calculateDistanceAlongBeam() {
				return calculateLength() * (getDotProduct() / getLengthSquared());
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
						return getOrigin().add(getDirection());
					default:
						throw new RuntimeException("Java should never let this happen, all enum cases accounted for");
				}
			}

			public Vector calculateVectorFromBeam() {
				return getPoint().subtract(calculateClosestPointOnBeam());
			}

			public PointLocation getPointLocation() { return pointLocation; }
		}

		public enum PointLocation {
			BEAM_SHAFT,
			ORIGIN_CAP,
			END_CAP
		}
	}
}
