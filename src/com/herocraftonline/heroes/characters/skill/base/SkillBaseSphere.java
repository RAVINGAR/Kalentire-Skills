package com.herocraftonline.heroes.characters.skill.base;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.EffectAddEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.SphereEffect;
import de.slikey.effectlib.util.ParticleEffect;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class SkillBaseSphere extends ActiveSkill {

	public SkillBaseSphere(Heroes plugin, String name) {
		super(plugin, name);
	}

	protected void castSphere(final Hero hero, final double radius, final SphereActions sphereActions) {
		final Set<Entity> possibleTargets = getEntitiesInChunks(hero.getPlayer().getLocation(), (int) (radius + 16) / 16);

		// TODO Not much logic needed with sphere casting, look into if async filtering is needed.
		Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				for (final Entity target : possibleTargets) {
					if (hero.getPlayer().getLocation().distanceSquared(target.getLocation()) <= radius * radius) {
						Bukkit.getScheduler().runTask(plugin, new Runnable() {
							@Override
							public void run() {
								sphereActions.sphereTargetAction(hero, target);
							}
						});
					}
				}
			}
		});
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

	public interface SphereActions {
		void sphereTickAction(Hero hero, AreaSphereEffect effect);
		void sphereTargetAction(Hero hero, Entity target);
	}

	protected void renderSphere(Location center, double radius, int particlesPerRadius, ParticleEffect particle, Color color) {
		EffectManager em = new EffectManager(plugin);
		SphereEffect effect = new SphereEffect(em);

		effect.setLocation(center);
		effect.radius = radius;
		effect.particle = particle;
		effect.color = color;
		effect.particles = (int) radius * particlesPerRadius;
		effect.type = de.slikey.effectlib.EffectType.INSTANT;
		effect.iterations = 1;
		effect.asynchronous = true;

		effect.start();
		em.disposeOnTermination();
	}

	protected void renderSphere(Location center, double radius, int particlesPerRadius, ParticleEffect particle) {
		renderSphere(center, radius, particlesPerRadius, particle, Color.WHITE);
	}

	protected void renderSphere(Location center, double radius, ParticleEffect particle, Color color) {
		renderSphere(center, radius, 100, particle, color);
	}

	protected void renderSphere(Location center, double radius, ParticleEffect particle) {
		renderSphere(center, radius, 100, particle);
	}

	protected void applyAreaSphereEffect(Hero hero, long period, long duration, double radius,
	                                     SphereActions sphereActions, String applyText, String expireText, EffectType... effectTypes) {
		AreaSphereEffect effect = new AreaSphereEffect(hero.getPlayer(), period, duration, radius, sphereActions, applyText, expireText);
		Collections.addAll(effect.types, effectTypes);
		hero.addEffect(effect);
	}

	protected void applyAreaSphereEffect(Hero hero, long period, long duration, double radius,
	                                     SphereActions sphereActions, EffectType... effectTypes) {
		applyAreaSphereEffect(hero, period, duration, radius, sphereActions, null, null, effectTypes);
	}

	protected boolean isAreaSphereApplied(Hero hero) {
		return hero.hasEffect(getName());
	}

	protected final class AreaSphereEffect extends PeriodicExpirableEffect {

		protected double radius;
		protected final SphereActions sphereActions;

		//private int ticks;

		private AreaSphereEffect(Player applier, long period, long duration, double radius, SphereActions sphereActions, String applyText, String expireText) {
			super(SkillBaseSphere.this, SkillBaseSphere.this.getName(), applier, period
					// TODO This is an experiment to attempt to make the effect tick amount more consistent
					, duration/* - (period / 2)*/
					, applyText, expireText);
			this.radius = radius;
			this.sphereActions = sphereActions;

			types.add(EffectType.AREA_OF_EFFECT);
			types.add(EffectType.BENEFICIAL);
			types.add(EffectType.MAGIC);
		}

		private AreaSphereEffect(Player applier, long period, long duration, double radius, SphereActions sphereActions) {
			this(applier, period, duration, radius, sphereActions, null, null);
		}

		public double getRadius() {
			return radius;
		}

		public void setRadius(double radius) {
			this.radius = radius;
		}

		@Override
		public void tickHero(Hero hero) {
			sphereActions.sphereTickAction(hero, this);
			castSphere(hero, radius, sphereActions);

			// TODO Tests the issue with inconsistent tick amounts over a fixed time (hint: its because effects use milliseconds)
			//hero.getPlayer().sendMessage(ChatColor.GREEN + "Expire Time: " + ChatColor.WHITE + (getExpiry() - getApplyTime()) + ChatColor.GREEN + " Current Tick: " + (getLastTickTime() - getApplyTime()));
			//ticks++;
		}

		@Override
		public void tickMonster(Monster monster) {
			throw new UnsupportedOperationException("Area Sphere tick on monster");
		}

		/*@Override
		public void removeFromHero(Hero hero) {
			super.removeFromHero(hero);
			hero.getPlayer().sendMessage(ChatColor.GREEN + "Ticks: " + ChatColor.WHITE + ticks);
		}*/
	}

	private final class SphereControlListeners implements Listener {

		@EventHandler(priority = EventPriority.MONITOR)
		private void OnEffectAdd(EffectAddEvent e) {

			if (e.getCharacter().hasEffect(SkillBaseSphere.this.getName()) && e.getEffect().isType(EffectType.SILENCE)) {
				e.getCharacter().removeEffect(e.getCharacter().getEffect(SkillBaseSphere.this.getName()));
			}
		}
	}
}
