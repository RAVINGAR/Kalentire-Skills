package com.herocraftonline.heroes.characters.skill.remastered;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;

import static com.herocraftonline.heroes.characters.skill.SkillConfigManager.getUseSetting;

public class SkillSmeltIron extends ActiveSkill{
	private static final String base="base-ingot-chance",gain="chance-gain-per-level";
	
	public SkillSmeltIron(Heroes plugin) {
		super(plugin, "SmeltIron");
		setDescription("You can turn iron ore into an iron ingot with a $1 percent chance of getting an extra ingot");
		setUsage("/skill smeltiron");
		setIdentifiers("skill smeltiron");
		setArgumentRange(0, 0);
        setTypes(SkillType.ITEM_MODIFYING, SkillType.UNBINDABLE);
	}
	
	private double calculateChance(Hero hero){
		return getUseSetting(hero, this, base, 10, false)
					+getUseSetting(hero,this,gain,0.2,false)*hero.getHeroLevel(hero.getSecondaryClass());
	}
	
	@Override
	public String getDescription(Hero hero) {
		return getDescription().replace("$1", calculateChance(hero)+"");
	}

	@Override
	public SkillResult use(Hero hero, String[] args) {
		Player player = hero.getPlayer();

		PlayerInventory inventory = player.getInventory();
		ItemStack[] contents = inventory.getContents();

        boolean addIngot = false;
		for (int i = 0; i < contents.length; i++) {
			ItemStack stack = contents[i];
			if ((stack != null) && (stack.getType() == Material.RAW_IRON)) {
				// Remove 1 Iron ore from their inventory
				final int cur_amount = stack.getAmount();
				if (cur_amount == 1)
					player.getInventory().setItem(i, null);
				else
					stack.setAmount(cur_amount - 1);

				addIngot = true;

				// Exit loop
				break;
			}
		}

		if (!addIngot) {
			player.sendMessage(ChatColor.GRAY + "You do not have any iron ore to smelt!");

			return SkillResult.FAIL;
		}

		broadcastExecuteText(hero);

		int amount = 1;
		hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6F, 1.0F);

		if (calculateChance(hero) > Math.random() * 100.0D) {
			amount++;
			player.sendMessage(ChatColor.GRAY + "You got an extra ingot from the smelting process!");
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
		return config;
	}
}
