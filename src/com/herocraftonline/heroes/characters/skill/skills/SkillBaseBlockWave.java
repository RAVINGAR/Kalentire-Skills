package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import org.bukkit.Location;
import org.bukkit.block.Block;

public abstract class SkillBaseBlockWave extends ActiveSkill {

	protected static final String HEIGHT_NODE = "height";
	protected static final String DEPTH_NODE = "depth";
	protected static final String EXPANSION_RATE = "expansion-rate";

	public SkillBaseBlockWave(Heroes plugin, String name) {
		super(plugin, name);
	}

	protected void castBlockWave(Hero hero, Location center, double radius, double height, double depth) {

	}

	private class WaveBlock {

		private final Block block;
		private final long launchTime;

		public WaveBlock(Block block, long launchTime) {
			this.block = block;
			this.launchTime = launchTime;
		}

		public void launch() {

		}

		public long getLaunchTime() {
			return launchTime;
		}
	}
}
