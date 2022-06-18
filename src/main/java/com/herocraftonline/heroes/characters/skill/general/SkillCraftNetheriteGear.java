package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.util.MaterialUtil;
import com.herocraftonline.heroes.util.Properties;
import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import net.Indyuce.mmoitems.stat.data.StringData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.logging.Level;

/**
 * Created By MysticMight 2021
 */

public class SkillCraftNetheriteGear extends PassiveSkill {

    private static final int SUBVERSION = 16;
    private final Set<Material> netheriteGear;

    public SkillCraftNetheriteGear(Heroes plugin) {
        super(plugin, "CraftNetheriteGear");
        setDescription("You are able to craft netherite items.");
        setArgumentRange(0, 0);
        setEffectTypes(EffectType.BENEFICIAL);

        netheriteGear = new HashSet<>();

        if(Properties.SUBVERSION >= this.SUBVERSION) {
            netheriteGear.add(Material.NETHERITE_SWORD);
            netheriteGear.add(Material.NETHERITE_PICKAXE);
            netheriteGear.add(Material.NETHERITE_AXE);
            netheriteGear.add(Material.NETHERITE_SHOVEL);
            netheriteGear.add(Material.NETHERITE_HOE);
            netheriteGear.add(Material.NETHERITE_HELMET);
            netheriteGear.add(Material.NETHERITE_CHESTPLATE);
            netheriteGear.add(Material.NETHERITE_LEGGINGS);
            netheriteGear.add(Material.NETHERITE_BOOTS);
            Bukkit.getServer().getPluginManager().registerEvents(new SkillCraftingListener(this), plugin);
        }
        else {
            Heroes.log(Level.SEVERE, "Could not load Skill " + this.getName() + " as it requires minimum Minecraft version of 1." + SUBVERSION);
        }


    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(Material.NETHERITE_SWORD.toString(), 1);
        config.set(Material.NETHERITE_PICKAXE.toString(), 1);
        config.set(Material.NETHERITE_AXE.toString(), 1);
        config.set(Material.NETHERITE_SHOVEL.toString(), 1);
        config.set(Material.NETHERITE_HOE.toString(), 1);
        config.set(Material.NETHERITE_HELMET.toString(), 1);
        config.set(Material.NETHERITE_CHESTPLATE.toString(), 1);
        config.set(Material.NETHERITE_LEGGINGS.toString(), 1);
        config.set(Material.NETHERITE_BOOTS.toString(), 1);
        config.set(SkillSetting.APPLY_TEXT.node(), "");
        config.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        return config;
    }

    public class SkillCraftingListener implements Listener {
        private final Map<UUID, ItemStack> upgradables;
        private final PassiveSkill skill;

        public SkillCraftingListener(PassiveSkill skill) {
            this.skill = skill;
            this.upgradables = new HashMap<>();
        }

        // Note Using the prepare event allows no result to appear when attempting to craft
        @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
        public void onPrepareUpgradingItem(PrepareSmithingEvent event) {
            final ItemStack result = event.getResult();
            if (result == null || !netheriteGear.contains(result.getType()))
                return; // Skip handled or non netherite items

            final Material resultType = result.getType();
            Hero hero = plugin.getCharacterManager().getHero((Player) event.getView().getPlayer());

            // Don't allow crafting to players that don't have this skill
            if (!skill.hasPassive(hero)) {
                hero.getPlayer().sendMessage(ChatColor.RED + "You must be a Blacksmith in order to create netherite gear!");
                event.setResult(null);
                return;
            }

            int levelRequired = SkillConfigManager.getUseSetting(hero, skill, resultType.toString(), 1, true);
            int level = hero.getHeroLevel(skill);

            // Don't allow crafting to players that don't have the level required for it
            if (level <= 0 || level < levelRequired) {
                hero.getPlayer().sendMessage(ChatColor.RED + "You must be level " + levelRequired + " to create "
                        + MaterialUtil.getFriendlyName(resultType) + "!");
                event.setResult(null);
                return;
                //event.getInventory().setResult(null);
            }

            upgradables.put(event.getView().getPlayer().getUniqueId(), event.getInventory().getItem(0));

            ItemStack hidden = new ItemStack(result.getType());
            ItemMeta meta = hidden.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "???");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Unidentified item!");
            lore.add(ChatColor.GRAY + "Upgrade this item to upgrade stats");
            meta.setLore(lore);
            hidden.setItemMeta(meta);
            event.setResult(hidden);
            //If we are at this point then craft is a success, replace with MMOItem todo
        }

        @EventHandler
        public void onInventoryClose(InventoryCloseEvent event) {
            if(event.getInventory().getType() == InventoryType.SMITHING) {
                upgradables.remove(event.getPlayer().getUniqueId());
            }
        }

        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            //Reference https://paste.denizenscript.com/View/97562
            if(event.isCancelled()) {
                return;
            }
            if(event.getInventory().getType() == InventoryType.SMITHING && plugin.getServer().getPluginManager().getPlugin("MMOItems") != null) {
                if(event.getSlotType() == InventoryType.SlotType.RESULT) {
                    ItemStack original = upgradables.remove(event.getView().getPlayer().getUniqueId());

                    Material material = Material.matchMaterial(original.getType().getKey().getKey().replaceAll("DIAMOND_", "NETHERITE_"));
                    if(material != null) {
                        original.setType(material);
                    }
                    else {
                        event.setCancelled(true);
                        return;
                    }

                    NBTItem nbtItem = NBTItem.get(original);
                    if(nbtItem.hasType()) {
                        Type type = MMOItems.plugin.getTypes().get(nbtItem.getType());
                        String typeName = type.getName();
                        MMOItem item = null;
                        if(typeName.equalsIgnoreCase("SWORD") || typeName.equalsIgnoreCase("AXE") || typeName.equalsIgnoreCase("TOOL") || typeName.equalsIgnoreCase("SPEAR") || typeName.equalsIgnoreCase("STAFF")) {
                            item =  upgradeWeaponMMOItem(nbtItem);
                        }
                        else if(typeName.equalsIgnoreCase("ARMOR")) {
                            item = upgradeArmorMMOItem(nbtItem);
                        }
                        else {
                            return;
                        }

                        NBTItem result = item.newBuilder().buildNBT();
                        event.setCurrentItem(result.toItem());

                    }
                }
            }
        }

        private MMOItem upgradeWeaponMMOItem(NBTItem item) {
            MMOItem mmoItem = new LiveMMOItem(item);
            mmoItem.setData(ItemStats.NAME, new StringData(mmoItem.getData(ItemStats.NAME).toString().replaceAll("Diamond", "Netherite")));
            mmoItem.setData(ItemStats.MAX_DURABILITY, new DoubleData(Double.parseDouble(mmoItem.getData(ItemStats.MAX_DURABILITY).toString()) + 600));
            mmoItem.setData(ItemStats.ATTACK_DAMAGE, new DoubleData(Double.parseDouble(mmoItem.getData(ItemStats.ATTACK_DAMAGE).toString()) + 2));
            return mmoItem;
        }

        private MMOItem upgradeArmorMMOItem(NBTItem item) {
            MMOItem mmoItem = new LiveMMOItem(item);

            mmoItem.setData(ItemStats.NAME, new StringData(mmoItem.getData(ItemStats.NAME).toString().replaceAll("Diamond", "Netherite")));
            mmoItem.setData(ItemStats.MAX_DURABILITY, new DoubleData(Double.parseDouble(mmoItem.getData(ItemStats.MAX_DURABILITY).toString()) + 1000));
            //mmoItem.setData(ItemStats.ARMOR, new DoubleData(Double.parseDouble(mmoItem.getData(ItemStats.ARMOR).toString()) + 2));
            mmoItem.setData(ItemStats.ARMOR_TOUGHNESS, new DoubleData(Double.parseDouble(mmoItem.getData(ItemStats.ARMOR_TOUGHNESS).toString()) + 1));
            double base = 0.1;
            if(mmoItem.hasData(ItemStats.KNOCKBACK_RESISTANCE)) {
                base += Double.parseDouble(mmoItem.getData(ItemStats.KNOCKBACK_RESISTANCE).toString());
            }
            mmoItem.setData(ItemStats.KNOCKBACK_RESISTANCE, new DoubleData(base));

            return mmoItem;
        }
    }
//
//    public HeroClass getClassWithSkill(Hero hero, Skill skill) {
//        HeroClass heroClass = null;
//
//        HeroClass prim = hero.getHeroClass();
//        HeroClass sec = hero.getSecondaryClass();
//        HeroClass race = hero.getRaceClass();
//        if (prim.hasSkill(skill.getName())){
//            heroClass = prim;
//        }
//
//        if (sec.hasSkill(skill.getName())){
//            heroClass = sec;
//        }
//
//        if (race.hasSkill(skill.getName())){
//            heroClass = race;
//        }
//        return heroClass;
//    }
}
