package com.herocraftonline.dev.heroes.skill.skills;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.config.ConfigurationNode;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.skill.ActiveSkill.SkillResult;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.herocraftonline.dev.heroes.util.Setting;
import com.herocraftonline.dev.heroes.util.Util;

public class SkillTransmuteOre extends ActiveSkill {

    public SkillTransmuteOre(Heroes plugin) {
        super(plugin, "TransmuteOre");
        setDescription("Transmutes ores into more valuable ones");
        setUsage("/skill transmuteore");
        setArgumentRange(0, 0);
        setIdentifiers("skill transmuteore", "skill xmuteore");
        setTypes(SkillType.EARTH, SkillType.FIRE);
    }

    @Override
    public ConfigurationNode getDefaultConfig() {
        ConfigurationNode node = super.getDefaultConfig();
        node.setProperty("COAL.product", "IRON_ORE");
        node.setProperty("COAL." + Setting.REAGENT_COST.node(), 5);
        node.setProperty("COAL." + Setting.LEVEL.node(), 1);
        node.setProperty("IRON_ORE.product", "GOLD_ORE");
        node.setProperty("IRON_ORE." + Setting.REAGENT_COST.node(), 3);
        node.setProperty("IRON_ORE." + Setting.LEVEL.node(), 1);
        node.setProperty("LAPIS_BLOCK.product", "DIAMOND");
        node.setProperty("LAPIS_BLOCK." + Setting.REAGENT_COST.node(), 1);
        node.setProperty("LAPIS_BLOCK." + Setting.LEVEL.node(), 1);
        node.setProperty("require-furnace", false);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        ItemStack item = player.getItemInHand();
        
        if (getSetting(hero, "require-furnace", false) && player.getTargetBlock((HashSet<Byte>) null, 3).getType() != Material.FURNACE) {
            Messaging.send(player, "You must have a furnace targetted to transmute ores!");
            return SkillResult.FAIL;
        }
        // List all items this hero can transmute
        Set<String> itemSet = new HashSet<String>(getSettingKeys(hero));
        itemSet.remove("require-furnace");
        for (Setting set : Setting.values()) {
            itemSet.remove(set.node());
        }
        
        String itemName = item.getType().name();
        if (item == null || !itemSet.contains(itemName)) {
            Messaging.send(player, "You can't transmute that item!");
            return SkillResult.FAIL;
        }
        
        int level = getSetting(hero, itemName + "." + Setting.LEVEL.node(), 1, true);
        if (hero.getLevel(this) < level) {
            Messaging.send(player, "You must be level $1 to transmute that.", level);
            return SkillResult.FAIL;
        }
        
        int cost = getSetting(hero, itemName + "." + Setting.REAGENT_COST.node(), 1, true);
        if (item.getAmount() < cost) {
            Messaging.send(player, "You need to be holding $1 of $2 to transmute.", cost, itemName);
            return SkillResult.FAIL;
        }
        
        Material finished = Material.matchMaterial(getSetting(hero, itemName + ".product", ""));
        if (finished == null) {
            throw new IllegalArgumentException("Invalid product material defined for TransmuteOre node: " + itemName);
        }
        item.setAmount(item.getAmount() - cost);
        PlayerInventory inventory = player.getInventory();
        if (item.getAmount() == 0) {
            inventory.clear(inventory.getHeldItemSlot());
        }
        
        HashMap<Integer, ItemStack> leftOvers = inventory.addItem(new ItemStack(finished, 1));
        
        if (!leftOvers.isEmpty()) {
            for (ItemStack leftOver : leftOvers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftOver);
            }
            Messaging.send(player, "Items have been dropped at your feet!");
        }
        Util.syncInventory(player, plugin);
        return SkillResult.NORMAL;
    }
}
