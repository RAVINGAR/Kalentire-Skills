package com.herocraftonline.heroes.characters.skill.skills;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;

public class SkillImbueRunestone extends ActiveSkill {
    public SkillImbueRunestone(Heroes plugin) {
        super(plugin, "ImbueRunestone");
        setDescription("Imbue your held Runestone with a unique message or note!");
        setUsage("/skill imbuerunestone <Text>");
        setArgumentRange(1, 99);
        setIdentifiers("skill imbuerunestone");
        setTypes(SkillType.ITEM, SkillType.SILENCABLE, SkillType.UNBINDABLE);
    }

    public String getDescription(Hero hero) {

        return getDescription();
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.NO_COMBAT_USE.node(), Boolean.valueOf(true));
        node.set("max-message-length", Integer.valueOf(40));

        return node;
    }

    public SkillResult use(Hero hero, String[] text) {

        Player player = hero.getPlayer();

        if (text.length == 0) {
            Messaging.send(player, "/skill imbuerunestone <Text>", new Object[0]);
            return SkillResult.FAIL;
        }

        // Merge the arguments into one string
        String textString = ChatColor.WHITE + StringUtils.join(text, " ");

        // Ensure the string is no longer than the specified maximum number of characters
        int maxTextLength = SkillConfigManager.getUseSetting(hero, this, "max-message-length", 40, false);
        if (textString.length() > (maxTextLength + 2))		// Add 2 more characters to the check for color encoding
        {
            Messaging.send(player, "You cannot imbue a message or note that is longer than " + maxTextLength + " characters.", new Object[0]);
            return SkillResult.FAIL;
        }

        if (player.getItemInHand() == null) {
            Messaging.send(player, "You must be holding an item in order to use this skill.", new Object[0]);
            return SkillResult.FAIL;
        }

        ItemStack heldItem = player.getInventory().getItemInHand();

        // Check to make sure it is a redstone block
        ItemStack item = player.getItemInHand();
        if (item.getType().name() != "REDSTONE_BLOCK") {
            Messaging.send(player, "You must be holding a Runestone Block to use this skill.", new Object[0]);
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
                }
                else {
                    // Runestone does not have any imbued message yet. Lets Add the new one.
                    loreData.add(textString);
                }

                // Update the item's meta data
                List<String> newLore = Arrays.asList(loreData.get(0), loreData.get(1), loreData.get(2), loreData.get(3));
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
                Messaging.send(player, "You must be holding a Runestone Block to use this skill.", new Object[0]);
                return SkillResult.FAIL;
            }
        }
        else {
            Messaging.send(player, "You must be holding a Runestone Block to use this skill.", new Object[0]);
            return SkillResult.FAIL;
        }
    }
}