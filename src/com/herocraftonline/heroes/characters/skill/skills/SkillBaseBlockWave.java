package com.herocraftonline.heroes.characters.skill.skills;

import com.google.common.base.Predicate;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.prefs.PreferenceChangeEvent;

import static org.bukkit.Location.locToBlock;
import static org.bukkit.util.NumberConversions.square;

public abstract class SkillBaseBlockWave extends ActiveSkill {

	protected static final String HEIGHT_NODE = "height";
	protected static final String DEPTH_NODE = "depth";
	protected static final String EXPANSION_RATE = "expansion-rate";
	protected static final String LAUNCH_FORCE = "launch-force";
	protected static final String WAVE_ARC = "wave-arc";

	private static final String FALLING_BLOCK_METADATA_KEY = "block-launch";

	private static final BlockFace[] SPREAD_FACES = {
			BlockFace.DOWN,
			BlockFace.NORTH,
			BlockFace.SOUTH,
			BlockFace.EAST,
			BlockFace.WEST,
			BlockFace.UP
	};

	public SkillBaseBlockWave(Heroes plugin, String name) {
		super(plugin, name);
		Bukkit.getServer().getPluginManager().registerEvents(new BlockFallListener(), plugin);
	}

	protected boolean castBlockWave(Hero hero, Block source) {
		if (source != null && source.getType().isSolid()) {
			final Location center = source.getLocation().add(0.5, 0.5, 0.5);

			Location playerLoc = hero.getPlayer().getEyeLocation();
			playerLoc.setPitch(0);
			final Vector direction = playerLoc.getDirection();

			final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5d, false);
			final double expansionRate = SkillConfigManager.getUseSetting(hero, this, EXPANSION_RATE, 1d, false);
			final double launchForce = SkillConfigManager.getUseSetting(hero, this, LAUNCH_FORCE, 0.2, false);
			final double waveArc = Math.toRadians(SkillConfigManager.getUseSetting(hero, this, WAVE_ARC, 360d, false)) / 2d;

			final int blockDepth =  Math.max(source.getY() - SkillConfigManager.getUseSetting(hero, this, DEPTH_NODE, 5, false), 0);
			final int blockHeight = Math.min(source.getY() + SkillConfigManager.getUseSetting(hero, this, HEIGHT_NODE, 0, false), hero.getPlayer().getWorld().getMaxHeight());

			final List<WaveBlock> wave = new ArrayList<>();
			spread(source, null, new HashSet<Block>(), new Predicate<Block>() {
				@Override
				public boolean apply(Block block) {
					if (block.getY() >= blockDepth && block.getY() <= blockHeight) {
						Location blockCenter = block.getLocation().add(0.5, 0.5, 0.5);
						double distance = distanceXZ(blockCenter, center);
						if (distance < radius) {
							Vector blockDirection = blockCenter.toVector().subtract(center.toVector());
							blockDirection.setY(0);
							if ((blockDirection.getX() < Vector.getEpsilon() && blockDirection.getZ() < Vector.getEpsilon()) || direction.angle(blockDirection) <= waveArc) {
								Block aboveBlock = block.getRelative(BlockFace.UP);
								if (aboveBlock != null && !aboveBlock.getType().isSolid()) {
									long launchTime = (long) (distance / expansionRate);
									WaveBlock waveBlock = new WaveBlock(block, launchTime, launchForce);
									wave.add(waveBlock);
								}
							}

							return true;
						}
					}

					return false;
				}
			});

			Collections.sort(wave);

			new BukkitRunnable() {

				long tick = 0;
				Iterator<WaveBlock> iterator = wave.iterator();
				WaveBlock waveBlock;

				{
					if (iterator.hasNext()) {
						waveBlock = iterator.next();
						runTaskTimer(plugin, 0, 1);
					}
				}

				@Override
				public void run() {
					do {
						waveBlock.launch();
					} while (advance());
					tick++;
				}

				private boolean advance() {
					if (iterator.hasNext()) {
						waveBlock = iterator.next();
						return waveBlock.getLaunchTime() <= tick;
					} else {
						waveBlock = null;
						cancel();
						return false;
					}
				}
			};

			return true;
		}

		return false;
	}

	private double distanceXZ(Location l1, Location l2) {
		return Math.sqrt(square(l1.getX() - l2.getX()) + square(l1.getZ() - l2.getZ()));
	}

	private void spread(Block source, BlockFace from, Set<Block> tracked, Predicate<Block> onSpread) {
		if (source != null && source.getType().isSolid() && !tracked.contains(source)) {
			tracked.add(source);

			if (onSpread.apply(source)) {
				for (BlockFace face : SPREAD_FACES) {
					if (face != from) {
						spread(source.getRelative(face), face.getOppositeFace(), tracked, onSpread);
					}
				}
			}
		}
	}

	protected interface WaveTarget {
		void onTarget(Hero hero, LivingEntity target, Location center);
	}

	private class WaveBlock implements Comparable<WaveBlock> {

		private final Block block;
		private final long launchTime;
		private final double launchForce;

		public WaveBlock(Block block, long launchTime, double launchForce) {
			this.block = block;
			this.launchTime = launchTime;
			this.launchForce = launchForce;
		}

		public void launch() {
			@SuppressWarnings("deprecation")// Bukkit can Sukkit
			FallingBlock fb = block.getWorld()
					.spawnFallingBlock(block
							.getRelative(BlockFace.UP)
							.getLocation(), block.getType(), block.getData());

			fb.setDropItem(false);
			fb.setVelocity(new Vector(0, launchForce, 0));

			fb.setMetadata(FALLING_BLOCK_METADATA_KEY, new FixedMetadataValue(plugin, new Object()));
		}

		public long getLaunchTime() {
			return launchTime;
		}

		@Override
		public int compareTo(WaveBlock o) {
			return Long.compare(launchTime, o.launchTime);
		}
	}

	private class BlockFallListener implements Listener {
		@EventHandler
		private void onBlockWaveFall(EntityChangeBlockEvent event) {
			if (event.getEntity().getType() == EntityType.FALLING_BLOCK && event.getEntity().hasMetadata(FALLING_BLOCK_METADATA_KEY)) {
				event.setCancelled(true);
				event.getEntity().remove();
			}
		}
	}
}
