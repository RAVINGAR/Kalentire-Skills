package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;

public class SkillBaseForgeItem extends ActiveSkill {

    protected int defaultAmount;
    protected Material deafultItem;

    public SkillBaseForgeItem(Heroes plugin, String name) {
        super(plugin, name);
    }

    public SkillBaseForgeItem(Heroes plugin) {
        this(plugin, "ForgeItem");
        setDescription("You forge an item!");
        setUsage("/skill forgeitem");
        setArgumentRange(0, 0);
        setIdentifiers("skill forgeitem");
        setTypes(SkillType.ITEM_CREATION);
        defaultAmount = 1;
        deafultItem = Material.LEATHER_BOOTS;
    }

    @Override
    public String getDescription(Hero hero) {
        int amount = SkillConfigManager.getUseSetting(hero, this, SkillSetting.AMOUNT, defaultAmount, false);
        return getDescription().replace("$1", amount + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.AMOUNT.node(), 1);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        ItemStack forgedItem = new ItemStack(deafultItem, SkillConfigManager.getUseSetting(hero, this, "amount", 1, false));
        ItemMeta metaData = forgedItem.getItemMeta();
        /*
        // Add the "Forged by" message to the item.
        String imbuedByInformation = ChatColor.DARK_PURPLE + "Forged by " + player.getName();
        List<String> newLore = Arrays.asList(imbuedByInformation);
        metaData.setLore(newLore);

        // Set the new metaData to the item
        forgedItem.setItemMeta(metaData);
        */

        // Add the item to their inventory, but only if they have space.
        PlayerInventory inventory = player.getInventory();
        HashMap<Integer, ItemStack> leftOvers = inventory.addItem(forgedItem);
        for (java.util.Map.Entry<Integer, ItemStack> entry : leftOvers.entrySet()) {
            player.getWorld().dropItemNaturally(player.getLocation(), entry.getValue());
            Messaging.send(player, "Items have been dropped at your feet!");
        }

        broadcastExecuteText(hero);

        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ANVIL_USE, 0.6F, 1.0F);

        return SkillResult.NORMAL;
    }
}