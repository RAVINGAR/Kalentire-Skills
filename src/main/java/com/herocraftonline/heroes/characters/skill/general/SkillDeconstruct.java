package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.SkillResult.ResultType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.classes.HeroClass.ExperienceType;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class SkillDeconstruct extends ActiveSkill {

    public SkillDeconstruct(Heroes plugin) {
        super(plugin, "Deconstruct");
        setDescription("Deconstructs the object you are holding");
        setUsage("/skill deconstruct <list|info|item>");
        setArgumentRange(0, 2);
        setIdentifiers("skill deconstruct", "skill dstruct", "skill decon");
        setTypes(SkillType.ITEM_DESTRUCTION, SkillType.ITEM_CREATION, SkillType.UNBINDABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        String root = "IRON_AXE";
        node.set("require-workbench", true);
        node.set(root + "." + SkillSetting.LEVEL.node(), 1);
        node.set(root + "." + SkillSetting.EXP.node(), 0);
        node.set(root + ".min-durability", .5); // Minimum durability percentage the item must have to deconstruct
        node.set(root + ".IRON_INGOT", 1);
        node.set(root + ".STICK", 1);
        node.set(SkillSetting.USE_TEXT.node(), "%hero% has deconstructed a %item%");

        return node;
    }

    @Override
    public void init() {
        super.init();
        setUseText(SkillConfigManager.getRaw(this, SkillSetting.USE_TEXT, "%hero% has deconstructed a %item%"));
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        Set<String> items = new HashSet<>(SkillConfigManager.getUseSettingKeys(hero, this));
        items.remove("require-workbench");
        for (SkillSetting set : SkillSetting.values()) {
            items.remove(set.node());
        }
        int slot = -1;

        ItemStack item = null;
        if (args.length > 0) {

            if (args[0].toLowerCase().equals("list")) {
                List<String> itemList = new ArrayList<>(items);
                int totalPages = itemList.size() / 10;
                if (totalPages % 10 == 0) {
                    totalPages++;
                }
                int page = 0;
                if (args.length > 1) {
                    try {
                        page = Integer.parseInt(args[1]);
                        if (page > totalPages || page < 0) {
                            page = 0;
                        }
                    } catch (NumberFormatException ignored) {}
                }
                int start = page * 10;
                int end = (page + 1) * 10;
                if (end > itemList.size()) {
                    end = itemList.size();
                }
                player.sendMessage(ChatColor.DARK_AQUA + "You can deconstruct these items at the level listed: ");
                for (; start < end; start++) {
                    String name = itemList.get(start);
                    player.sendMessage(ChatColor.GOLD + name + ChatColor.GRAY + " - " + ChatColor.AQUA + SkillConfigManager.getUseSetting(hero, this, name + "." + SkillSetting.LEVEL.node(), 1, true));
                }

                return SkillResult.SKIP_POST_USAGE;
            } else if (args[0].toLowerCase().equals("info")) {
                // Usage Checks if the player passed in arguments
                if (args.length < 2) {
                    player.sendMessage("Proper usage is /skill deconstruct info item");
                    return SkillResult.FAIL;
                } else if (!items.contains(args[1].toUpperCase())) {
                    player.sendMessage("You can't deconstruct that item!");
                    return SkillResult.INVALID_TARGET_NO_MSG;
                } else {
                    // Iterate over the deconstruct recipe and get all the items/amounts it turns into
                    player.sendMessage(args[1] + " deconstructs into the following items: ");
                    for (String s : SkillConfigManager.getUseSettingKeys(hero, this, args[1])) {
                        if (s.equals("min-durability") || s.equals(SkillSetting.LEVEL.node()) || s.equals(SkillSetting.EXP.node())) {
                            continue;
                        }

                        int amount = SkillConfigManager.getUseSetting(hero, this, args[1] + "." + s, 1, false);
                        player.sendMessage(s.toLowerCase().replace("_", " ") + ": " + amount);
                    }

                    return SkillResult.SKIP_POST_USAGE;
                }
            } else if (items.contains(args[0].toUpperCase())) {
                item = new ItemStack(Material.matchMaterial(args[0].toUpperCase()), 1);
                if (!player.getInventory().contains(item.getType(), 1)) {
                    return new SkillResult(ResultType.MISSING_REAGENT, true, 1, item.getType().name().toLowerCase().replace("_", " "));
                }
            }
            if (item == null) {
                player.sendMessage("Invalid item to deconstruct, or bad command!");
                return SkillResult.INVALID_TARGET_NO_MSG;
            }
        } else {
            // if no args attempt to deconstruct item in hand
            item = NMSHandler.getInterface().getItemInMainHand(player.getInventory());
            if (item == null || item.getType() == Material.AIR) {
                player.sendMessage("Invalid item to deconstruct, or bad command!");
                return SkillResult.INVALID_TARGET_NO_MSG;
            }
            item = item.clone();
            item.setAmount(1);
            slot = player.getInventory().getHeldItemSlot();
        }
        Block block = player.getTargetBlock((HashSet<Material>) null, 3);
        Location expLoc = block.getLocation();
        if (SkillConfigManager.getUseSetting(hero, this, "require-workbench", true) && block.getType() != Material.CRAFTING_TABLE) {
            player.sendMessage("You must have a workbench targeted to deconstruct an item!");
            return SkillResult.FAIL;
        }

        if (item.getType() == Material.AIR) {
            player.sendMessage("You must be holding the item you wish to deconstruct!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        String matName = item.getType().name();
        if (!items.contains(matName)) {
            player.sendMessage("You don't know how to deconstruct " + matName);
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        int level = SkillConfigManager.getUseSetting(hero, this, matName + "." + SkillSetting.LEVEL, 1, true);
        if (level > hero.getHeroLevel(this)) {
            player.sendMessage("You must be level " + level + " to deconstruct that item!");
            return new SkillResult(ResultType.LOW_LEVEL, false);
        }
        double minDurability = 0;
        if (item.getType().getMaxDurability() > 16) {
            minDurability = item.getType().getMaxDurability() * (1D - SkillConfigManager.getUseSetting(hero, this, matName + ".min-durability", .5, true));
        }

        if (slot == -1) {
            ItemStack[] contents = player.getInventory().getContents();
            for (int i = 0; i < contents.length; i++) {
                if (contents[i].getType() != item.getType()) {
                    continue;
                } else if (contents[i].getType().getMaxDurability() > 0 && contents[i].getDurability() > minDurability) {
                    continue;
                } else if (contents[i].getType().getMaxDurability() > 0 && slot != -1 && contents[i].getDurability() <= player.getInventory().getContents()[slot].getDurability()) {
                    continue;
                }
                slot = i;
            }
        }
        if (slot == -1) {
            player.sendMessage("That item does not have enough durability remaining!");
            return SkillResult.FAIL;
        }

        Set<String> returned = SkillConfigManager.getUseSettingKeys(hero, this, matName);
        if (returned.isEmpty()) {
            player.sendMessage("There was an error attempting to deconstruct that item!");
            return SkillResult.FAIL;
        }

        for (String s : returned) {
            if (s.equals("min-durability") || s.equals(SkillSetting.LEVEL.node()) || s.equals(SkillSetting.EXP.node())) {
                continue;
            }

            Material m = Material.matchMaterial(s);
            if (m == null) {
                throw new IllegalArgumentException("Error with skill " + getName() + ": bad item definition " + s);
            }
            int amount = SkillConfigManager.getUseSetting(hero, this, matName + "." + s, 1, false);
            if (amount < 1) {
                throw new IllegalArgumentException("Error with skill " + getName() + ": bad amount definition for " + s + ": " + amount);
            }

            ItemStack stack = new ItemStack(m, amount);
            Map<Integer, ItemStack> leftOvers = player.getInventory().addItem(stack);
            // Drop any leftovers we couldn't add to the players inventory
            if (!leftOvers.isEmpty()) {
                for (ItemStack leftOver : leftOvers.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftOver);
                }
                player.sendMessage("Items have been dropped at your feet!");
            }
        }
        int amount = player.getInventory().getContents()[slot].getAmount() - 1;
        if (amount == 0) {
            player.getInventory().clear(slot);
        } else {
            player.getInventory().getContents()[slot].setAmount(amount);
        }
        Util.syncInventory(player, plugin);

        // Grant the hero experience
        int xp = SkillConfigManager.getUseSetting(hero, this, matName + "." + SkillSetting.EXP, 0, false);
        hero.gainExp(xp, ExperienceType.CRAFTING, expLoc);

        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.BLOCK_ANVIL_USE , 0.6F, 1.0F);
        broadcast(player.getLocation(), getUseText().replace("%hero%", player.getName()).replace("%item%", matName.toLowerCase().replace("_", " ")));
        return SkillResult.NORMAL;
    }
}
