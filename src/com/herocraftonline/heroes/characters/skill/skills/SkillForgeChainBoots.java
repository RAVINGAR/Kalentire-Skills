package com.herocraftonline.heroes.characters.skill.skills;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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

public class SkillForgeChainBoots extends ActiveSkill {
    public SkillForgeChainBoots(Heroes plugin) {
        super(plugin, "ForgeChainBoots");
        setDescription("You forge chain boots!");
        setUsage("/skill forgechainchest");
        setArgumentRange(0, 0);
        setIdentifiers("skill forgechainboots");
        setTypes(SkillType.ITEM_CREATION);
    }

    public String getDescription(Hero hero) {
        int amount = SkillConfigManager.getUseSetting(hero, this, SkillSetting.AMOUNT, 2, false);
        return getDescription().replace("$1", amount + "");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.AMOUNT.node(), Integer.valueOf(1));

        return node;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        ItemStack forgedItem = new ItemStack(Material.CHAINMAIL_BOOTS, SkillConfigManager.getUseSetting(hero, this, "amount", 1, false));
        ItemMeta metaData = forgedItem.getItemMeta();

        // Add the "Forged by" message to the item.
        String imbuedByInformation = ChatColor.DARK_PURPLE + "Forged by " + player.getDisplayName();
        List<String> newLore = Arrays.asList(imbuedByInformation);
        metaData.setLore(newLore);

        // Set the new metaData to the item
        forgedItem.setItemMeta(metaData);

        // Add the item to their inventory, but only if they have space.
        PlayerInventory inventory = player.getInventory();
        HashMap<Integer, ItemStack> leftOvers = inventory.addItem(forgedItem);
        for (java.util.Map.Entry<Integer, ItemStack> entry : leftOvers.entrySet()) {
            player.getWorld().dropItemNaturally(player.getLocation(), entry.getValue());
            Messaging.send(player, "Items have been dropped at your feet!", new Object[0]);
        }

        broadcastExecuteText(hero);

        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ANVIL_USE, 0.6F, 1.0F);

        return SkillResult.NORMAL;
    }
}