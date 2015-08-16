package com.herocraftonline.heroes.characters.skill.skills.utils;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.herocraftonline.heroes.characters.Hero;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

/*
	Math involved with this class comes from http://www.flipcode.com/archives/Fast_Point-In-Cylinder_Test.shtml

	See that site for more documentation about the following math.
 */
public final class Beam {

	private static final Function<Entity, Vector> DEFAULT_POINT_FUNCTION = new Function<Entity, Vector>() {
		@Override
		public Vector apply(Entity entity) {
			return entity.getLocation().toVector();
		}
	};

	public static final Function<LivingEntity, Vector> DEFAULT_LIVING_ENTITY_POINT_FUNCTION = new Function<LivingEntity, Vector>() {
		@Override
		public Vector apply(LivingEntity livingEntity) {
			return livingEntity.getEyeLocation().toVector();
		}
	};

	public static <E extends Entity> void cast(Hero caster, Beam beam,
	                                           TargetFunction<? super E> targetFunction, Class<E> targetClass,
	                                           Predicate<? super E> targetFilter,
	                                           Function<? super E, Vector> pointFunction) {
		Player player = caster.getPlayer();
		List<Entity> targets = player.getNearbyEntities(beam.getRangeBounds(), beam.getRangeBounds(), beam.getRangeBounds());
		for (Entity possibleTarget : targets) {
			if (possibleTarget.equals(player)) {
				continue;
			}
			if (targetClass.isInstance(possibleTarget)) {
				E target = targetClass.cast(possibleTarget);
				if (targetFilter.apply(target)) {
					Optional<PointData> pointData = beam.calculatePointData(pointFunction.apply(target));
					if (pointData.isPresent()) {
						targetFunction.handle(caster, target, pointData.get());
					}
				}
			}
		}
	}

	public static <E extends Entity> void cast(Hero caster, Beam beam,
	                                           TargetFunction<? super E> targetFunction, Class<E> targetClass,
	                                           Predicate<? super E> targetFilter) {
		cast(caster, beam, targetFunction, targetClass, targetFilter, DEFAULT_POINT_FUNCTION);
	}

	public static <E extends Entity> void cast(Hero caster, Beam beam,
	                                           TargetFunction<E> targetFunction, Class<E> targetClass,
	                                           Function<E, Vector> pointFunction) {
		cast(caster, beam, targetFunction, targetClass, Predicates.<E>alwaysTrue(), pointFunction);
	}

	public static <E extends Entity> void cast(Hero caster, Beam beam,
	                                           TargetFunction<E> targetFunction, Class<E> targetClass) {
		cast(caster, beam, targetFunction, targetClass, Predicates.<E>alwaysTrue(), DEFAULT_POINT_FUNCTION);
	}

	public static void castOnLivingEntities(Hero caster, Beam beam,
	                        TargetFunction<LivingEntity> targetFunction,
	                        Predicate<LivingEntity> targetFilter,
	                        Function<LivingEntity, Vector> pointFunction) {
		cast(caster, beam, targetFunction, LivingEntity.class, targetFilter, pointFunction);
	}

	public static void castOnLivingEntities(Hero caster, Beam beam,
	                        TargetFunction<LivingEntity> targetFunction,
	                        Predicate<LivingEntity> targetFilter) {
		cast(caster, beam, targetFunction, LivingEntity.class, targetFilter, DEFAULT_LIVING_ENTITY_POINT_FUNCTION);
	}

	public static void castOnLivingEntities(Hero caster, Beam beam,
	                        TargetFunction<LivingEntity> targetFunction,
	                        Function<LivingEntity, Vector> pointFunction) {
		cast(caster, beam, targetFunction, LivingEntity.class, Predicates.<LivingEntity>alwaysTrue(), pointFunction);
	}

	public static void castOnLivingEntities(Hero caster, Beam beam,
	                        TargetFunction<LivingEntity> targetFunction) {
		cast(caster, beam, targetFunction, LivingEntity.class, Predicates.<LivingEntity>alwaysTrue(), DEFAULT_LIVING_ENTITY_POINT_FUNCTION);
	}

	public static void castOnPlayers(Hero caster, Beam beam,
	                        TargetFunction<Player> targetFunction,
	                        Predicate<Player> targetFilter,
	                        Function<Player, Vector> pointFunction) {
		cast(caster, beam, targetFunction, Player.class, targetFilter, pointFunction);
	}

	public static void castOnPlayers(Hero caster, Beam beam,
	                        TargetFunction<Player> targetFunction,
	                        Predicate<Player> targetFilter) {
		cast(caster, beam, targetFunction, Player.class, targetFilter, DEFAULT_LIVING_ENTITY_POINT_FUNCTION);
	}

	public static void castOnPlayers(Hero caster, Beam beam,
	                        TargetFunction<Player> targetFunction,
	                        Function<Player, Vector> pointFunction) {
		cast(caster, beam, targetFunction, Player.class, Predicates.<LivingEntity>alwaysTrue(), pointFunction);
	}

	public static void castOnPlayers(Hero caster, Beam beam,
	                        TargetFunction<Player> targetFunction) {
		cast(caster, beam, targetFunction, Player.class, Predicates.<LivingEntity>alwaysTrue(), DEFAULT_LIVING_ENTITY_POINT_FUNCTION);
	}

	private final double ox, oy, oz;    // Beam origin vector
	private final double dx, dy, dz;    // Beam direction vector
	private final double lengthSq;      // Pre-calculated length squared of the beam
	private final double radiusSq;      // Pre-calculated radius squared of the beam
	private final double rangeBounds;   // Pre-calculated range bounds (for use with `getNearbyEntities()`)

	private Beam(double ox, double oy, double oz, double dx, double dy, double dz, double lengthSq, double radiusSq) {
		checkArgument(lengthSq > 0, "Beam length must be greater than 0");
		checkArgument(radiusSq > 0, "Beam radius must be greater than 0");

		this.ox = ox;
		this.oy = oy;
		this.oz = oz;
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
		this.lengthSq = lengthSq;
		this.radiusSq = radiusSq;
		this.rangeBounds = calculateLength() + calculateRadius();
	}

	public Beam(Vector origin, Vector beam, double radius) {
		this(origin.getX(), origin.getY(), origin.getZ(), beam.getX(), beam.getY(), beam.getZ(), beam.lengthSquared(), radius * radius);
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

	public double getRangeBounds() {
		return rangeBounds;
	}

	public Optional<PointData> calculatePointData(Vector point) {
		double pdx, pdy, pdz;
		double dot;

		pdx = point.getX() - ox;
		pdy = point.getY() - oy;
		pdz = point.getZ() - oz;

		dot = pdx * dx + pdy * dy + pdz * dz;

		if (dot < 0 || dot > lengthSq) {
			return Optional.absent();
		}
		else {
			// Accessing this value can make variable effects based on distance from beam line.
			double dsq = (pdx * pdx + pdy * pdy + pdz * pdz) - dot * dot / lengthSq;

			if (dsq > radiusSq) {
				return Optional.absent();
			}
			else {
				return Optional.of(new PointData(point, pdx, pdy, pdz, dsq, dot));
			}
		}
	}

	public final class PointData {

		private final double px, py, pz;            // The point tested
		private final double pdx, pdy, pdz;         // The vector from the beam origin to the point
		private final double distanceFromBeamSq;    // Distance squared from the line that represents the beam
		private final double dotProduct;            // The dot product between the beam and the point

		private PointData(Vector point, double pdx, double pdy, double pdz, double distanceFromBeamSq, double dotProduct) {
			px = point.getX();
			py = point.getY();
			pz = point.getZ();
			this.pdx = pdx;
			this.pdy = pdy;
			this.pdz = pdz;
			this.distanceFromBeamSq = distanceFromBeamSq;
			this.dotProduct = dotProduct;
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

		public double calculateDistanceAlongBeam() {
			return calculateLength() * (getDotProduct() / getLengthSquared());
		}

		public Vector calculateClosestPointOnBeam() {
			Vector result = new Vector(dx, dy, dz).normalize().multiply(calculateDistanceAlongBeam());
			result.setX(result.getX() + ox);
			result.setY(result.getY() + oy);
			result.setZ(result.getZ() + oz);
			return result;
		}

		public Vector calculateVectorFromBeam() {
			return getPoint().subtract(calculateClosestPointOnBeam());
		}
	}

	public interface TargetFunction<E extends Entity> {
		void handle(Hero hero, E target, PointData pointData);
	}
}
