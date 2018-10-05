package com.herocraftonline.heroes.characters.skill.pack4;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;

public class SkillSmeltGold extends ActiveSkill {

	public SkillSmeltGold(Heroes plugin) {
		super(plugin, "SmeltGold");
		setDescription("You can turn gold ore into a gold ingot with a $1 percent chance of getting an extra ingot");
		setUsage("/skill smeltgold");
		setIdentifiers("skill smeltgold");
		setArgumentRange(0, 0);
        setTypes(SkillType.ITEM_MODIFYING, SkillType.UNBINDABLE);
	}

	public String getDescription(Hero hero) {
		return getDescription().replace("$1", calculateChance(hero) + "");
	}

	public final ConfigurationSection getDefaultConfig() {
		ConfigurationSection config = super.getDefaultConfig();
		config.set(SkillSetting.NO_COMBAT_USE.node(), true);
		config.set("base-nugget-chance", 10);
		config.set("chance-gain-per-level", 0.25F);
		return config;
	}

	public SkillResult use(Hero hero, String[] args) {
		Player player = hero.getPlayer();

		PlayerInventory inventory = player.getInventory();
		ItemStack[] contents = inventory.getContents();

        boolean addIngot = false;
		for (int i = 0; i < contents.length; i++) {
			ItemStack stack = contents[i];
			if ((stack != null) && (stack.getType() == Material.GOLD_ORE)) {
				// Remove 1 gold ore from their inventory
				int curAmount = stack.getAmount();
				if (curAmount == 1)
					player.getInventory().setItem(i, null);
				else
					stack.setAmount(curAmount - 1);

				addIngot = true;

				// Exit loop
				break;
			}
		}

		if (!addIngot) {
			player.sendMessage(ChatColor.GRAY + "You do not have any gold ore to smelt!");

			return SkillResult.FAIL;
		}

		broadcastExecuteText(hero);

		int amount = 1;
		hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6F, 1.0F);

		if (calculateChance(hero) > Math.random() * 100.0D) {
			amount++;
			player.sendMessage(ChatColor.GRAY + "You got an extra ingot from the smelting process!");
		}

		HashMap<Integer, ItemStack> leftOvers = inventory.addItem(new ItemStack(Material.GOLD_INGOT, amount));
		for (java.util.Map.Entry<Integer, ItemStack> entry : leftOvers.entrySet()) {
			player.getWorld().dropItemNaturally(player.getLocation(), entry.getValue());
			player.sendMessage("Items have been dropped at your feet!");
		}

		return SkillResult.NORMAL;
	}

	private double calculateChance(Hero hero) {
		return SkillConfigManager.getUseSetting(hero, this, "base-nugget-chance", 5, false) + SkillConfigManager.getUseSetting(hero, this, "chance-gain-per-level", 0.2D, false) * hero.getHeroLevel(hero.getSecondClass());
	}
}