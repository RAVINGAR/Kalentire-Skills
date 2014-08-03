package com.herocraftonline.heroes.characters.skill.skills;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillEnchantingBook extends ActiveSkill {
	HashMap<Player,PlayerExecuteData> executors;
	
	private class PlayerExecuteData {
		Map<Enchantment, Integer> enchant;
		long expirationTime;
		ItemStack hand;
		int heldSlot;
		public PlayerExecuteData(Map<Enchantment, Integer> enchant, long expirationTime, ItemStack hand, int heldSlot) {
			this.enchant = enchant;
			this.expirationTime = expirationTime;
			this.hand =  hand;
			this.heldSlot = heldSlot;
		}
		
	}
	public SkillEnchantingBook(Heroes plugin) {
		super(plugin, "EnchantingBook");
		setUsage("/skill enchantingbook");
		setDescription("Grants the ability to use book enchanting.");
		setArgumentRange(0,0);
		executors = new LinkedHashMap<Player, PlayerExecuteData>(100);
		setIdentifiers("skill enchantingbook");
        setTypes(SkillType.SILENCEABLE);
	}

	@Override
	public SkillResult use(Hero h, String[] args) {
		Player p = h.getPlayer();
		if(!executors.containsKey(p)) {
			ItemStack hand = p.getItemInHand();
			if(!hand.getType().equals(Material.ENCHANTED_BOOK)) {
				p.sendMessage(ChatColor.GRAY + "This is not an Enchanted Book!");
				return SkillResult.INVALID_TARGET_NO_MSG;
			}
			Map<Enchantment, Integer> enchant = ((EnchantmentStorageMeta)hand.getItemMeta()).getStoredEnchants();
			executors.put(p, new PlayerExecuteData(enchant, System.currentTimeMillis() + 10000 , hand, p.getInventory().getHeldItemSlot()));
			p.sendMessage(ChatColor.GRAY + "Select an item to enchant by using this skill again!");
			return SkillResult.INVALID_TARGET_NO_MSG; //Prevent cooldowns/reagent use from triggering
		} else {
			if(executors.get(p).expirationTime <= System.currentTimeMillis()) {
				p.sendMessage(ChatColor.GRAY + "Your selection has expired, please try again");
				executors.remove(p);
			}
			ItemStack tool = p.getItemInHand();
			if(!isEnchantable(tool)) {
				p.sendMessage(ChatColor.GRAY + "This is not an enchantable item!");
				return SkillResult.INVALID_TARGET_NO_MSG;
			}
			PlayerExecuteData struct = executors.get(p);
			executors.remove(p);
			if(!p.getInventory().getItem(struct.heldSlot).equals(struct.hand)) {
				p.sendMessage(ChatColor.GRAY + "Cannot find the original enchantment book inside your inventory anymore! Did you move it?");
				return SkillResult.FAIL;
			}
			p.getInventory().getItem(struct.heldSlot).setAmount(0);
			p.updateInventory(); //Blah blah deprecated but bukkit doesn't include new functionality for it
			tool.addEnchantments(struct.enchant);
		}
		return SkillResult.NORMAL;
	}

	@Override
	public String getDescription(Hero arg0) {
		return getDescription();
	}
	
	//Code shamelessly stolen from mcmmo because >effort to individually type out 50000 cases
	public static boolean isSword(ItemStack is) {
		switch (is.getType()) {
		case DIAMOND_SWORD:
		case GOLD_SWORD:
		case IRON_SWORD:
		case STONE_SWORD:
		case WOOD_SWORD:
			return true;

		default:
			return false;
		}
	}

	/**
	 * Checks if the item is a hoe.
	 *
	 * @param is Item to check
	 * @return true if the item is a hoe, false otherwise
	 */
	public static boolean isHoe(ItemStack is) {
		switch (is.getType()) {
		case DIAMOND_HOE:
		case GOLD_HOE:
		case IRON_HOE:
		case STONE_HOE:
		case WOOD_HOE:
			return true;
			
		default:
			return false;
		}
	}

	/**
	 * Checks if the item is a shovel.
	 *
	 * @param is Item to check
	 * @return true if the item is a shovel, false otherwise
	 */
	public static boolean isShovel(ItemStack is) {
		switch (is.getType()) {
		case DIAMOND_SPADE:
		case GOLD_SPADE:
		case IRON_SPADE:
		case STONE_SPADE:
		case WOOD_SPADE:
			return true;

		default:
			return false;
		}
	}

	/**
	 * Checks if the item is an axe.
	 *
	 * @param is Item to check
	 * @return true if the item is an axe, false otherwise
	 */
	public static boolean isAxe(ItemStack is) {
		switch (is.getType()) {
		case DIAMOND_AXE:
		case GOLD_AXE:
		case IRON_AXE:
		case STONE_AXE:
		case WOOD_AXE:
			return true;

		default:
			return false;
		}
	}

	/**
	 * Checks if the item is a pickaxe.
	 *
	 * @param is Item to check
	 * @return true if the item is a pickaxe, false otherwise
	 */
	public static boolean isPickaxe(ItemStack is) {
		switch (is.getType()) {
		case DIAMOND_PICKAXE:
		case GOLD_PICKAXE:
		case IRON_PICKAXE:
		case STONE_PICKAXE:
		case WOOD_PICKAXE:
			return true;

		default:
			return false;
		}
	}

	/**
	 * Checks if the item is a helmet.
	 *
	 * @param is Item to check
	 * @return true if the item is a helmet, false otherwise
	 */
	public static boolean isHelmet(ItemStack is) {
		switch (is.getType()) {
		case DIAMOND_HELMET:
		case GOLD_HELMET:
		case IRON_HELMET:
		case LEATHER_HELMET:
			return true;

		default:
			return false;
		}
	}

	/**
	 * Checks if the item is a chestplate.
	 *
	 * @param is Item to check
	 * @return true if the item is a chestplate, false otherwise
	 */
	public static boolean isChestplate(ItemStack is) {
		switch (is.getType()) {
		case DIAMOND_CHESTPLATE:
		case GOLD_CHESTPLATE:
		case IRON_CHESTPLATE:
		case LEATHER_CHESTPLATE:
			return true;

		default:
			return false;
		}
	}

	/**
	 * Checks if the item is a pair of pants.
	 *
	 * @param is Item to check
	 * @return true if the item is a pair of pants, false otherwise
	 */
	public static boolean isPants(ItemStack is) {
		switch (is.getType()) {
		case DIAMOND_LEGGINGS:
		case GOLD_LEGGINGS:
		case IRON_LEGGINGS:
		case LEATHER_LEGGINGS:
			return true;

		default:
			return false;
		}
	}

	/**
	 * Checks if the item is a pair of boots.
	 *
	 * @param is Item to check
	 * @return true if the item is a pair of boots, false otherwise
	 */
	public static boolean isBoots(ItemStack is) {
		switch (is.getType()) {
		case DIAMOND_BOOTS:
		case GOLD_BOOTS:
		case IRON_BOOTS:
		case LEATHER_BOOTS:
			return true;

		default:
			return false;
		}
	}

	/**
	 * Checks to see if an item is a wearable armor piece.
	 *
	 * @param is Item to check
	 * @return true if the item is armor, false otherwise
	 */
	public static boolean isArmor(ItemStack is) {
		return isLeatherArmor(is) || isGoldArmor(is) || isIronArmor(is) || isDiamondArmor(is);
	}

	/**
	 * Checks to see if an item is a leather armor piece.
	 *
	 * @param is Item to check
	 * @return true if the item is leather armor, false otherwise
	 */
	public static boolean isLeatherArmor(ItemStack is) {
		switch (is.getType()) {
		case LEATHER_BOOTS:
		case LEATHER_CHESTPLATE:
		case LEATHER_HELMET:
		case LEATHER_LEGGINGS:
			return true;

		default:
			return false;
		}
	}

	/**
	 * Checks to see if an item is a gold armor piece.
	 *
	 * @param is Item to check
	 * @return true if the item is gold armor, false otherwise
	 */
	public static boolean isGoldArmor(ItemStack is) {
		switch (is.getType()) {
		case GOLD_BOOTS:
		case GOLD_CHESTPLATE:
		case GOLD_HELMET:
		case GOLD_LEGGINGS:
			return true;

		default:
			return false;
		}
	}

	/**
	 * Checks to see if an item is an iron armor piece.
	 *
	 * @param is Item to check
	 * @return true if the item is iron armor, false otherwise
	 */
	public static boolean isIronArmor(ItemStack is) {
		switch (is.getType()) {
		case IRON_BOOTS:
		case IRON_CHESTPLATE:
		case IRON_HELMET:
		case IRON_LEGGINGS:
			return true;

		default:
			return false;
		}
	}

	/**
	 * Checks to see if an item is a diamond armor piece.
	 *
	 * @param is Item to check
	 * @return true if the item is diamond armor, false otherwise
	 */
	public static boolean isDiamondArmor(ItemStack is) {
		switch (is.getType()) {
		case DIAMOND_BOOTS:
		case DIAMOND_CHESTPLATE:
		case DIAMOND_HELMET:
		case DIAMOND_LEGGINGS:
			return true;

		default:
			return false;
		}
	}

	/**
	 * Checks to see if an item is a tool.
	 *
	 * @param is Item to check
	 * @return true if the item is a tool, false otherwise
	 */
	public static boolean isTool(ItemStack is) {
		return isStoneTool(is) || isWoodTool(is) || isGoldTool(is) || isIronTool(is) || isDiamondTool(is) || isStringTool(is);
	}

	/**
	 * Checks to see if an item is a stone tool.
	 *
	 * @param is Item to check
	 * @return true if the item is a stone tool, false otherwise
	 */
	public static boolean isStoneTool(ItemStack is) {
		switch (is.getType()) {
		case STONE_AXE:
		case STONE_HOE:
		case STONE_PICKAXE:
		case STONE_SPADE:
		case STONE_SWORD:
			return true;

		default:
			return false;
		}
	}

	/**
	 * Checks to see if an item is a wooden tool.
	 *
	 * @param is Item to check
	 * @return true if the item is a wooden tool, false otherwise
	 */
	public static boolean isWoodTool(ItemStack is) {
		switch (is.getType()) {
		case WOOD_AXE:
		case WOOD_HOE:
		case WOOD_PICKAXE:
		case WOOD_SPADE:
		case WOOD_SWORD:
			return true;

		default:
			return false;
		}
	}

	/**
	 * Checks to see if an item is a string tool.
	 *
	 * @param is Item to check
	 * @return true if the item is a string tool, false otherwise
	 */
	public static boolean isStringTool(ItemStack is) {
		switch (is.getType()) {
		case BOW:
		case FISHING_ROD:
			return true;

		default:
			return false;
		}
	}


	/**
	 * Checks to see if an item is a gold tool.
	 *
	 * @param is Item to check
	 * @return true if the item is a stone tool, false otherwise
	 */
	public static boolean isGoldTool(ItemStack is) {
		switch (is.getType()) {
		case GOLD_AXE:
		case GOLD_HOE:
		case GOLD_PICKAXE:
		case GOLD_SPADE:
		case GOLD_SWORD:
			return true;

		default:
			return false;
		}
	}

	/**
	 * Checks to see if an item is an iron tool.
	 *
	 * @param is Item to check
	 * @return true if the item is an iron tool, false otherwise
	 */
	public static boolean isIronTool(ItemStack is) {
		switch (is.getType()) {
		case IRON_AXE:
		case IRON_HOE:
		case IRON_PICKAXE:
		case IRON_SPADE:
		case IRON_SWORD:
		case SHEARS:
			return true;

		default:
			return false;
		}
	}

	/**
	 * Checks to see if an item is a diamond tool.
	 *
	 * @param is Item to check
	 * @return true if the item is a diamond tool, false otherwise
	 */
	public static boolean isDiamondTool(ItemStack is) {
		switch (is.getType()) {
		case DIAMOND_AXE:
		case DIAMOND_HOE:
		case DIAMOND_PICKAXE:
		case DIAMOND_SPADE:
		case DIAMOND_SWORD:
			return true;

		default:
			return false;
		}
	}

	/**
	 * Checks to see if an item is enchantable.
	 *
	 * @param is Item to check
	 * @return true if the item is enchantable, false otherwise
	 */
	public static boolean isEnchantable(ItemStack is) {
		return isArmor(is) || isSword(is) || isAxe(is) || isShovel(is) || isPickaxe(is) || (is.getType() == Material.BOW);
	}
	

}
