package com.herocraftonline.heroes.characters.skill.public1;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.listeners.HBlockListener;
import com.herocraftonline.heroes.util.Util;

public class SkillHerbalism extends PassiveSkill {

	public SkillHerbalism(Heroes plugin) {
		super(plugin, "Herbalism");
		setDescription("You have a $1% chance to harvest extra herbs, fruits, and vegetables.");
		Bukkit.getServer().getPluginManager().registerEvents(new SkillBlockListener(this), plugin);
	}

	@Override
	public String getDescription(Hero hero) {
		double chance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.CHANCE_PER_LEVEL, .001, false);
		int level = hero.getSkillLevel(this);
		if (level < 1)
			level = 1;
		return getDescription().replace("$1", Util.stringDouble(chance * level * 100));
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.APPLY_TEXT.node(), "");
		node.set(SkillSetting.UNAPPLY_TEXT.node(), "");
		node.set(SkillSetting.CHANCE_PER_LEVEL.node(), .001);

		return node;
	}

	/**
	 * Something messes up just using getData(), need to turn the extra leaves into a player-usable version.
	 */
	public byte transmuteLeaves(Material mat, byte data)
	{
		if (mat == Material.LEAVES)
		{
			switch (data)
			{
			case 4:
			case 8:
			case 12:
				return 0;
			case 5:
			case 9:
			case 13:
				return 1;
			case 6:
			case 10:
			case 14:
				return 2;
			case 7:
			case 11:
			case 15:
				return 3;
			default:
				return 0;
			}
		}
		else if (mat == Material.LEAVES_2)
		{
			switch (data)
			{
			case 4:
			case 8:
			case 12:
				return 0;
			case 5:
			case 9:
			case 13:
				return 1;
			default:
				return 0;
			}
		}
		else
		{
			return 0;
		}
	}

	public class SkillBlockListener implements Listener {

		private Skill skill;

		SkillBlockListener(Skill skill) {
			this.skill = skill;
		}

		@EventHandler(priority = EventPriority.MONITOR)
		public void onBlockBreak(BlockBreakEvent event) {
			if (event.isCancelled()) {
				return;
			}

			Block block = event.getBlock();
			if (HBlockListener.placedBlocks.containsKey(block.getLocation())) {
				return;
			}

			int extraDrops = 0;
			Material mat = null;
			switch (block.getType()) {
			case CROPS:                    
				mat = Material.WHEAT;
				extraDrops = 3;
				break;
			case SUGAR_CANE_BLOCK:
				mat = Material.SUGAR_CANE;
				extraDrops = 2;
				break;
			case MELON_BLOCK:
				mat = Material.MELON;
				extraDrops = 7;
				break;
			case SAPLING:
			case LEAVES:
			case YELLOW_FLOWER:
			case RED_ROSE:
			case BROWN_MUSHROOM:
			case RED_MUSHROOM:
			case CACTUS:
			case LONG_GRASS:
			case PUMPKIN:
			case DEAD_BUSH:
				break;
			default:
				return;
			}

			Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
			if (!hero.hasEffect("Herbalism") || Util.nextRand() >= SkillConfigManager.getUseSetting(hero, skill, SkillSetting.CHANCE_PER_LEVEL, .001, false) * hero.getSkillLevel(skill)) {
				return;
			}

			if (extraDrops != 0) {
				extraDrops = Util.nextInt(extraDrops) + 1;
			} else {
				extraDrops = 1;
			}
			if (mat != null) {
				block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(mat, extraDrops));
			} 
			else if (block.getType() == Material.LEAVES || block.getType() == Material.LEAVES_2)
			{
				block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(block.getType(), extraDrops, transmuteLeaves(block.getType(), block.getData())));
			}
			else
			{
				block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(block.getType(), extraDrops, block.getData()));
			}
		}
	}
}
