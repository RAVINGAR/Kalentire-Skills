package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.EffectType;
import de.slikey.effectlib.effect.ConeEffect;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import static com.google.common.base.Preconditions.checkArgument;

public abstract class SkillBaseSpike extends TargettedSkill {

	private static final Vector DOWN = new Vector(0, -1, 0);
	private static final int PARTICLES_PER_HEIGHT = 10;
	private static final double CONE_ANGULAR_VELOCITY = Math.PI / 5;
	private static final long TICK_RISE_TIME = 5;

	public SkillBaseSpike(Heroes plugin, String name) {
		super(plugin, name);
	}

	private static ConeEffect createCone(EffectManager em, ParticleEffect particle, double height, double radius) {
		ConeEffect effect = new ConeEffect(em);

		effect.particle = particle;
		effect.angularVelocity = CONE_ANGULAR_VELOCITY;
		effect.particles = (int) (height * PARTICLES_PER_HEIGHT);
		effect.particlesCone = effect.particles;
		effect.lengthGrow =  (float) height / effect.particles;
		effect.radiusGrow = (float) radius / effect.particles;
		effect.asynchronous = true;
		effect.type = EffectType.INSTANT;
		effect.iterations = 1;

		return effect;
	}

	private static ConeEffect createCone(EffectManager em, ParticleEffect particle, double height, double radius, Color color) {
		ConeEffect effect = createCone(em, particle, height, radius);
		effect.color = color;
		return effect;
	}

	protected void renderSpike(final Location location, final double height, final double radius, final ParticleEffect particle, final Color color) {

		location.setDirection(DOWN);

		new BukkitRunnable() {

			private int life = 0;

			@Override
			public void run() {
				EffectManager em = new EffectManager(plugin);
				ConeEffect effect = createCone(em, particle, height, radius, color);
				effect.setLocation(location.clone().add(0, height / TICK_RISE_TIME * life, 0));
				effect.start();
				em.disposeOnTermination();

				if (++life > TICK_RISE_TIME) {
					cancel();
				}
			}
		}.runTaskTimer(plugin, 0, 1);
	}

	protected void renderSpike(final Location location, final double height, final double radius, final ParticleEffect particle) {

		location.setDirection(DOWN);

		new BukkitRunnable() {

			private int life = 0;

			@Override
			public void run() {
				EffectManager em = new EffectManager(plugin);
				ConeEffect effect = createCone(em, particle, height / TICK_RISE_TIME * life, radius);
				effect.setLocation(location.clone().add(0, height / TICK_RISE_TIME * life, 0));
				effect.start();
				em.disposeOnTermination();

				if (++life > TICK_RISE_TIME) {
					cancel();
				}
			}
		}.runTaskTimer(plugin, 0, 1);
	}
}
