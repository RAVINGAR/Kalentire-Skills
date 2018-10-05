package com.herocraftonline.heroes.characters.skill.skills;

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

import static com.herocraftonline.heroes.characters.skill.SkillConfigManager.getUseSetting;

public class SkillMegaSmeltIron extends ActiveSkill{
	private static final String base="base-ingot-chance",gain="chance-gain-per-level";

	public SkillMegaSmeltIron(Heroes plugin) {
		super(plugin, "MegaSmeltIron");
		setDescription("You can turn $1 iron ore into $1 iron ingots with a $2 percent chance of getting extra ingots");
		setUsage("/skill megasmeltiron");
		setIdentifiers("skill megasmeltiron");
		setArgumentRange(0, 0);
        setTypes(SkillType.ITEM_MODIFYING, SkillType.UNBINDABLE);
	}
	
	private double calculateChance(Hero hero){
		return getUseSetting(hero, this, base, 10, false)
					+getUseSetting(hero,this,gain,0.2,false)*hero.getHeroLevel(hero.getSecondClass());
	}
	
	@Override
	public String getDescription(Hero hero) {
        int itemsToSmelt = SkillConfigManager.getUseSetting(hero, this, "items-to-smelt", 10, false);
		return getDescription().replace("$1",itemsToSmelt+"").replace("$2", calculateChance(hero)+"");
	}

	@Override
	public SkillResult use(Hero hero, String[] args) {
		Player player = hero.getPlayer();

		PlayerInventory inventory = player.getInventory();
		ItemStack[] contents = inventory.getContents();

        int itemsToSmelt = SkillConfigManager.getUseSetting(hero, this, "items-to-smelt", 10, false);
        int itemsSmelted = 0;
        for (int i = 0; i < contents.length; i++) {
			ItemStack stack = contents[i];
			if ((stack != null) && (stack.getType() == Material.IRON_ORE)) {
				// Remove 10 Iron ore from their inventory
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
			player.sendMessage(ChatColor.GRAY + "You do not have any iron ore to smelt!");

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
					.replace("$1",(amount-itemsSmelted)+""));
		}

        HashMap<Integer, ItemStack> leftOvers = inventory.addItem(new ItemStack(Material.IRON_INGOT, amount));
		for (java.util.Map.Entry<Integer, ItemStack> entry : leftOvers.entrySet()) {
			player.getWorld().dropItemNaturally(player.getLocation(), entry.getValue());
			player.sendMessage("Items have been dropped at your feet!");
		}

		return SkillResult.NORMAL;
	}
	
	@Override
	public final ConfigurationSection getDefaultConfig(){
		ConfigurationSection config = super.getDefaultConfig();
		config.set(SkillSetting.NO_COMBAT_USE.node(), true);
		config.set(base, 10);
		config.set(gain,  0.2f);//max possible price per ingot is 11c at level 60, using defaults
        config.set("items-to-smelt", 10);
		return config;
	}
}
