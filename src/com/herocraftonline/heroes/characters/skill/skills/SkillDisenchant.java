package com.herocraftonline.heroes.characters.skill.skills;
//src=http://pastie.org/private/qi5088ssrpskghzs3tujg#31
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.util.Util;

public class SkillDisenchant extends ActiveSkill {

	public SkillDisenchant(Heroes plugin) {
		super(plugin, "Disenchant");
		setDescription("You are able to disenchant items, returning them to normal.");
		setIdentifiers(new String[] { "skill disenchant" });
		setUsage("/skill disenchant");
		setArgumentRange(0,0);
	}

	@SuppressWarnings("deprecation")
	@Override
	public SkillResult use(Hero h, String[] args) {
		ItemStack hand = h.getPlayer().getItemInHand();
		if(!(Util.isArmor(hand.getType()) || Util.isWeapon(hand.getType()))) {
			h.getPlayer().sendMessage(ChatColor.GRAY + "This is not a disenchantable item!");
			return SkillResult.INVALID_TARGET_NO_MSG;
		}
		Random randgen = new Random();
		//Get all enchants currently on an item
		Iterator<Entry<Enchantment, Integer>> enchants = hand.getEnchantments().entrySet().iterator();
		//Go through each of these enchants 1 by 1
		while(enchants.hasNext()) {
			Entry<Enchantment, Integer> next = enchants.next();
			Enchantment ench = next.getKey();
			//Skip over sharpness/prot/punch because  that is durability dependent, not enchantment dependent
			if(ench.equals(Enchantment.ARROW_DAMAGE) || ench.equals(Enchantment.DAMAGE_ALL) || ench.equals(Enchantment.PROTECTION_ENVIRONMENTAL)) {
				continue;
			}
			hand.removeEnchantment(ench);
			//Handle breaking
			if(randgen.nextInt(100) < Math.pow(h.getLevel(h.getSecondClass()),-1)*100) {
				h.getPlayer().sendMessage(ChatColor.GRAY + "Oh no your item broke :("); //Like we actually give a fuck
				hand.setAmount(0);
				h.getPlayer().updateInventory();
				break;
			}
		}
		return SkillResult.NORMAL;
	}

	@Override
	public String getDescription(Hero h) {
		return getDescription().replace("$1", Math.pow(h.getLevel(h.getSecondClass()),-1)*100 + "");
	}


}