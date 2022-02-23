package com.herocraftonline.heroes.characters.skill.pack2;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;

public class SkillRechargeRunestone extends ActiveSkill {
    public SkillRechargeRunestone(Heroes plugin) {
        super(plugin, "RechargeRunestone");
        setDescription("Recharge your held Runestone to refill it's uses.");
        setUsage("/skill rechargerunestone");
        setArgumentRange(0, 0);
        setIdentifiers("skill rechargerunestone");
        setTypes(SkillType.ITEM_MODIFYING, SkillType.SILENCEABLE);
    }

    public String getDescription(Hero hero) {

        return getDescription();
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.NO_COMBAT_USE.node(), true);
        node.set(SkillSetting.DELAY.node(), 8000);

        return node;
    }

    public SkillResult use(Hero hero, String[] args) {

        Player player = hero.getPlayer();
        ItemStack heldItem = player.getInventory().getItemInHand();

        // Check to make sure it is a redstone block
        if (heldItem.getType() != Material.REDSTONE_BLOCK) {
            return SkillResult.INVALID_TARGET;
        }

        ItemStack oldHeldItem = heldItem.clone();

        // Read the meta data (if it has any)
        ItemMeta metaData = heldItem.getItemMeta();
        List<String> loreData = metaData.getLore();

        // Ensure that we actually have meta data
        if (loreData != null) {

            // Ensure that there is at least three rows of meta-data available on the block
            if (loreData.size() > 2) {

                // Get the uses on the Runestone and ensure it is greater than 1
                String usesString = loreData.get(1);
                usesString = usesString.toLowerCase();

                // Strip the usesString of the "Uses :" text.
                int currentIndexLocation = usesString.indexOf(":", 0) + 2;  // Set the start point
                int endIndexLocation = usesString.length();                 // Get the end point for grabbing remaining uses data
                String currentUsesString = usesString.substring(currentIndexLocation, endIndexLocation);

                // Strip the currentUsesString of the "max uses" text.
                currentIndexLocation = 0;
                endIndexLocation = currentUsesString.indexOf("/", 0);
                if (endIndexLocation != -1) {
                    // There is a possible maximum uses located within the usesString
                    currentUsesString = currentUsesString.substring(currentIndexLocation, endIndexLocation);
                }

                if (validateUsesValue(currentUsesString)) {
                    // We have a valid value for "uses". It is either a number, or "unlimited"

                    int uses = -1;
                    if (!currentUsesString.equals("unlimited"))
                        uses = Integer.parseInt(currentUsesString);    // Grab the uses from the string

                    if (uses == -1) {
                        // Runestone has unlimited uses, no need to recharge.
                        player.sendMessage("Runestone has unlimited uses and does not need to be recharged.");
                        return SkillResult.FAIL;
                    }

                    // Get the max uses of the usesString
                    currentIndexLocation = usesString.indexOf("/", 0);
                    if (currentIndexLocation != 0) {

                        // There is a possible maximum uses located within the usesString
                        currentIndexLocation++;
                        endIndexLocation = usesString.length();
                        String maxUsesString = usesString.substring(currentIndexLocation, endIndexLocation);

                        if (validateUsesValue(maxUsesString)) {

                            int maxUses = -1;
                            if (!maxUsesString.equals("unlimited"))
                                maxUses = Integer.parseInt(maxUsesString);    // Grab the uses from the string

                            if (maxUses == -1) {
                                // Somehow we're detecting "unlimited" for max uses. This shouldn't ever happen, but just in case...
                                player.sendMessage("Runestone has unlimited uses and does not need to be recharged.");
                                return SkillResult.FAIL;
                            }

                            // Seperate 1 block from their current stack (if they have one)
                            int actualAmount = heldItem.getAmount();
                            if (actualAmount > 1) {
                                heldItem.setAmount(1);      // Remove the excess blocks.
                            }

                            // We have a valid max uses value, let's set it.

                            // Recharge the runestone
                            loreData.set(1, ChatColor.AQUA + "Uses: " + maxUses + "/" + maxUses);
                            metaData.setLore(loreData);
                            heldItem.setItemMeta(metaData);

                            // Play Effects
                            //Util.playClientEffect(player, "enchantmenttable", new Vector(0, 0, 0), 1F, 10, true);
                            player.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, player.getEyeLocation(), 10, 0.5, 0.3, 0.5, 1F);
                            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.5F, 1.0F);

                            broadcastExecuteText(hero);

                            // We need to return their excess blocks to them.
                            if (actualAmount > 1) {
                                PlayerInventory inventory = player.getInventory();

                                oldHeldItem.setAmount(oldHeldItem.getAmount() - 1);
                                HashMap<Integer, ItemStack> leftOvers = inventory.addItem(oldHeldItem);
                                if (!leftOvers.isEmpty()) {
                                    for (ItemStack leftOver : leftOvers.values()) {
                                        player.getWorld().dropItemNaturally(player.getLocation(), leftOver);
                                    }
                                    player.sendMessage("Items have been dropped at your feet!");
                                }
                            }

                            return SkillResult.NORMAL;
                        }
                        else {
                            player.sendMessage("Could not determine the maximum use limit for this Runestone. It has not been recharged.");
                            return SkillResult.FAIL;
                        }
                    }
                    else {
                        player.sendMessage("ERROR: INVALID USES. NOT UNLIMITED BUT CANNOT DETECT MAXIMUM USE VALUE.");
                        return SkillResult.FAIL;
                    }
                }
                else {
                    player.sendMessage("ERROR: INVALID USES");
                    return SkillResult.FAIL;
                }
            }
            else {
                player.sendMessage("ERROR: NOT ENOUGH LOREDATA");
                return SkillResult.FAIL;
            }
        }
        else {
            player.sendMessage("This is not a Runestone. You can only recharge Runestone tables!");
            return SkillResult.FAIL;
        }
    }

    public boolean validateUsesValue(String uses) {
        if (uses.equals("unlimited")) {
            return true;
        }

        try {
            Integer.parseInt(uses);
            return true;
        }
        catch (Exception ex) {
            return false;
        }
    }
}