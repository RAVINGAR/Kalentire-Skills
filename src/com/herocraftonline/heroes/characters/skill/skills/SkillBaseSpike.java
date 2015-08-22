package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.EffectType;
import de.slikey.effectlib.effect.ConeEffect;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

import static com.google.common.base.Preconditions.checkArgument;

public abstract class SkillBaseSpike extends TargettedSkill {

	private static final long TICK_RISE_TIME = 5;

	public SkillBaseSpike(Heroes plugin, String name) {
		super(plugin, name);
	}

	private static ConeEffect createCone(EffectManager em, ParticleEffect particle, int particlesCone, double height, double radius) {
		checkArgument(particlesCone > 0, "particlesCone must be greater than 0");

		ConeEffect effect = new ConeEffect(em);

		effect.particle = particle;
		effect.angularVelocity = Math.PI / 10;
		effect.particles = particlesCone;
		effect.particlesCone = particlesCone;
		effect.lengthGrow =  (float) height / particlesCone;
		effect.radiusGrow = (float) radius / particlesCone;
		effect.asynchronous = true;
		effect.type = EffectType.INSTANT;
		effect.iterations = 1;

		return effect;
	}

	private static ConeEffect createCone(EffectManager em, ParticleEffect particle, int particlesCone, double height, double radius, Color color) {
		ConeEffect effect = createCone(em, particle, particlesCone, height, radius);
		effect.color = color;
		return effect;
	}

	protected void renderSpike(Location location, double height, double radius, ParticleEffect particle, Color color) {
		EffectManager em = new EffectManager(plugin);
		renderSpike(location.clone(), em, createCone(em, particle, 120, height, radius, color), height);
	}

	protected void renderSpike(Location location, double height, double radius, ParticleEffect particle) {
		EffectManager em = new EffectManager(plugin);
		renderSpike(location.clone(), em, createCone(em, particle, 120, height, radius), height);
	}

	private void renderSpike(final Location location, final EffectManager em, final ConeEffect effect, final double height) {
		new BukkitRunnable() {

			private int life = 0;
			private double increment = height / TICK_RISE_TIME;

			@Override
			public void run() {
				location.setY(location.getY() + increment);
				effect.setLocation(location);
				effect.start();

				if (++life >= TICK_RISE_TIME) {
					cancel();
					em.disposeOnTermination();
				}
			}

		}.runTaskTimer(plugin, 0, 1);
	}
}
