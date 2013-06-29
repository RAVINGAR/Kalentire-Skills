package com.herocraftonline.heroes.characters.skill.skills;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.ResidencePermissions;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.SkillResult.ResultType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.townships.HeroTowns;

public class SkillGiganticRunestone extends ActiveSkill {

	private boolean herotowns = false;
	private HeroTowns ht;
	private boolean residence = false;

	public SkillGiganticRunestone(Heroes plugin) {
		super(plugin, "GiganticRunestone");
		setDescription("You imbue a redstone block with an Gigantic Runestone. Gigantic Runestones $1");
		setUsage("/skill giganticrunestone");
		setArgumentRange(0, 0);
		setIdentifiers("skill giganticrunestone");
		setTypes(SkillType.TELEPORT, SkillType.ITEM, SkillType.SILENCABLE);

		try {
			Residence res = (Residence) plugin.getServer().getPluginManager().getPlugin("Residence");
			if (res != null)
				residence = true;
			HeroTowns ht = (HeroTowns) plugin.getServer().getPluginManager().getPlugin("HeroTowns");
			if (ht != null) {
				this.ht = ht;
				herotowns = true;
			}
		}
		catch (Exception e) {
			Heroes.log(Level.SEVERE, "Could not get Residence or HeroTowns! Region checking may not work!");
		}
	}

	public String getDescription(Hero hero) {

		int maxUses = SkillConfigManager.getUseSetting(hero, this, "max-uses", 15, false);

		String maxUsesString = "";
		if (maxUses > -1)
			maxUsesString = "have a maximum use limit of " + maxUses + ".";
		else
			maxUsesString = "have no usage limit.";

		return getDescription().replace("$1", maxUsesString);
	}

	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.NO_COMBAT_USE.node(), Boolean.valueOf(true));
		node.set(SkillSetting.DELAY.node(), 8000);
		node.set("max-uses", Integer.valueOf(15));

		return node;
	}

	public SkillResult use(Hero hero, String[] args) {
		Player player = hero.getPlayer();
		ItemStack heldItem = player.getInventory().getItemInHand();

		// Check to make sure it is a redstone block
		ItemStack item = player.getItemInHand();
		if (item.getType().name() != "REDSTONE_BLOCK") {
			Messaging.send(player, "You must be holding a Redstone Block in order to imbue Runestones.", new Object[0]);
			return new SkillResult(ResultType.MISSING_REAGENT, false);
		}

		// Read the meta data (if it has any)
		ItemMeta metaData = heldItem.getItemMeta();
		List<String> loreData = metaData.getLore();

		// Ensure that there is no meta-data already on the object
		if (loreData == null) {

			// Get their current location
			Location location = player.getLocation();

			// Validate Residences
			if (residence) {
				ClaimedResidence residence = Residence.getResidenceManager().getByLoc(location);
				if (residence != null) {
					ResidencePermissions perm = residence.getPermissions();
					if (!(perm.playerHas(player.getName(), "build", false))) {
						Messaging.send(player, "You cannot imbue a Runestone within a residence you do not have access to!", new Object[0]);
						return SkillResult.FAIL;
					}
				}
			}

			// Validate Herotowns
			if (herotowns) {
				if (!(ht.getGlobalRegionManager().canBuild(player, location))) {
					Messaging.send(player, "You cannot imbue a Runestone within a town you do not have access to!", new Object[0]);
					return SkillResult.FAIL;
				}
			}

			// Set the first letter of the world name to be upper-case rather than lower case.
			String worldName = location.getWorld().getName();
			worldName = worldName.substring(0, 1).toUpperCase() + worldName.substring(1);

			// Set the Runestone name
			metaData.setDisplayName("�eGigantic Runestone");

			// Set the Lore with all Runestone information
			String locationInformation = "�b" + worldName + ": " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();

			int numUses = SkillConfigManager.getUseSetting(hero, this, "max-uses", 15, false);
			String runestoneUsesInformation = "";
			if (numUses > -1) 			// -1 is unlimited
				runestoneUsesInformation = "�bUses: " + numUses + "/" + numUses;
			else
				runestoneUsesInformation = "�bUses: Unlimited";

			String imbuedByInformation = "�5Imbued by " + player.getDisplayName();
			List<String> newLore = Arrays.asList(locationInformation, runestoneUsesInformation, imbuedByInformation);
			metaData.setLore(newLore);

			// Seperate 1 block from their current stack (if they have one)
			int actualAmount = heldItem.getAmount();
			if (actualAmount > 1) {
				heldItem.setAmount(1);		// Remove the excess blocks.
			}

			// Set the new metaData to the item
			heldItem.setItemMeta(metaData);

			// Play sound
			hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.WITHER_IDLE, 0.5F, 1.0F);

			broadcastExecuteText(hero);

			if (actualAmount > 1) {
				// We need to return their excess blocks to them.
				PlayerInventory inventory = player.getInventory();

				HashMap<Integer, ItemStack> leftOvers = inventory.addItem(new ItemStack[] { new ItemStack(Material.REDSTONE_BLOCK, actualAmount - 1) });
				if (!leftOvers.isEmpty()) {
					for (ItemStack leftOver : leftOvers.values()) {
						player.getWorld().dropItemNaturally(player.getLocation(), leftOver);
					}
					Messaging.send(player, "Items have been dropped at your feet!", new Object[0]);
				}
			}

			return SkillResult.NORMAL;
		}
		else {
			Messaging.send(player, "You cannot imbue a Rune to that item!", new Object[0]);
			return SkillResult.FAIL;
		}
	}
}