package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class SkillBaseGroundEffect extends ActiveSkill {

	protected static final String HEIGHT_NODE = "height";

	public SkillBaseGroundEffect(Heroes plugin, String name) {
		super(plugin, name);
	}

	private void castGroundEffect(final Hero hero, final Location location, final double radius, final double height, final GroundEffectActions actions) {
		final Set<Entity> possibleTargets = getEntitiesInChunks(hero.getPlayer().getLocation(), (int) (radius + 16) / 16);

		// TODO Not much logic needed with sphere casting, look into if async filtering is needed.
		Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				for (final Entity target : possibleTargets) {
					if (target instanceof LivingEntity) {
						Location targetLocation = target.getLocation();
						double targetY = targetLocation.getY();
						targetLocation.setY(location.getY());
						if (location.distanceSquared(targetLocation) <= radius * radius && targetY <= location.getY() + height && targetY >= location.getY() - height) {
							Bukkit.getScheduler().runTask(plugin, new Runnable() {
								@Override
								public void run() {
									actions.groundEffectTargetAction(hero, (LivingEntity) target);
								}
							});
						}
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

	public interface GroundEffectActions {
		void groundEffectTickAction(Hero hero, AreaGroundEffectEffect effect);
		void groundEffectTargetAction(Hero hero, LivingEntity target);
	}

	protected void applyAreaGroundEffectEffect(Hero hero, long period, long duration, Location location, double radius, double height,
	                                           GroundEffectActions sphereActions, String applyText, String expireText, EffectType... effectTypes) {
		AreaGroundEffectEffect effect = new AreaGroundEffectEffect(hero.getPlayer(), period, duration, location, radius, height, sphereActions, applyText, expireText);
		Collections.addAll(effect.types, effectTypes);
		hero.addEffect(effect);
	}

	protected void applyAreaGroundEffectEffect(Hero hero, long period, long duration, Location location, double radius, double height,
	                                     GroundEffectActions sphereActions, EffectType... effectTypes) {
		applyAreaGroundEffectEffect(hero, period, duration, location, radius, height, sphereActions, null, null, effectTypes);
	}

	protected boolean isAreaGroundEffectApplied(Hero hero) {
		return hero.hasEffect(getName());
	}

	protected final class AreaGroundEffectEffect extends PeriodicExpirableEffect {

		private Location location;
		private double radius;
		private double height;
		private GroundEffectActions actions;

		public AreaGroundEffectEffect(Player applier, long period, long duration, Location location, double radius, double height, GroundEffectActions actions, String applyText, String expireText) {
			super(SkillBaseGroundEffect.this, SkillBaseGroundEffect.this.getName(), applier, period, duration, applyText, expireText);
			this.location = location;
			this.radius = radius;
			this.height = height;
			this.actions = actions;

			types.add(EffectType.AREA_OF_EFFECT);
			types.add(EffectType.BENEFICIAL);
			types.add(EffectType.MAGIC);
		}

		public Location getLocation() {
			return location;
		}

		public void setLocation(Location location) {
			this.location = location;
		}

		public double getRadius() {
			return radius;
		}

		public void setRadius(double radius) {
			this.radius = radius;
		}

		public double getHeight() {
			return height;
		}

		public void setHeight(double height) {
			this.height = height;
		}

		@Override
		public void tickHero(Hero hero) {
			actions.groundEffectTickAction(hero, this);
			castGroundEffect(hero, location, radius, height, actions);
		}

		@Override
		public void tickMonster(Monster monster) {
			throw new UnsupportedOperationException("Area Sphere tick on monster");
		}
	}

	/*
	The following is a reference to when I tried to implement this as a block spread extending `SkillBaseMassBlockEffector`
	 */
	/*protected final void process(final Hero hero, final double radius, final int effectHeight, final long duration, final long period,
	                             final BlockProcessor blockProcessor, final TargetHandler targetHandler, final Predicate<BlockState> blockFilter) {
		final Location origin = hero.getPlayer().getLocation();
		final BlockRegion region = BlockRegion.bounds(origin.getBlock(), NumberConversions.ceil(radius));

		Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {

			private Set<BlockState> tracked = new HashSet<>();

			@Override
			public void run() {

				//TODO Expand from center in a sort of mold of the landscape

				BlockState center = region.getCenter();
				spread(center);
			}

			private void spread(BlockState block) {
				if (validSpread(block)) {
					track(block);

					if (validTarget(block)) {

					}

					spread(region.getRelative(block, BlockFace.DOWN));
					spread(region.getRelative(block, BlockFace.NORTH));
					spread(region.getRelative(block, BlockFace.SOUTH));
					spread(region.getRelative(block, BlockFace.EAST));
					spread(region.getRelative(block, BlockFace.WEST));
					spread(region.getRelative(block, BlockFace.UP));
				}
			}

			private boolean validSpread(BlockState block) {
				return block != null && isTracked(block) && Util.transparentBlocks.contains(block.getType())
						&& blockFilter.apply(block) && origin.distanceSquared(block.getLocation().add(0.5, 0.5, 0.5)) <= NumberConversions.square(radius);
			}

			private boolean isTracked(BlockState block) {
				return !tracked.contains(block);
			}

			private void track(BlockState block) {
				tracked.add(block);
			}

			private boolean validTarget(BlockState block) {
				return !Util.transparentBlocks.contains(region.getRelative(block, BlockFace.DOWN));
			}
		});
	}

	public interface TargetHandler {
		void handle(Hero hero, LivingEntity target);
	}*/
}
