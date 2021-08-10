package com.herocraftonline.heroes.characters.skill.skills;

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
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SkillRepair extends ActiveSkill {

    String useText = null;

    public SkillRepair(Heroes plugin) {
        super(plugin, "Repair");
        setDescription("You are able to repair tools and armor. There is a $1% chance the item will be disenchanted.");
        setUsage("/skill repair <hotbarslot>");
        setArgumentRange(0, 1);
        setIdentifiers("skill repair");
        setTypes(SkillType.ITEM_MODIFYING, SkillType.ABILITY_PROPERTY_PHYSICAL);
    }

    @Override
    public String getDescription(Hero hero) {
        double unchant = SkillConfigManager.getUseSetting(hero, this, "unchant-chance", .5, true);
        unchant -= SkillConfigManager.getUseSetting(hero, this, "unchant-chance-reduce", .005, false) * hero.getHeroLevel(this);
        return getDescription().replace("$1", Util.stringDouble(unchant * 100.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.USE_TEXT.node(), "%hero% repaired a %item%%ench%");
        node.set("wood-weapons", 1);
        node.set("stone-weapons", 1);
        node.set("iron-weapons", 1);
        node.set("gold-weapons", 1);
        node.set("diamond-weapons", 1);
        node.set("netherite-weapons", 1);
        node.set("leather-armor", 1);
        node.set("iron-armor", 1);
        node.set("chain-armor", 1);
        node.set("gold-armor", 1);
        node.set("diamond-armor", 1);
        node.set("netherite-armor", 1);
        node.set("wood-tools", 1);
        node.set("stone-tools", 1);
        node.set("iron-tools", 1);
        node.set("gold-tools", 1);
        node.set("diamond-tools", 1);
        node.set("netherite-tools", 1);
        node.set("fishing-rod", 1);
        node.set("shears", 1);
        node.set("flint-steel", 1);
        node.set("netherite-armor-cost", 1);
        node.set("netherite-tool-cost", 1);
        node.set("additional-cost-per-enchantment-levels", 1);
        node.set("unchant-chance", .5);
        node.set("unchant-chance-reduce", .005);
        return node;
    }

    @Override
    public void init() {
        super.init();
        useText = SkillConfigManager.getRaw(this, SkillSetting.USE_TEXT, "%hero% repaired a %item%%ench%");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        ItemStack is;
        if (args == null || args.length < 1 ) {
            is = NMSHandler.getInterface().getItemInMainHand(player.getInventory());
        } else {
            int itemSlotNumber = 0;
            try {
                itemSlotNumber = Integer.parseInt(args[0]);
            } catch (final NumberFormatException e){
                player.sendMessage("That is not a valid slot number (0-8).");
                return SkillResult.INVALID_TARGET_NO_MSG;
            }

            // Support only hotbar slots
            if (itemSlotNumber > 8){
                player.sendMessage("That is not a valid hotbar slot number (0-8).");
                return SkillResult.INVALID_TARGET_NO_MSG;
            }

            is = player.getInventory().getItem(itemSlotNumber);
        }
        Material isType = is.getType();
        int level = getRequiredLevel(hero, isType);
        Material reagent = getRequiredReagent(isType);

        if (level == -1 || reagent == null) {
            player.sendMessage("You are not holding a repairable tool.");
            return SkillResult.FAIL;
        }

        if (hero.getHeroLevel(this) < level) {
            player.sendMessage("You must be level " + level + " to repair " + MaterialUtil.getFriendlyName(isType));
            return new SkillResult(ResultType.LOW_LEVEL, false);
        }
        if (is.getDurability() == 0) {
            player.sendMessage("That item is already at full durability!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        boolean enchanted = !is.getEnchantments().isEmpty();
        int repairCost = getRepairCost(hero, is);
        if (enchanted) {
            int additionalCostPerEnchantmentLevels = SkillConfigManager.getUseSetting(hero, this, "additional-cost-per-enchantment-levels", 1, true);
            int enchantmentLevels = 0;
            for (Map.Entry<Enchantment, Integer> entry : is.getEnchantments().entrySet()) {
                Enchantment enchantment = entry.getKey();
                Integer enchantmentLevel = entry.getValue();
                enchantmentLevels += enchantmentLevel;
            }
            repairCost += (additionalCostPerEnchantmentLevels * enchantmentLevels);
        }

        ItemStack reagentStack = null;
        if (reagent == Material.OAK_PLANKS){
            //Handle all wood variants as a reagent
            List<Material> woodMaterials = new ArrayList<Material>();
            woodMaterials.add(Material.OAK_PLANKS);
            woodMaterials.add(Material.BIRCH_PLANKS);
            woodMaterials.add(Material.SPRUCE_PLANKS);
            woodMaterials.add(Material.JUNGLE_PLANKS);
            woodMaterials.add(Material.ACACIA_PLANKS);
            woodMaterials.add(Material.DARK_OAK_PLANKS);

            boolean hasReagant = false;
            for (Material woodMaterial : woodMaterials) {
                reagentStack = new ItemStack(woodMaterial, repairCost);
                hasReagant = hasReagentCost(player, reagentStack);
                if (hasReagant){
                    // Found valid wood reagent that the player has
                    break;
                }
            }

            if (!hasReagant) {
                String planksString = MaterialUtil.getFriendlyName(Material.OAK_PLANKS)
                        + " or " + MaterialUtil.getFriendlyName(Material.BIRCH_PLANKS)
                        + " or " + MaterialUtil.getFriendlyName(Material.SPRUCE_PLANKS)
                        + " or " + MaterialUtil.getFriendlyName(Material.JUNGLE_PLANKS)
                        + " or " + MaterialUtil.getFriendlyName(Material.ACACIA_PLANKS)
                        + " or " + MaterialUtil.getFriendlyName(Material.DARK_OAK_PLANKS);
                return new SkillResult(ResultType.MISSING_REAGENT, true, repairCost, planksString);
            }
        } else {
            reagentStack = new ItemStack(reagent, repairCost);
            if (!hasReagentCost(player, reagentStack)) {
                return new SkillResult(ResultType.MISSING_REAGENT, true, reagentStack.getAmount(), MaterialUtil.getFriendlyName(reagentStack.getType()));
            }
        }

        boolean lost = false;
        if (enchanted) {
            double unchant = SkillConfigManager.getUseSetting(hero, this, "unchant-chance", .5, true);
            unchant -= SkillConfigManager.getUseSetting(hero, this, "unchant-chance-reduce", .005, false) * hero.getHeroLevel(this);
            if (Util.nextRand() <= unchant) {
                for (Enchantment enchant : new ArrayList<>(is.getEnchantments().keySet())) {
                    is.removeEnchantment(enchant);
                }
                lost = true;
            }
        }
        is.setDurability((short) 0);
        player.getInventory().removeItem(reagentStack);
        Util.syncInventory(player, plugin);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.BLOCK_ANVIL_USE, 0.6F, 1.0F);
        //hero.getPlayer().getWorld().spigot().playEffect(hero.getPlayer().getLocation().add(0, 0.6, 0), org.bukkit.Effect.ITEM_BREAK, Material.DIAMOND_SWORD.getId(), 0, 0.1F, 0.1F, 0.1F, 0.0F, 15, 16);
        broadcast(player.getLocation(), useText.replace("%hero%", player.getName()).replace("%item%", is.getType().name().toLowerCase().replace("_", " ")).replace("%ench%", !enchanted ? "." : lost ? " and stripped it of enchantments!" : " and successfully kept the enchantments."));        return SkillResult.NORMAL;
    }

    private int getRepairCost(Hero hero, ItemStack is) {
        Material mat = is.getType();
        int amt;
        switch (mat) {
            case BOW:
                amt = (int) ((is.getDurability() / (double) mat.getMaxDurability()) * 2.0);
                return amt < 1 ? 1 : amt;
            case NETHERITE_CHESTPLATE:
            case NETHERITE_LEGGINGS:
            case NETHERITE_BOOTS:
            case NETHERITE_HELMET:
                return SkillConfigManager.getUseSetting(hero, this, "netherite-armor-cost", 1, true); // FIXME how much cost per netherite item?
            case NETHERITE_SWORD:
            case NETHERITE_AXE:
            case NETHERITE_HOE:
            case NETHERITE_PICKAXE:
            case NETHERITE_SHOVEL:
                return SkillConfigManager.getUseSetting(hero, this, "netherite-tool-cost", 1, true); // FIXME how much cost per netherite item?
            case LEATHER_BOOTS:
            case IRON_BOOTS:
            case CHAINMAIL_BOOTS:
            case GOLDEN_BOOTS:
            case DIAMOND_BOOTS:
                amt = (int) ((is.getDurability() / (double) mat.getMaxDurability()) * 3.0);
                return amt < 1 ? 1 : amt;
            case LEATHER_HELMET:
            case IRON_HELMET:
            case CHAINMAIL_HELMET:
            case GOLDEN_HELMET:
            case DIAMOND_HELMET:
                amt = (int) ((is.getDurability() / (double) mat.getMaxDurability()) * 4.0);
                return amt < 1 ? 1 : amt;
            case LEATHER_CHESTPLATE:
            case IRON_CHESTPLATE:
            case CHAINMAIL_CHESTPLATE:
            case GOLDEN_CHESTPLATE:
            case DIAMOND_CHESTPLATE:
                amt = (int) ((is.getDurability() / (double) mat.getMaxDurability()) * 7.0);
                return amt < 1 ? 1 : amt;
            case LEATHER_LEGGINGS:
            case IRON_LEGGINGS:
            case CHAINMAIL_LEGGINGS:
            case GOLDEN_LEGGINGS:
            case DIAMOND_LEGGINGS:
                amt = (int) ((is.getDurability() / (double) mat.getMaxDurability()) * 6.0);
                return amt < 1 ? 1 : amt;
            default:
                return 1;
        }
    }

    private int getRequiredLevel(Hero hero, Material material) {
        switch (material) {
            case WOODEN_SWORD:
            case WOODEN_AXE:
            case BOW:
            case CROSSBOW:
                return SkillConfigManager.getUseSetting(hero, this, "wood-weapons", 1, true);
            case WOODEN_HOE:
            case WOODEN_PICKAXE:
            case WOODEN_SHOVEL:
                return SkillConfigManager.getUseSetting(hero, this, "wood-tools", 1, true);
            case STONE_SWORD:
            case STONE_AXE:
                return SkillConfigManager.getUseSetting(hero, this, "stone-weapons", 1, true);
            case STONE_HOE:
            case STONE_PICKAXE:
            case STONE_SHOVEL:
                return SkillConfigManager.getUseSetting(hero, this, "stone-tools", 1, true);
            case SHEARS:
                return SkillConfigManager.getUseSetting(hero, this, "shears", 1, true);
            case FLINT_AND_STEEL:
                return SkillConfigManager.getUseSetting(hero, this, "flint-steel", 1, true);
            case IRON_CHESTPLATE:
            case IRON_LEGGINGS:
            case IRON_BOOTS:
            case IRON_HELMET:
                return SkillConfigManager.getUseSetting(hero, this, "iron-armor", 1, true);
            case IRON_SWORD:
            case IRON_AXE:
                return SkillConfigManager.getUseSetting(hero, this, "iron-weapons", 1, true);
            case IRON_HOE:
            case IRON_PICKAXE:
            case IRON_SHOVEL:
                return SkillConfigManager.getUseSetting(hero, this, "iron-tools", 1, true);
            case CHAINMAIL_HELMET:
            case CHAINMAIL_CHESTPLATE:
            case CHAINMAIL_BOOTS:
            case CHAINMAIL_LEGGINGS:
                return SkillConfigManager.getUseSetting(hero, this, "chain-armor", 1, true);
            case GOLDEN_CHESTPLATE:
            case GOLDEN_LEGGINGS:
            case GOLDEN_BOOTS:
            case GOLDEN_HELMET:
                return SkillConfigManager.getUseSetting(hero, this, "gold-armor", 1, true);
            case GOLDEN_SWORD:
            case GOLDEN_AXE:
                return SkillConfigManager.getUseSetting(hero, this, "gold-weapons", 1, true);
            case GOLDEN_HOE:
            case GOLDEN_PICKAXE:
            case GOLDEN_SHOVEL:
                return SkillConfigManager.getUseSetting(hero, this, "gold-tools", 1, true);
            case DIAMOND_CHESTPLATE:
            case DIAMOND_LEGGINGS:
            case DIAMOND_BOOTS:
            case DIAMOND_HELMET:
                return SkillConfigManager.getUseSetting(hero, this, "diamond-armor", 1, true);
            case DIAMOND_SWORD:
            case DIAMOND_AXE:
                return SkillConfigManager.getUseSetting(hero, this, "diamond-weapons", 1, true);
            case DIAMOND_HOE:
            case DIAMOND_PICKAXE:
            case DIAMOND_SHOVEL:
                return SkillConfigManager.getUseSetting(hero, this, "diamond-tools", 1, true);
            case NETHERITE_SWORD:
            case NETHERITE_AXE:
                return SkillConfigManager.getUseSetting(hero, this, "netherite-weapons", 1, true);
            case NETHERITE_HOE:
            case NETHERITE_PICKAXE:
            case NETHERITE_SHOVEL:
                return SkillConfigManager.getUseSetting(hero, this, "netherite-tools", 1, true);
            case LEATHER_BOOTS:
            case LEATHER_CHESTPLATE:
            case LEATHER_HELMET:
            case LEATHER_LEGGINGS:
                return SkillConfigManager.getUseSetting(hero, this, "leather-armor", 1, true);
            case FISHING_ROD:
                return SkillConfigManager.getUseSetting(hero, this, "fishing-rod", 1, true);
            default:
                return -1;
        }
    }

    private Material getRequiredReagent(Material material) {
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
                return Material.NETHERITE_INGOT; // FIXME use netherite ingot or scrap for netherite items? (since only one is used per item, or just one ingot?)
            case LEATHER_BOOTS:
            case LEATHER_CHESTPLATE:
            case LEATHER_HELMET:
            case LEATHER_LEGGINGS:
                return Material.LEATHER;
            case FISHING_ROD:
            case BOW:
            case CROSSBOW: // FIXME use string for crossbow too?
                return Material.STRING;
            case CHAINMAIL_HELMET:
            case CHAINMAIL_CHESTPLATE:
            case CHAINMAIL_BOOTS:
            case CHAINMAIL_LEGGINGS:
                return Material.IRON_BARS;
            default:
                return null;
        }
    }
}