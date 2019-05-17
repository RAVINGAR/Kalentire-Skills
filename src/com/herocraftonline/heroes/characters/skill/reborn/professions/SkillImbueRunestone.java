package com.herocraftonline.heroes.characters.skill.reborn.professions;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Util;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SkillImbueRunestone extends ActiveSkill {
    public SkillImbueRunestone(Heroes plugin) {
        super(plugin, "ImbueRunestone");
        setDescription("Imbue your held Runestone with a unique message or note!");
        setUsage("/skill imbuerunestone <Text>");
        setArgumentRange(1, 99);
        setIdentifiers("skill imbuerunestone");
        setTypes(SkillType.ITEM_MODIFYING, SkillType.SILENCEABLE, SkillType.UNBINDABLE);
    }

    public String getDescription(Hero hero) {

        return getDescription();
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.NO_COMBAT_USE.node(), true);
        node.set("max-message-length", 40);

        return node;
    }

    public SkillResult use(Hero hero, String[] text) {

        Player player = hero.getPlayer();

        if (text.length == 0) {
            player.sendMessage("/skill imbuerunestone <Text>");
            return SkillResult.FAIL;
        }

        // Merge the arguments into one string
        String textString = ChatColor.WHITE + StringUtils.join(text, " ");

        // Ensure the string is no longer than the specified maximum number of characters
        int maxTextLength = SkillConfigManager.getUseSetting(hero, this, "max-message-length", 40, false);
        if (textString.length() > (maxTextLength + 2))        // Add 2 more characters to the check for color encoding
        {
            player.sendMessage("You cannot imbue a message or note that is longer than " + maxTextLength + " characters.");
            return SkillResult.FAIL;
        }

        if (NMSHandler.getInterface().getItemInMainHand(player.getInventory()) == null) {
            player.sendMessage("You must be holding an item in order to use this skill.");
            return SkillResult.FAIL;
        }

        ItemStack heldItem = player.getInventory().getItemInHand();
        ItemStack oldHeldItem = heldItem.clone();

        // Check to make sure it is a redstone block
        ItemStack item = NMSHandler.getInterface().getItemInMainHand(player.getInventory());
        if (!item.getType().name().equals("REDSTONE_BLOCK")) {
            player.sendMessage("You must be holding a Runestone Block to use this skill.");
            return SkillResult.INVALID_TARGET;
        }

        // Read the meta data (if it has any)
        ItemMeta metaData = heldItem.getItemMeta();
        List<String> loreData = metaData.getLore();

        // Ensure that we actually have meta data
        if (loreData != null) {

            // Ensure that there is at least three rows of meta-data available on the block
            if (loreData.size() > 2) {

                // We have a valid runestone block

                // Check to see if we need to add a string to the loredata, or change the already existing one.
                if (loreData.size() == 4) {
                    // Runestone already has an imbued message. Change it.
                    loreData.set(3, textString);
                } else {
                    // Runestone does not have any imbued message yet. Lets Add the new one.
                    loreData.add(textString);
                }

                // Update the item's meta data
                List<String> newLore = Arrays.asList(loreData.get(0), loreData.get(1), loreData.get(2), loreData.get(3));
                metaData.setLore(newLore);

                // Seperate 1 block from their current stack (if they have one)
                int actualAmount = heldItem.getAmount();
                if (actualAmount > 1) {
                    heldItem.setAmount(1);        // Remove the excess blocks.
                }

                // Set the new metaData to the item
                heldItem.setItemMeta(metaData);

                // Play Effects
                Util.playClientEffect(player, "enchantmenttable", new Vector(0, 0, 0), 1F, 10, true);
                player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_WITHER_AMBIENT.value(), 0.5F, 1.0F);

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
            } else {
                player.sendMessage("You must be holding a Runestone Block to use this skill.");
                return SkillResult.FAIL;
            }
        } else {
            player.sendMessage("You must be holding a Runestone Block to use this skill.");
            return SkillResult.FAIL;
        }
    }
}