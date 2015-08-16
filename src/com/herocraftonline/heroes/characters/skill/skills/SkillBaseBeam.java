package com.herocraftonline.heroes.characters.skill.skills;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Optional;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Set;

public abstract class SkillBaseBeam extends ActiveSkill {

	public SkillBaseBeam(Heroes plugin, String name) {
		super(plugin, name);
	}

	protected final void castBeam(Hero hero, Beam beam) {
		List<Entity> possibleTargets = hero.getPlayer().getNearbyEntities(beam.targetRange, beam.targetRange, beam.targetRange);
		for (Entity possibleTarget : possibleTargets) {
			if (possibleTarget instanceof LivingEntity) {
				Optional<Beam.PointData> pointData = beam.calculatePointData(possibleTarget.getLocation().toVector());
				if (pointData.isPresent()) {
					onTargetHit(hero, (LivingEntity) possibleTarget, pointData.get());
				}
			}
		}
	}

	protected abstract void onTargetHit(Hero hero, LivingEntity target, Beam.PointData pointData);

	/*
		Math involved with this class comes from http://www.flipcode.com/archives/Fast_Point-In-Cylinder_Test.shtml

		See that site for more documentation about the following math.
	 */
	protected final class Beam {

		private final Vector origin;
		private final double dx, dy, dz;
		private final double lengthSq;
		private final double radiusSq;
		private final double targetRange;

		private Beam(Vector origin, double dx, double dy, double dz, double lengthSq, double radiusSq) {
			checkArgument(lengthSq > 0, "Beam length must be greater than 0");
			checkArgument(radiusSq > 0, "Beam radius must be greater than 0");

			this.origin = origin;
			this.dx = dx;
			this.dy = dy;
			this.dz = dz;
			this.lengthSq = lengthSq;
			this.radiusSq = radiusSq;
			this.targetRange = Math.sqrt(lengthSq) + Math.sqrt(radiusSq);
		}

		public Beam(Vector origin, Vector beam, double radius) {
			this(origin, beam.getX(), beam.getY(), beam.getZ(), beam.lengthSquared(), radius * radius);
		}

		public Beam(Location origin, Vector beam, double radius) {
			this(origin.toVector(), beam, radius);
		}

		public Beam(Vector origin, Vector direction, double length, double radius) {
			this(origin, direction.normalize().multiply(length), radius);
		}

		public Beam(Location origin, Vector direction, double length, double radius) {
			this(origin.toVector(), direction, length, radius);
		}

		public Beam(Location origin, double length, double radius) {
			this(origin, origin.getDirection(), length, radius);
		}

		public Beam(LivingEntity origin, double length, double radius) {
			this(origin.getEyeLocation(), length, radius);
		}

		public Beam(LivingEntity origin, Set<Material> transparentBlocks, int maxLength, double radius) {
			this(origin.getEyeLocation(), origin.getTargetBlock(transparentBlocks, maxLength).getLocation().subtract(origin.getEyeLocation()).toVector(), radius);
		}

		public Optional<PointData> calculatePointData(Vector point) {
			double pdx, pdy, pdz;
			double dot;

			pdx = point.getX() - origin.getX();
			pdy = point.getY() - origin.getY();
			pdz = point.getZ() - origin.getZ();

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
					return Optional.of(new PointData(point, dsq, dot));
				}
			}
		}

		public final class PointData {

			private final double px, py, pz;
			private final double distanceFromAxisSq;
			private final double distanceFromOrigin;

			private PointData(Vector point, double distanceFromAxisSq, double distanceFromOrigin) {
				px = point.getX();
				py = point.getY();
				pz = point.getZ();
				this.distanceFromAxisSq = distanceFromAxisSq;
				this.distanceFromOrigin = distanceFromOrigin;
			}

			public double getDistanceFromBeamSquared() {
				return distanceFromAxisSq;
			}

			public double getDistanceFromOrigin() {
				return distanceFromOrigin;
			}

			public Vector getOrigin() {
				return origin.clone();
			}

			public Vector getBeam() {
				return new Vector(dx, dy, dz);
			}

			public Vector getPoint() {
				return new Vector(px, py, pz);
			}

			public Vector calculateClosestPointOnBeam() {
				return new Vector(dx, dy, dz).normalize().multiply(distanceFromOrigin).add(origin);
			}
		}
	}
}
