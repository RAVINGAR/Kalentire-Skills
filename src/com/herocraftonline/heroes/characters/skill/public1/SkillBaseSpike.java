package com.herocraftonline.heroes.characters.skill.public1;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.EffectType;
import de.slikey.effectlib.effect.ConeEffect;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public abstract class SkillBaseSpike extends TargettedSkill {

	private static final Vector DOWN = new Vector(0, -1, 0);
	private static final int PARTICLES_PER_HEIGHT = 10;
	private static final int PARTICLES_PER_RADIUS = 10;
	private static final double ANGULAR_VELOCITY_PI_DIVISOR_PER_RADIUS = 10;
	private static final long TICK_RISE_TIME = 5;

	protected static final double BLOCK_SPIKE_RADIUS = 0.5;

	protected static final String SPIKE_HEIGHT_NODE = "spike-height";
	protected static final String DOES_KNOCK_UP_NODE = "does-knock-up";
	protected static final String KNOCK_UP_STRENGTH_NODE = "knock-up-strength";

	public SkillBaseSpike(Heroes plugin, String name) {
		super(plugin, name);
	}

	private static ConeEffect createCone(EffectManager em, ParticleEffect particle, double height, double radius) {
		ConeEffect effect = new ConeEffect(em);

		effect.particle = particle;
		effect.angularVelocity = Math.PI / (radius * ANGULAR_VELOCITY_PI_DIVISOR_PER_RADIUS);
		effect.particles = 1 + (int) (height * PARTICLES_PER_HEIGHT) + (int) (radius * PARTICLES_PER_RADIUS);
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

	private abstract class ConeEffectProvider {
		public abstract ConeEffect provide(EffectManager em, double scaledHeight);
	}

	private void renderSpike(final Location location, final double height, final ConeEffectProvider provider) {

		location.setDirection(DOWN);

		new BukkitRunnable() {

			private int life = 0;

			@Override
			public void run() {
				EffectManager em = new EffectManager(plugin);

				double scaledHeight = (height / TICK_RISE_TIME) * life;
				ConeEffect effect = provider.provide(em, scaledHeight);
				effect.setLocation(location.clone().add(0, scaledHeight, 0));

				effect.start();
				em.disposeOnTermination();

				if (++life > TICK_RISE_TIME) {
					cancel();
				}
			}
		}.runTaskTimer(plugin, 0, 1);
	}

	protected void renderSpike(Location location, double height, final double radius, final ParticleEffect particle, final Color color) {
		renderSpike(location.clone(), height, new ConeEffectProvider() {
			@Override
			public ConeEffect provide(EffectManager em, double scaledHeight) {
				return createCone(em, particle, scaledHeight, radius, color);
			}
		});
	}

	protected void renderSpike(Location location, double height, final double radius, final ParticleEffect particle) {
		renderSpike(location.clone(), height, new ConeEffectProvider() {
			@Override
			public ConeEffect provide(EffectManager em, double scaledHeight) {
				return createCone(em, particle, scaledHeight, radius);
			}
		});
	}

	protected void renderSpikeOnBlock(Block block, double height, ParticleEffect particle, Color color) {
		renderSpike(block.getLocation().add(0.5, 1, 0.5), height, BLOCK_SPIKE_RADIUS, particle, color);
	}

	protected void renderSpikeOnBlock(Block block, double height, ParticleEffect particle) {
		renderSpike(block.getLocation().add(0.5, 1, 0.5), height, BLOCK_SPIKE_RADIUS, particle);
	}
}
