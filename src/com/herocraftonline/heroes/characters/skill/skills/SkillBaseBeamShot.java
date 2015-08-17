package com.herocraftonline.heroes.characters.skill.skills;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Predicate;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public abstract class SkillBaseBeamShot extends SkillBaseBeam {

	public SkillBaseBeamShot(Heroes plugin, String name) {
		super(plugin, name);
	}

	protected void fireShot(Hero hero, BeamShot shot, Predicate<LivingEntity> targetFilter) {
		Set<UUID> processedTargets = new HashSet<>();
	}

	protected static final class BeamShot {

		private final Beam base;           // Base beam to shoot

		public BeamShot(Vector origin, Vector velocity, double radius) {
			base = new Beam(origin, velocity, radius);
		}

		public Vector getOrigin() {
			return base.getOrigin();
		}

		public Vector getVelocity() {
			return base.getDirection();
		}
	}

	private final class BeamShotRunnable extends BukkitRunnable {

		private final Hero hero;
		private final BeamShot shot;
		private final Predicate<LivingEntity> targetFilter;

		private final Set<UUID> processedTargets = new HashSet<>();
		private Beam currentBeam;

		private BeamShotRunnable(Hero hero, BeamShot shot, Predicate<LivingEntity> targetFilter) {
			this.hero = hero;
			this.shot = shot;
			this.targetFilter = targetFilter;

			currentBeam = shot.base;
			asyncTargetProcessing();
			runTaskTimer(plugin, 1, 1);
		}

		@Override
		public void run() {

		}

		private void asyncTargetProcessing() {
			final List<Entity> possibleTargets = hero.getPlayer().getWorld().getEntities();
			Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
				@Override
				public void run() {
					for (Entity possibleTarget : possibleTargets) {
						if (possibleTarget instanceof LivingEntity) {

						}
					}
				}
			});
		}
	}
}
