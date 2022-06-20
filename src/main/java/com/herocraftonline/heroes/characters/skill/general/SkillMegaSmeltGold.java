package com.herocraftonline.heroes.characters.skill.general;

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
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;

public class SkillMegaSmeltGold extends ActiveSkill {

	public SkillMegaSmeltGold(Heroes plugin) {
		super(plugin, "MegaSmeltGold");
		setDescription("You can turn $1 gold ore into $1 gold ingots with a $2 percent chance of getting extra ingots");
		setUsage("/skill megasmeltgold");
		setIdentifiers("skill megasmeltgold");
		setArgumentRange(0, 0);
        setTypes(SkillType.ITEM_MODIFYING, SkillType.UNBINDABLE);
	}

	public String getDescription(Hero hero) {
		int itemsToSmelt = SkillConfigManager.getUseSetting(hero, this, "items-to-smelt", 10, false);
		return getDescription().replace("$1", itemsToSmelt+"").replace("$2", calculateChance(hero) + "");
	}

	public final ConfigurationSection getDefaultConfig() {
		ConfigurationSection config = super.getDefaultConfig();
		config.set(SkillSetting.NO_COMBAT_USE.node(), true);
		config.set("base-nugget-chance", 10);
		config.set("chance-gain-per-level", 0.25F);
		config.set("items-to-smelt", 10);
		return config;
	}

	public SkillResult use(Hero hero, String[] args) {
		Player player = hero.getPlayer();

		PlayerInventory inventory = player.getInventory();
		ItemStack[] contents = inventory.getContents();

		int itemsToSmelt = SkillConfigManager.getUseSetting(hero, this, "items-to-smelt", 10, false);
		int itemsSmelted = 0;
        for (int i = 0; i < contents.length; i++) {
			ItemStack stack = contents[i];
			if ((stack != null) && (stack.getType() == Material.GOLD_ORE)) {
				// Remove 10 gold ore from their inventory
				final int curAmount = stack.getAmount();
				if (curAmount <= (itemsToSmelt - itemsSmelted)) {
					player.getInventory().setItem(i, null);
					itemsSmelted += curAmount;
				} else {
					stack.setAmount(curAmount - (itemsToSmelt - itemsSmelted));
					itemsSmelted += itemsToSmelt - itemsSmelted;
				}

                // Exit loop
				if (itemsSmelted == itemsToSmelt) {
					break;
				}
			}
		}

		if (itemsSmelted == 0) {
			player.sendMessage(ChatColor.GRAY + "You do not have any gold ore to smelt!");

			return SkillResult.FAIL;
		}

		broadcastExecuteText(hero);

		int amount = itemsSmelted;
		hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6F, 1.0F);

		for (int i = 0; i < itemsSmelted; i++) {
			if (calculateChance(hero) > Math.random() * 100.0D) {
				amount++;
			}
		}
		if (amount > itemsSmelted){
			player.sendMessage(ChatColor.GRAY + "You got $1 extra ingots from the smelting process!"
					.replace("$1", (amount-itemsSmelted)+""));
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