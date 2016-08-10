package com.herocraftonline.heroes.characters.skill.public1;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public abstract class SkillBaseBlockPulse extends ActiveSkill {

	public SkillBaseBlockPulse(Heroes plugin, String name) {
		super(plugin, name);
	}

	private final LinkedList<PulseBlock> calculatePulseList(Player player, double velocity, double radius, int slopeLimit, double arcLength) {
		LinkedList<PulseBlock> list = new LinkedList<>();
		Set<Block> tracked = new HashSet<>();

		if (player.getLocation().getBlock().getType() == Material.AIR && player.getEyeLocation().getBlock().getType() == Material.AIR) {
			Location origin = player.getLocation();
			Block centerBlock = origin.getBlock().getRelative(BlockFace.DOWN);
			if (!Util.transparentBlocks.contains(centerBlock.getType())) {

			}
		}

		return list;
	}

	private final double distance(double x1, double y1, double x2, double y2) {
		return Math.sqrt(x1 * x2 + y1 * y2);
	}



	private final class PulseBlock implements Comparable<PulseBlock> {

		private final Block block;
		private final long tickOrder;

		public PulseBlock(Block block, long tickOrder) {
			this.block = block;
			this.tickOrder = tickOrder;
		}

		@Override
		public int compareTo(PulseBlock o) {
			return Long.compare(tickOrder, o.tickOrder);
		}
	}
}
