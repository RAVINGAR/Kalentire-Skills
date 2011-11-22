package com.herocraftonline.dev.heroes.skill.skills;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.config.ConfigurationNode;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.api.SkillResult.ResultType;
import com.herocraftonline.dev.heroes.classes.HeroClass.ExperienceType;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.herocraftonline.dev.heroes.util.Setting;
import com.herocraftonline.dev.heroes.util.Util;

public class SkillConstruct extends ActiveSkill {

    public SkillConstruct(Heroes plugin) {
        super(plugin, "Construct");
        setDescription("Constructs an object from materials");
        setUsage("/skill construct <item|list|info>");
        setArgumentRange(1, 2);
        setIdentifiers("skill construct");
        setTypes(SkillType.ITEM);
    }

    @Override
    public ConfigurationNode getDefaultConfig() {
        ConfigurationNode node = super.getDefaultConfig();
        String root = "IRON_AXE";
        node.setProperty("require-workbench", true);
        node.setProperty(root + "." + Setting.AMOUNT.node(), 1);
        node.setProperty(root + "." + Setting.LEVEL.node(), 1);
        node.setProperty(root + "." + Setting.EXP.node(), 0);
        node.setProperty(root + ".IRON_INGOT", 1);
        node.setProperty(root + ".STICK", 1);
        node.setProperty(Setting.USE_TEXT.node(), "%hero% has constructed a %item%");
        return node;
    }

    @Override
    public void init() {
        super.init();
        setUseText(getSetting(null, Setting.USE_TEXT.node(), "%hero% has constructed a %item%").replace("%hero%", "$1").replace("%item%", "$2"));
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        // List all items this hero can make with construct
        Set<String> itemSet = new HashSet<String>(getSettingKeys(hero));
        itemSet.remove("require-workbench");
        for (Setting set : Setting.values()) {
            itemSet.remove(set.node());
        }

        if (args[0].toLowerCase().equals("list")) {
            Messaging.send(player, "You can craft these items: " + itemSet.toString().replace("[", "").replace("]", ""));
            return SkillResult.FAIL;
        } else if (args[0].toLowerCase().equals("info")) {
            // Usage Checks if the player passed in arguments
            if (args.length < 2) {
                Messaging.send(player, "Proper usage is /skill construct info item");
                return SkillResult.FAIL;
            } else if (!itemSet.contains(args[1])) {
                Messaging.send(player, "You can't construct that item!");
                return SkillResult.FAIL;
            } else {
                // Iterate over the construct recipe and get all the items/amounts it turns into
                Messaging.send(player, args[1] + " requires the following items to craft: ");
                for (String s : getSettingKeys(hero, args[1])) {
                    if (s.equals(Setting.LEVEL.node()) || s.equals(Setting.EXP.node()) || s.equals(Setting.AMOUNT.node())) {
                        continue;
                    }

                    int amount = getSetting(hero, args[1] + "." + s, 1, false);
                    Messaging.send(player, s.toLowerCase().replace("_", " ") + ": " + amount);
                }
                return SkillResult.SKIP_POST_USAGE;
            }
        }

        if (player.getTargetBlock((HashSet<Byte>) null, 3).getType() != Material.WORKBENCH && getSetting(hero, "require-workbench", true)) {
            Messaging.send(player, "You must have a workbench targetted to construct an item!");
            return SkillResult.FAIL;
        }

        if (player.getInventory().firstEmpty() == -1) {
            Messaging.send(player, "You need at least 1 free inventory spot to construct an item!");
            return SkillResult.FAIL;
        }

        String matName = args[0];
        if (!getSettingKeys(hero).contains(matName)) {
            Messaging.send(player, "You can't construct that item!");
            return SkillResult.FAIL;
        }

        int level = getSetting(hero, matName + "." + Setting.LEVEL.node(), 1, true);
        if (level > hero.getLevel(this)) {
            Messaging.send(player, "You must be level " + level + " to construct that item!");
            return new SkillResult(ResultType.LOW_LEVEL, false);
        }

        Material mat = Material.matchMaterial(matName);
        if (mat == null)
            throw new IllegalArgumentException("Invalid Material definition for skill construct: " + matName);

        List<String> returned = getSettingKeys(hero, matName);
        if (returned == null) {
            Messaging.send(player, "Unable to construct that item!");
            return SkillResult.FAIL;
        }

        List<ItemStack> items = new ArrayList<ItemStack>();
        for (String s : returned) {
            if (s.equals(Setting.LEVEL.node()) || s.equals(Setting.EXP.node()) || s.equals(Setting.AMOUNT.node())) {
                continue;
            }

            Material m = Material.matchMaterial(s);
            if (m == null)
                throw new IllegalArgumentException("Error with skill " + getName() + ": bad item definition " + s);
            int amount = getSetting(hero, matName + "." + s, 1, true);
            if (amount < 1)
                throw new IllegalArgumentException("Error with skill " + getName() + ": bad amount definition for " + s + ": " + amount);

            ItemStack stack = new ItemStack(m, amount);
            if (!hasReagentCost(player, stack)) {
                return new SkillResult(ResultType.MISSING_REAGENT, true, amount, s);
            }
            items.add(stack);
        }
        // Remove the item costs from the player
        player.getInventory().removeItem(items.toArray(new ItemStack[0]));
        int amount = getSetting(hero, matName + "." + Setting.AMOUNT.node(), 1, false);
        Map<Integer, ItemStack> leftOvers = player.getInventory().addItem(normalizeItemStack(mat, amount));

        // Drop any leftovers we couldn't add to the players inventory
        if (!leftOvers.isEmpty()) {
            for (ItemStack leftOver : leftOvers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftOver);
            }
            Messaging.send(player, "Items have been dropped at your feet!");
        }
        Util.syncInventory(player, plugin);

        // Give/Take experience from the hero
        int xp = getSetting(hero, matName + "." + Setting.EXP.node(), 0, false);
        hero.gainExp(xp, ExperienceType.CRAFTING);

        broadcast(player.getLocation(), getUseText(), player.getDisplayName(), matName.toLowerCase().replace("_", " "));
        return SkillResult.NORMAL;
    }

    private ItemStack[] normalizeItemStack(Material mat, int amount) {
        List<ItemStack> items = new ArrayList<ItemStack>();
        while (amount > 0) {
            if (amount > mat.getMaxStackSize()) {
                items.add(new ItemStack(mat, mat.getMaxStackSize()));
                amount -= mat.getMaxStackSize();
            } else {
                items.add(new ItemStack(mat, amount));
                amount = 0;
            }
        }
        return items.toArray(new ItemStack[0]);
    }
}
