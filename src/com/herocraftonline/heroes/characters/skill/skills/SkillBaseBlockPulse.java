package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.SortedSet;
import java.util.TreeSet;

public abstract class SkillBaseBlockPulse extends ActiveSkill {

	public SkillBaseBlockPulse(Heroes plugin, String name) {
		super(plugin, name);
	}

	private final SortedSet<PulseBlock> calculatePulseSet(Player player, double velocity) {
		SortedSet<PulseBlock> result = new TreeSet<>();

		Block origin = player.getLocation().getBlock();
		if (origin.getType() == Material.AIR && player.getEyeLocation().getBlock().getType() == Material.AIR) {
			
		}

		return result;
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
