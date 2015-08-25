package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.SphereEffect;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.HashSet;
import java.util.Set;

public abstract class SkillBaseSphere extends ActiveSkill {

	public SkillBaseSphere(Heroes plugin, String name) {
		super(plugin, name);
	}

	protected void castSphere(final Hero hero, final double radius, final TargetHandler targetHandler) {
		final Set<Entity> possibleTargets = getEntitiesInChunks(hero.getPlayer().getLocation(), (int) (radius + 16) / 16);

		// TODO Not much logic needed with sphere casting, look into if async filtering is needed.
		Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				for (final Entity target : possibleTargets) {
					if (!target.equals(hero.getPlayer())
							// TODO Use line of sight?
							//&& hero.getPlayer().hasLineOfSight(target)
							&& hero.getPlayer().getLocation().distanceSquared(target.getLocation()) <= radius * radius) {
						Bukkit.getScheduler().runTask(plugin, new Runnable() {
							@Override
							public void run() {
								targetHandler.handle(hero, target);
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

	public interface TargetHandler {
		void handle(Hero hero, Entity target);
	}

	protected void renderSphere(Location center, double radius, ParticleEffect particle) {

	}

	protected void renderSphere(Location center, double radius, ParticleEffect particle, Color color) {

	}
}
