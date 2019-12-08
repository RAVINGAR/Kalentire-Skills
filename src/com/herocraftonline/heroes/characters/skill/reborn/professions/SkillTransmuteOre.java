package com.herocraftonline.heroes.characters.skill.reborn.professions;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.SkillResult.ResultType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;
import java.util.logging.Level;

public class SkillTransmuteOre extends ActiveSkill {

    public SkillTransmuteOre(Heroes plugin) {
        super(plugin, "TransmuteOre");
        setDescription("You can transmute ores into more valuable ones. See /skill transmuteore info");
        setUsage("/skill transmuteore");
        setArgumentRange(0, 1);
        setIdentifiers("skill transmuteore");
        setTypes(SkillType.ABILITY_PROPERTY_EARTH, SkillType.ABILITY_PROPERTY_FIRE);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("COAL.product", "IRON_ORE");
        node.set("COAL." + SkillSetting.REAGENT_COST.node(), 5);
        node.set("COAL." + SkillSetting.LEVEL.node(), 1);
        node.set("IRON_ORE.product", "GOLD_ORE");
        node.set("IRON_ORE." + SkillSetting.REAGENT_COST.node(), 3);
        node.set("IRON_ORE." + SkillSetting.LEVEL.node(), 1);
        node.set("LAPIS_BLOCK.product", "DIAMOND");
        node.set("LAPIS_BLOCK." + SkillSetting.REAGENT_COST.node(), 1);
        node.set("LAPIS_BLOCK." + SkillSetting.LEVEL.node(), 1);
        node.set("require-furnace", false);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        if (args.length == 1 && args[0].equalsIgnoreCase("info")){
            for (String message : buildInfoStringMessages(hero)) {
                player.sendMessage(message);
            }
            return SkillResult.SKIP_POST_USAGE;
        }

        ItemStack item = NMSHandler.getInterface().getItemInMainHand(player.getInventory());

        if (SkillConfigManager.getUseSetting(hero, this, "require-furnace", false) && player.getTargetBlock((HashSet<Material>) null, 3).getType() != Material.FURNACE) {
            player.sendMessage("You must have a furnace targetted to transmute ores!");
            return SkillResult.FAIL;
        }
        // List all items this hero can transmute
        Set<String> itemSet = new HashSet<>(SkillConfigManager.getUseSettingKeys(hero, this));
        itemSet.remove("require-furnace");
        for (SkillSetting set : SkillSetting.values()) {
            itemSet.remove(set.node());
        }

        if (item == null || !itemSet.contains(item.getType().name())) {
            player.sendMessage("You can't transmute that item!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        String itemName = item.getType().name();

        int level = SkillConfigManager.getUseSetting(hero, this, itemName + "." + SkillSetting.LEVEL, 1, true);
        if (hero.getHeroLevel(this) < level) {
            return new SkillResult(ResultType.LOW_LEVEL, true, level);
        }

        int cost = SkillConfigManager.getUseSetting(hero, this, itemName + "." + SkillSetting.REAGENT_COST, 1, true);
        if (item.getAmount() < cost) {
            player.sendMessage("You need to be holding " + cost + " of " + itemName + " to transmute.");
            return new SkillResult(ResultType.MISSING_REAGENT, false);
        }

        Material finished = Material.matchMaterial(SkillConfigManager.getUseSetting(hero, this, itemName + ".product", ""));
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
            player.sendMessage("Items have been dropped at your feet!");
        }
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), CompatSound.ENTITY_PLAYER_LEVELUP.value(), 0.8F, 1.0F);
        Util.syncInventory(player, plugin);
        return SkillResult.NORMAL;
    }


    private List<String> buildInfoStringMessages(Hero hero){
        List<String> messages = new ArrayList<>();
        messages.add(ChatColor.BLUE + "Transmutation Table:");

        // Remove all skill settings apart from those related to the items
        Set<String> keySet = new HashSet<>(SkillConfigManager.getUseSettingKeys(hero, this));
        keySet.remove("require-furnace");
        for (SkillSetting set : SkillSetting.values()) {
            keySet.remove(set.node());
        }

        // Get all valid item names
        List<String> itemNames = new ArrayList<>();
        for (String itemName : keySet) {
            Material itemMaterial = Material.getMaterial(itemName);
            if (itemMaterial != null){
                itemNames.add(itemName);
            } else {
                Heroes.debugLog(Level.WARNING, "\"" + itemName + "\" is an invalid item for transmuting ores.");
            }
        }

        // Sort items by level
        HashMap<Integer, List<String>> itemsByLevelRequired = new HashMap<>();
        for (String itemName : itemNames) {
            int levelRequired = SkillConfigManager.getUseSetting(hero, this, itemName + "." + SkillSetting.LEVEL, 1, true);
            if (itemsByLevelRequired.containsKey(levelRequired)){
                itemsByLevelRequired.get(levelRequired).add(itemName);
            } else {
                List<String> newList = new ArrayList<>(3);
                newList.add(itemName);
                itemsByLevelRequired.put(levelRequired, newList);
            }
        }

        // Build up the messages
        int heroLevel = hero.getHeroLevel(this);
        for (int itemsRequiredLevel : itemsByLevelRequired.keySet()){
            String levelString = "Lvl " + itemsRequiredLevel;
            ChatColor messageColour = heroLevel < itemsRequiredLevel ? ChatColor.RED : ChatColor.GREEN;
            for (String itemName : itemsByLevelRequired.get(itemsRequiredLevel)) {
                String product = SkillConfigManager.getUseSetting(hero, this, itemName + ".product", "");
                int cost = SkillConfigManager.getUseSetting(hero, this, itemName + "." + SkillSetting.REAGENT_COST, 1, true);

                messages.add(messageColour + levelString + ": " + cost + "x" + itemName + " -> " + product);
            }
        }

        if (messages.size() == 1){
            messages.add(ChatColor.RED + "No valid recipes found! Contact dev/admin for assistance.");
        }

        return messages;
    }
}
