package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.SkillResult.ResultType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.MaterialUtil;
import com.herocraftonline.heroes.util.Util;
import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.api.interaction.util.DurabilityItem;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class SkillRepair extends ActiveSkill {
    private boolean usingMMOItems = false;
    private String useText = null;

    private final Map<Material, Integer> repairCostMap;

    public SkillRepair(final Heroes plugin) {
        super(plugin, "Repair");
        setDescription("You are able to repair tools and armor. There is a $1% chance the item will be disenchanted.");
        setUsage("/skill repair <hotbarslot>");
        setArgumentRange(0, 1);
        setIdentifiers("skill repair");
        setTypes(SkillType.ITEM_MODIFYING, SkillType.ABILITY_PROPERTY_PHYSICAL);

        repairCostMap = new HashMap<>();
        if (plugin.getServer().getPluginManager().getPlugin("MMOItems") != null) {
            usingMMOItems = true;
        }


    }

    @Override
    public String getDescription(final Hero hero) {
        double unchant = SkillConfigManager.getUseSetting(hero, this, "unchant-chance", .5, true);
        unchant -= SkillConfigManager.getUseSetting(hero, this, "unchant-chance-reduce", .005, false) * hero.getHeroLevel(this);
        return getDescription().replace("$1", Util.stringDouble(unchant * 100.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.USE_TEXT.node(), "%hero% repaired a %item%%ench%");
        final List<String> list = new ArrayList<>();
        final String[] materialTool = new String[]{"wooden", "stone", "golden", "iron", "diamond", "netherite"};
        final String[] materialArmour = new String[]{"leather", "chainmail", "iron", "golden", "diamond", "netherite"};
        final String[] tool = new String[]{"pickaxe, axe, shovel, hoe"};
        final String[] armour = new String[]{"helmet", "chestplate", "leggings", "boots"};

        for (final String mat : materialTool) {
            for (final String t : tool) {
                final Material material = Material.matchMaterial(mat + "_" + t);
                if (material != null) {
                    list.add(material.name().toLowerCase() + " 1");
                }
            }
        }

        for (final String mat : materialArmour) {
            for (final String t : armour) {
                final Material material = Material.matchMaterial(mat + "_" + t);
                if (material != null) {
                    list.add(material.name().toLowerCase() + " 1");
                }
            }
        }

        list.add("bow 1");
        list.add("shield 1");
        list.add("fishing_rod 1");
        list.add("shears 1");
        list.add("flint_and_steel 1");

        node.set("repair-costs", list);

        node.set("netherite-armor-max-cost", 1);
        node.set("netherite-tool-max-cost", 1);
        node.set("trident", 1);
        node.set("trident-material", "GOLD_INGOT");
        node.set("trident-max-cost", 2);
        node.set("additional-cost-per-enchantment-levels", 1);
        node.set("unchant-chance", .5);
        node.set("unchant-chance-reduce", .005);
        return node;
    }

    @Override
    public void init() {
        super.init();
        useText = SkillConfigManager.getRaw(this, SkillSetting.USE_TEXT, "%hero% repaired a %item%%ench%");

        SkillConfigManager.getRawKeys(this, "repair-costs").forEach(entry -> {
            final String[] split = entry.split(" ");
            if (split.length > 1) {
                final int i = Integer.parseInt(split[1]);
                final Material material = Material.matchMaterial(split[0]);
                if (material != null && i > 0) {
                    repairCostMap.put(material, i);
                }
            }
        });
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();
        final ItemStack is;
        if (args == null || args.length < 1) {
            is = NMSHandler.getInterface().getItemInMainHand(player.getInventory());
        } else {
            final int itemSlotNumber;
            try {
                itemSlotNumber = Integer.parseInt(args[0]);
            } catch (final NumberFormatException e) {
                player.sendMessage("That is not a valid slot number (0-8).");
                return SkillResult.INVALID_TARGET_NO_MSG;
            }

            // Support only hotbar slots
            if (itemSlotNumber > 8) {
                player.sendMessage("That is not a valid hotbar slot number (0-8).");
                return SkillResult.INVALID_TARGET_NO_MSG;
            }

            is = player.getInventory().getItem(itemSlotNumber);
        }
        if (is == null) {
            player.sendMessage("You cannot repair nothing");
            return SkillResult.FAIL;
        }
        final Material isType = is.getType();
        Integer level = repairCostMap.get(isType);
        if (level == null) {
            level = -1;
        }
        final Material reagent = getRequiredReagent(hero, isType);
        final ItemMeta itemMeta = is.getItemMeta();

        if (level == -1 || reagent == null || !(itemMeta instanceof Damageable)) { // note implies itemMeta == null
            player.sendMessage("You are not holding a repairable tool.");
            return SkillResult.FAIL;
        }

        if (hero.getHeroLevel(this) < level) {
            player.sendMessage("You must be level " + level + " to repair " + MaterialUtil.getFriendlyName(isType));
            return new SkillResult(ResultType.LOW_LEVEL, false);
        }
        //if (is.getDurability() == 0) {

        if (((Damageable) itemMeta).getDamage() == 0) {
            player.sendMessage("That item is already at full durability!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        final boolean enchanted = !is.getEnchantments().isEmpty();
        double repairCost = getRepairCost(hero, is);
        if (enchanted) {
            final double additionalCostPerEnchantmentLevels = SkillConfigManager.getUseSetting(hero, this, "additional-cost-per-enchantment-levels", 1.0, true);
            int enchantmentLevels = 0;
            for (final Map.Entry<Enchantment, Integer> entry : is.getEnchantments().entrySet()) {
                final Enchantment enchantment = entry.getKey();
                final Integer enchantmentLevel = entry.getValue();
                enchantmentLevels += enchantmentLevel;
            }
            repairCost += (additionalCostPerEnchantmentLevels * enchantmentLevels);
        }

        ItemStack reagentStack = null;
        if (repairCost > 0) {
            if (reagent == Material.OAK_PLANKS) {
                //Handle all wood variants as a reagent
                final List<Material> woodMaterials = new ArrayList<>();
                woodMaterials.add(Material.OAK_PLANKS);
                woodMaterials.add(Material.BIRCH_PLANKS);
                woodMaterials.add(Material.SPRUCE_PLANKS);
                woodMaterials.add(Material.JUNGLE_PLANKS);
                woodMaterials.add(Material.ACACIA_PLANKS);
                woodMaterials.add(Material.DARK_OAK_PLANKS);
                woodMaterials.add(Material.CRIMSON_PLANKS);
                woodMaterials.add(Material.WARPED_PLANKS);

                boolean hasReagant = false;
                for (final Material woodMaterial : woodMaterials) {
                    reagentStack = new ItemStack(woodMaterial, (int) repairCost);
                    hasReagant = hasReagentCost(player, reagentStack);
                    if (hasReagant) {
                        // Found valid wood reagent that the player has
                        break;
                    }
                }

                if (!hasReagant) {
                    final String planksString = MaterialUtil.getFriendlyName(Material.OAK_PLANKS)
                            + " or " + MaterialUtil.getFriendlyName(Material.BIRCH_PLANKS)
                            + " or " + MaterialUtil.getFriendlyName(Material.SPRUCE_PLANKS)
                            + " or " + MaterialUtil.getFriendlyName(Material.JUNGLE_PLANKS)
                            + " or " + MaterialUtil.getFriendlyName(Material.ACACIA_PLANKS)
                            + " or " + MaterialUtil.getFriendlyName(Material.DARK_OAK_PLANKS)
                            + " or " + MaterialUtil.getFriendlyName(Material.CRIMSON_PLANKS)
                            + " or " + MaterialUtil.getFriendlyName(Material.WARPED_PLANKS);
                    return new SkillResult(ResultType.MISSING_REAGENT, true, repairCost, planksString);
                }
            } else {
                reagentStack = new ItemStack(reagent, (int) repairCost);
                if (!hasReagentCost(player, reagentStack)) {
                    return new SkillResult(ResultType.MISSING_REAGENT, true, reagentStack.getAmount(), MaterialUtil.getFriendlyName(reagentStack.getType()));
                }
            }
        }

        boolean lost = false;
        if (enchanted) {
            double unchant = SkillConfigManager.getUseSetting(hero, this, "unchant-chance", .5, true);
            unchant -= SkillConfigManager.getUseSetting(hero, this, "unchant-chance-reduce", .005, false) * hero.getHeroLevel(this);
            if (Util.nextRand() <= unchant) {
                for (final Enchantment enchant : new ArrayList<>(is.getEnchantments().keySet())) {
                    is.removeEnchantment(enchant);
                }
                lost = true;
            }
        }
        // Old repair approach (doesn't consider custom durability)
        //is.setDurability((short) 0);
//        ((Damageable)itemMeta).setDamage(0); // repair item
//        is.setItemMeta(itemMeta); // apply meta changes

        // Repair item (applies meta changes too)
        if (usingMMOItems && NBTItem.get(is).hasType()) {
            final DurabilityItem durabilityItem = new DurabilityItem(hero.getPlayer(), is);
            durabilityItem.addDurability(durabilityItem.getMaxDurability());
            //Repair currently works in that it will always repair to the max
            final ItemMeta result = durabilityItem.toItem().getItemMeta();
            is.setItemMeta(result);
        } else {
            Util.repairItem(plugin, is, itemMeta);
        }


        if (reagentStack != null) {
            player.getInventory().removeItem(reagentStack);
            Util.syncInventory(player, plugin);
        }
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.BLOCK_ANVIL_USE, 0.6F, 1.0F);
        //hero.getPlayer().getWorld().spigot().playEffect(hero.getPlayer().getLocation().add(0, 0.6, 0), org.bukkit.Effect.ITEM_BREAK, Material.DIAMOND_SWORD.getId(), 0, 0.1F, 0.1F, 0.1F, 0.0F, 15, 16);
        final String message = useText.replace("%hero%", player.getName())
                .replace("%item%", MaterialUtil.getFriendlyName(is.getType()))
                .replace("%ench%", !enchanted ? "." : lost ? " and stripped it of enchantments!" : " and successfully kept the enchantments.");
        broadcast(player.getLocation(), message);
        return SkillResult.NORMAL;
    }

    private int getRepairCost(final Hero hero, final ItemStack item) {
        if (!item.hasItemMeta()) {
            return 0;
        }

        final Material mat = item.getType();

        final double currDurability;
        final double maxDurability;

        if (usingMMOItems && NBTItem.get(item).hasType()) {
            final DurabilityItem dura = new DurabilityItem(hero.getPlayer(), item);
            currDurability = dura.getDurability();
            maxDurability = dura.getMaxDurability();
        } else {
            final Damageable is = (Damageable) (item.getItemMeta());
            maxDurability = mat.getMaxDurability();
            currDurability = maxDurability - is.getDamage();
        }

        final int amt;
        switch (mat) {
            case BOW:
                amt = (int) ((currDurability / maxDurability) * 2.0);
                return Math.max(amt, 1);
            case TRIDENT:
                final int cost = SkillConfigManager.getUseSetting(hero, this, "trident-max-cost", 2, true);
                if (cost <= 0) {
                    return 0;
                }
                amt = (int) ((currDurability / maxDurability) * cost);
                return Math.max(amt, 1);
            case NETHERITE_CHESTPLATE:
            case NETHERITE_LEGGINGS:
            case NETHERITE_BOOTS:
            case NETHERITE_HELMET:
                amt = (int) ((currDurability / maxDurability) * (SkillConfigManager.getUseSetting(hero, this, "netherite-armor-max-cost", 1, true)));
                return Math.max(amt, 1);
            case NETHERITE_SWORD:
            case NETHERITE_AXE:
            case NETHERITE_HOE:
            case NETHERITE_PICKAXE:
            case NETHERITE_SHOVEL:
                amt = (int) ((currDurability / maxDurability) * (SkillConfigManager.getUseSetting(hero, this, "netherite-tool-max-cost", 1, true)));
                return Math.max(amt, 1);
            case LEATHER_BOOTS:
            case IRON_BOOTS:
            case CHAINMAIL_BOOTS:
            case GOLDEN_BOOTS:
            case DIAMOND_BOOTS:
                amt = (int) ((currDurability / maxDurability) * 3.0);
                return Math.max(amt, 1);
            case LEATHER_HELMET:
            case IRON_HELMET:
            case CHAINMAIL_HELMET:
            case GOLDEN_HELMET:
            case DIAMOND_HELMET:
                amt = (int) ((currDurability / maxDurability) * 4.0);
                return Math.max(amt, 1);
            case LEATHER_CHESTPLATE:
            case IRON_CHESTPLATE:
            case CHAINMAIL_CHESTPLATE:
            case GOLDEN_CHESTPLATE:
            case DIAMOND_CHESTPLATE:
                amt = (int) ((currDurability / maxDurability) * 7.0);
                return Math.max(amt, 1);
            case LEATHER_LEGGINGS:
            case IRON_LEGGINGS:
            case CHAINMAIL_LEGGINGS:
            case GOLDEN_LEGGINGS:
            case DIAMOND_LEGGINGS:
                amt = (int) ((currDurability / maxDurability) * 6.0);
                return Math.max(amt, 1);
            default:
                return 1;
        }
    }

    /*
    private int getRequiredLevel(Hero hero, Material material) {
        return switch (material) {
            case WOODEN_SWORD, WOODEN_AXE, BOW, CROSSBOW -> SkillConfigManager.getUseSetting(hero, this, "wood-weapons", 1, true);
            case WOODEN_HOE, WOODEN_PICKAXE, WOODEN_SHOVEL -> SkillConfigManager.getUseSetting(hero, this, "wood-tools", 1, true);
            case STONE_SWORD, STONE_AXE -> SkillConfigManager.getUseSetting(hero, this, "stone-weapons", 1, true);
            case STONE_HOE, STONE_PICKAXE, STONE_SHOVEL -> SkillConfigManager.getUseSetting(hero, this, "stone-tools", 1, true);
            case SHEARS -> SkillConfigManager.getUseSetting(hero, this, "shears", 1, true);
            case FLINT_AND_STEEL -> SkillConfigManager.getUseSetting(hero, this, "flint-steel", 1, true);
            case IRON_CHESTPLATE, IRON_LEGGINGS, IRON_BOOTS, IRON_HELMET -> SkillConfigManager.getUseSetting(hero, this, "iron-armor", 1, true);
            case IRON_SWORD, IRON_AXE -> SkillConfigManager.getUseSetting(hero, this, "iron-weapons", 1, true);
            case IRON_HOE, IRON_PICKAXE, IRON_SHOVEL -> SkillConfigManager.getUseSetting(hero, this, "iron-tools", 1, true);
            case CHAINMAIL_HELMET, CHAINMAIL_CHESTPLATE, CHAINMAIL_BOOTS, CHAINMAIL_LEGGINGS -> SkillConfigManager.getUseSetting(hero, this, "chain-armor", 1, true);
            case GOLDEN_CHESTPLATE, GOLDEN_LEGGINGS, GOLDEN_BOOTS, GOLDEN_HELMET -> SkillConfigManager.getUseSetting(hero, this, "gold-armor", 1, true);
            case GOLDEN_SWORD, GOLDEN_AXE -> SkillConfigManager.getUseSetting(hero, this, "gold-weapons", 1, true);
            case GOLDEN_HOE, GOLDEN_PICKAXE, GOLDEN_SHOVEL -> SkillConfigManager.getUseSetting(hero, this, "gold-tools", 1, true);
            case DIAMOND_CHESTPLATE, DIAMOND_LEGGINGS, DIAMOND_BOOTS, DIAMOND_HELMET -> SkillConfigManager.getUseSetting(hero, this, "diamond-armor", 1, true);
            case DIAMOND_SWORD, DIAMOND_AXE -> SkillConfigManager.getUseSetting(hero, this, "diamond-weapons", 1, true);
            case DIAMOND_HOE, DIAMOND_PICKAXE, DIAMOND_SHOVEL -> SkillConfigManager.getUseSetting(hero, this, "diamond-tools", 1, true);
            case NETHERITE_CHESTPLATE, NETHERITE_LEGGINGS, NETHERITE_BOOTS, NETHERITE_HELMET -> SkillConfigManager.getUseSetting(hero, this, "netherite-armor", 1, true);
            case NETHERITE_SWORD, NETHERITE_AXE -> SkillConfigManager.getUseSetting(hero, this, "netherite-weapons", 1, true);
            case NETHERITE_HOE, NETHERITE_PICKAXE, NETHERITE_SHOVEL -> SkillConfigManager.getUseSetting(hero, this, "netherite-tools", 1, true);
            case LEATHER_BOOTS, LEATHER_CHESTPLATE, LEATHER_HELMET, LEATHER_LEGGINGS -> SkillConfigManager.getUseSetting(hero, this, "leather-armor", 1, true);
            case FISHING_ROD -> SkillConfigManager.getUseSetting(hero, this, "fishing-rod", 1, true);
            case TRIDENT -> SkillConfigManager.getUseSetting(hero, this, "trident", 1, true);
            default -> -1;
        };
    }*/ // I REFUSE TO CONVERT THIS TO JAVA 8 FORMAT. SO DO THE CONFIG YA SELF

    private Material getRequiredReagent(final Hero hero, final Material material) {
        switch (material) {
            case WOODEN_SWORD:
            case WOODEN_AXE:
            case WOODEN_HOE:
            case WOODEN_PICKAXE:
            case WOODEN_SHOVEL:
                // There are 6 types of wooden planks use Oak as main reagent, see above.
                return Material.OAK_PLANKS;
            case STONE_SWORD:
            case STONE_AXE:
            case STONE_HOE:
            case STONE_PICKAXE:
            case STONE_SHOVEL:
                return Material.COBBLESTONE;
            case SHEARS:
            case FLINT_AND_STEEL:
            case IRON_CHESTPLATE:
            case IRON_LEGGINGS:
            case IRON_BOOTS:
            case IRON_HELMET:
            case IRON_SWORD:
            case IRON_AXE:
            case IRON_HOE:
            case IRON_PICKAXE:
            case IRON_SHOVEL:
                return Material.IRON_INGOT;
            case GOLDEN_CHESTPLATE:
            case GOLDEN_LEGGINGS:
            case GOLDEN_BOOTS:
            case GOLDEN_HELMET:
            case GOLDEN_SWORD:
            case GOLDEN_AXE:
            case GOLDEN_HOE:
            case GOLDEN_PICKAXE:
            case GOLDEN_SHOVEL:
                return Material.GOLD_INGOT;
            case DIAMOND_CHESTPLATE:
            case DIAMOND_LEGGINGS:
            case DIAMOND_BOOTS:
            case DIAMOND_HELMET:
            case DIAMOND_SWORD:
            case DIAMOND_AXE:
            case DIAMOND_HOE:
            case DIAMOND_PICKAXE:
            case DIAMOND_SHOVEL:
                return Material.DIAMOND;
            case NETHERITE_CHESTPLATE:
            case NETHERITE_LEGGINGS:
            case NETHERITE_BOOTS:
            case NETHERITE_HELMET:
            case NETHERITE_SWORD:
            case NETHERITE_AXE:
            case NETHERITE_HOE:
            case NETHERITE_PICKAXE:
            case NETHERITE_SHOVEL:
                return Material.NETHERITE_SCRAP;
            case LEATHER_BOOTS:
            case LEATHER_CHESTPLATE:
            case LEATHER_HELMET:
            case LEATHER_LEGGINGS:
                return Material.LEATHER;
            case FISHING_ROD:
            case BOW:
            case CROSSBOW:
                return Material.STRING;
            case CHAINMAIL_HELMET:
            case CHAINMAIL_CHESTPLATE:
            case CHAINMAIL_BOOTS:
            case CHAINMAIL_LEGGINGS:
                return Material.IRON_BARS;
            case TRIDENT:
                final String materialName = SkillConfigManager.getUseSetting(hero, this, "trident-material", "GOLD_INGOT");
                final Material reagent = Material.matchMaterial(materialName);
                if (reagent == null) {
                    Heroes.debugLog(Level.WARNING, "Invalid Trident reagent material: " + materialName);
                }
                return reagent; // may be null
            default:
                return null;
        }
    }
}