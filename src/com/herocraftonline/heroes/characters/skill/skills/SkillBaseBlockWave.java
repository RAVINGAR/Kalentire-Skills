package com.herocraftonline.heroes.characters.skill.skills;

import com.google.common.base.Predicate;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.nms.physics.NMSPhysics;
import com.herocraftonline.heroes.nms.physics.collision.AABB;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import static org.bukkit.Location.locToBlock;
import static org.bukkit.util.NumberConversions.square;

public abstract class SkillBaseBlockWave extends ActiveSkill {

	protected static final String HEIGHT_NODE = "height";
	protected static final String DEPTH_NODE = "depth";
	protected static final String EXPANSION_RATE_NODE = "expansion-rate";
	protected static final String LAUNCH_FORCE_NODE = "launch-force";
	protected static final String WAVE_ARC_NODE = "wave-arc";
	protected static final String HIT_LIMIT_NODE = "hit-limit";

	private static final String FALLING_BLOCK_METADATA_KEY = "block-launch";

	private static final Set<FallingBlock> waveBlocks = new HashSet<>();

	/*private static final BlockFace[] SPREAD_FACES = {
			BlockFace.DOWN,
			BlockFace.NORTH,
			BlockFace.SOUTH,
			BlockFace.EAST,
			BlockFace.WEST,
			BlockFace.UP
	};*/

	public SkillBaseBlockWave(Heroes plugin, String name) {
		super(plugin, name);
		Bukkit.getServer().getPluginManager().registerEvents(new BlockFallListener(), plugin);
	}

	protected void castBlockWave(final Hero hero, Block centerBlock, WaveTargetAction targetAction) {
		if (centerBlock != null) {
			World world = centerBlock.getWorld();
			Location center = centerBlock.getLocation().add(0.5, 0.5, 0.5);

			Location playerLoc = hero.getPlayer().getEyeLocation();
			playerLoc.setPitch(0);
			Vector direction = playerLoc.getDirection();

			double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5d, false);
			double expansionRate = SkillConfigManager.getUseSetting(hero, this, EXPANSION_RATE_NODE, 1d, false);
			double launchForce = SkillConfigManager.getUseSetting(hero, this, LAUNCH_FORCE_NODE, 0.2, false);
			double waveArc = Math.toRadians(SkillConfigManager.getUseSetting(hero, this, WAVE_ARC_NODE, 360d, false)) / 2d;
			int hitLimit = SkillConfigManager.getUseSetting(hero, this, HIT_LIMIT_NODE, 1, false);

			int blockDepth =  Math.max(centerBlock.getY() - SkillConfigManager.getUseSetting(hero, this, DEPTH_NODE, 5, false), 0);
			int blockHeight = Math.min(centerBlock.getY() + SkillConfigManager.getUseSetting(hero, this, HEIGHT_NODE, 0, false), hero.getPlayer().getWorld().getMaxHeight());

			int xMin = locToBlock(center.getX() - radius);
			int xMax = locToBlock(center.getX() + radius);

			int zMin = locToBlock(center.getZ() - radius);
			int zMax = locToBlock(center.getZ() + radius);

			final List<WaveBlock> wave = new ArrayList<>();
			Map<UUID, Integer> hitTracker = new HashMap<>();

			for (int x = xMin; x <= xMax; x++) {
				for (int y = blockDepth; y <= blockHeight; y++) {
					for (int z = zMin; z <= zMax; z++) {
						Block block = world.getBlockAt(x, y, z);
						Block aboveBlock = block.getRelative(BlockFace.UP);
						if (block != null && block.getType().isSolid() && aboveBlock != null && !aboveBlock.getType().isSolid()) {
							if (block.getY() >= blockDepth && block.getY() <= blockHeight) {
								Location blockCenter = block.getLocation().add(0.5, 0.5, 0.5);
								double distance = distanceXZ(blockCenter, center);
								if (distance < radius) {
									if (waveArc < 180) {
										Vector blockDirection = blockCenter.toVector().subtract(center.toVector());
										blockDirection.setY(0);
										if (!(block.getX() == centerBlock.getX() && block.getZ() == centerBlock.getZ()) && direction.angle(blockDirection) > waveArc) {
											continue;
										}
									}

									long launchTime = (long) (distance / expansionRate);
									WaveBlock waveBlock = new WaveBlock(hero, center, block, launchTime, launchForce, targetAction, hitTracker, hitLimit);
									wave.add(waveBlock);
								}
							}
						}
					}
				}
			}

			/*spread(source, null, new HashSet<Block>(), new Predicate<Block>() {
				@Override
				public boolean apply(Block block) {
					if (block.getY() >= blockDepth && block.getY() <= blockHeight) {
						Location blockCenter = block.getLocation().add(0.5, 0.5, 0.5);
						double distance = distanceXZ(blockCenter, center);
						if (distance < radius) {
							Vector blockDirection = blockCenter.toVector().subtract(center.toVector());
							blockDirection.setY(0);
							if ((block.getX() == source.getX() && block.getZ() == source.getZ()) || direction.angle(blockDirection) <= waveArc) {
								Block aboveBlock = block.getRelative(BlockFace.UP);
								if (aboveBlock != null && !aboveBlock.getType().isSolid()) {
									long launchTime = (long) (distance / expansionRate);
									WaveBlock waveBlock = new WaveBlock(hero, center, block, launchTime, launchForce, targetAction);
									wave.add(waveBlock);
								}
							}

							return true;
						}
					}

					return false;
				}
			});*/

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
		}
	}

	private double distanceXZ(Location l1, Location l2) {
		return Math.sqrt(square(l1.getX() - l2.getX()) + square(l1.getZ() - l2.getZ()));
	}

	/*private void spread(Block source, BlockFace from, Set<Block> tracked, Predicate<Block> onSpread) {
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
	}*/

	protected interface WaveTargetAction {
		void onTarget(Hero hero, LivingEntity target, Location center);
	}

	private class WaveBlock implements Comparable<WaveBlock> {

		private final Hero hero;
		private final Location center;
		private final Block block;
		private final long launchTime;
		private final double launchForce;
		private final WaveTargetAction targetAction;
		private final Map<UUID, Integer> hitTracker;
		private final int hitLimit;

		public WaveBlock(Hero hero, Location center, Block block, long launchTime, double launchForce, WaveTargetAction targetAction, Map<UUID, Integer> hitTracker, int hitLimit) {
			this.hero = hero;
			this.center = center;
			this.block = block;
			this.launchTime = launchTime;
			this.launchForce = launchForce;
			this.targetAction = targetAction;
			this.hitTracker = hitTracker;
			this.hitLimit = hitLimit;
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
			waveBlocks.add(fb);

			NMSPhysics physics = NMSHandler.getInterface().getNMSPhysics();

			AABB aabb = physics.createAABB(block.getX(), block.getY(), block.getZ(), block.getX() + 1, block.getY() + 3, block.getZ() + 1);
			for (Entity target : physics.getEntitiesInVolume(block.getWorld(), hero.getPlayer(), aabb, new Predicate<Entity>() {
				@Override
				public boolean apply(Entity entity) {
					if (entity instanceof LivingEntity) {
						Integer hitCount = hitTracker.get(entity.getUniqueId());
						if (hitCount == null) {
							hitCount = 0;
						}

						boolean hit = hitCount++ < hitLimit;
						hitTracker.put(entity.getUniqueId(), hitCount);
						return hit;
					}

					return false;
				}
			})) {
				targetAction.onTarget(hero, (LivingEntity) target, center);
			}
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
			if (event.getEntity() instanceof FallingBlock && event.getEntity().hasMetadata(FALLING_BLOCK_METADATA_KEY)) {
				event.setCancelled(true);
				event.getEntity().remove();
				waveBlocks.remove(event.getEntity());
			}
		}

		@EventHandler
		public void onPluginDisable(PluginDisableEvent event) {
			if (event.getPlugin() == plugin) {
				for (FallingBlock waveBlock : waveBlocks) {
					waveBlock.remove();
				}
			}
		}
	}
}
