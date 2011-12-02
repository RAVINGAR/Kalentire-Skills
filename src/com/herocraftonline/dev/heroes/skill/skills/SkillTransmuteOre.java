package com.herocraftonline.dev.heroes.skill.skills;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.api.SkillResult.ResultType;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.SkillType;
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
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("COAL.product", "IRON_ORE");
        node.set("COAL." + Setting.REAGENT_COST.node(), 5);
        node.set("COAL." + Setting.LEVEL.node(), 1);
        node.set("IRON_ORE.product", "GOLD_ORE");
        node.set("IRON_ORE." + Setting.REAGENT_COST.node(), 3);
        node.set("IRON_ORE." + Setting.LEVEL.node(), 1);
        node.set("LAPIS_BLOCK.product", "DIAMOND");
        node.set("LAPIS_BLOCK." + Setting.REAGENT_COST.node(), 1);
        node.set("LAPIS_BLOCK." + Setting.LEVEL.node(), 1);
        node.set("require-furnace", false);
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
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        
        int level = getSetting(hero, itemName + "." + Setting.LEVEL.node(), 1, true);
        if (hero.getLevel(this) < level) {
        	return new SkillResult(ResultType.LOW_LEVEL, true, level);
        }
        
        int cost = getSetting(hero, itemName + "." + Setting.REAGENT_COST.node(), 1, true);
        if (item.getAmount() < cost) {
            Messaging.send(player, "You need to be holding $1 of $2 to transmute.", cost, itemName);
            return new SkillResult(ResultType.MISSING_REAGENT, false);
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
