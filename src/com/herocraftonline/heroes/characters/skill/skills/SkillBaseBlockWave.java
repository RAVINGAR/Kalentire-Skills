package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.TreeSet;

import static org.bukkit.Location.locToBlock;
import static org.bukkit.util.NumberConversions.square;

public abstract class SkillBaseBlockWave extends ActiveSkill {

	protected static final String HEIGHT_NODE = "height";
	protected static final String DEPTH_NODE = "depth";
	protected static final String EXPANSION_RATE = "expansion-rate";

	public SkillBaseBlockWave(Heroes plugin, String name) {
		super(plugin, name);
	}

	protected void castBlockWave(Hero hero, Location center) {
		World world = hero.getPlayer().getWorld();
		double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 0, false);
		double expansionRate = SkillConfigManager.getUseSetting(hero, this, EXPANSION_RATE, 1, false);
		int blockDepth = locToBlock(center.getY() + SkillConfigManager.getUseSetting(hero, this, DEPTH_NODE, 5, false));
		int blockHeight = locToBlock(center.getY() + SkillConfigManager.getUseSetting(hero, this, HEIGHT_NODE, 0, false));
		int minX = locToBlock(center.getX() - radius);
		int maxX = locToBlock(center.getX() + radius);
		int minZ = locToBlock(center.getZ() - radius);
		int maxZ = locToBlock(center.getZ() + radius);

		final TreeSet<WaveBlock> queue = new TreeSet<>();

		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				for (int y = blockDepth; y <= blockHeight; y++) {
					Block block = world.getBlockAt(x, y, z);
					if (block != null &&
							/*
							Spawning falling block on a block removes it client side, so we must spawn it above
							 */
							block.getRelative(BlockFace.UP).getType() == Material.AIR) {
						long launchTime = (long) (distanceXZ(block.getLocation().add(0.5, 0.5, 0.5), center) / expansionRate);
						WaveBlock waveBlock = new WaveBlock(block, launchTime);
						queue.add(waveBlock);
					}
				}
			}
		}

		new BukkitRunnable() {

			long tick = 0;
			Iterator<WaveBlock> iterator = queue.iterator();
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
					cancel();
					return false;
				}
			}
		};
	}

	private double distanceXZ(Location l1, Location l2) {
		return Math.sqrt(square(l1.getX() + l2.getX()) + square(l1.getZ() + l2.getZ()));
	}

	private class WaveBlock implements Comparable<WaveBlock> {

		private final Block block;
		private final long launchTime;

		public WaveBlock(Block block, long launchTime) {
			this.block = block;
			this.launchTime = launchTime;
		}

		public void launch() {
			@SuppressWarnings("deprecation")// Bukkit can Sukkit
			FallingBlock fb = block.getWorld().spawnFallingBlock(block
					/*
					Spawning falling block on a block removes it client side, so we must spawn it above
					 */
					.getRelative(BlockFace.UP)
					.getLocation(), block.getType(), block.getData());

			fb.setDropItem(false);
			fb.setVelocity(new Vector(0, 0.4, 0));

			fb.setMetadata("block-launch", new FixedMetadataValue(plugin, new Object()));
		}

		public long getLaunchTime() {
			return launchTime;
		}

		@Override
		public int compareTo(WaveBlock o) {
			return Long.compare(launchTime, o.launchTime);
		}
	}

	@EventHandler
	private void onFallingBlockFall(EntityChangeBlockEvent event) {
		if (event.getEntity().getType() == EntityType.FALLING_BLOCK && event.getEntity().hasMetadata("block-launch")) {
			event.setCancelled(true);
		}
	}
}
