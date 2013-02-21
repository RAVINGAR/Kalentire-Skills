package com.herocraftonline.heroes.characters.skill.skills;

import java.util.HashSet;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;

public class SkillEngrave extends ActiveSkill {

    private HashSet<Material> mats = new HashSet<Material>();

    public SkillEngrave(Heroes plugin) {
        super(plugin, "Engrave");
        setDescription("$1 of chance of renaming the item in your hand with a custom text.");
        setUsage("/skill Engrave <Text>");
        setArgumentRange(1, 99);
        setIdentifiers(new String[]{"skill Engrave"});
        setTypes(SkillType.ITEM, SkillType.UNBINDABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        double chance = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.CHANCE.node(), 1.0, false) +
                (SkillConfigManager.getUseSetting(hero, this, SkillSetting.CHANCE_LEVEL.node(), 0.0, false) * hero.getSkillLevel(this))) * 100;
        chance = chance > 0 ? chance : 0;
        String description = getDescription().replace("$1", chance + "%");
        return description;
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.CHANCE.node(), 1.0);
        node.set(SkillSetting.CHANCE_LEVEL.node(), 0.0);
        mats.add(Material.WOOD_AXE);
        mats.add(Material.WOOD_HOE);
        mats.add(Material.WOOD_PICKAXE);
        mats.add(Material.WOOD_SPADE);
        mats.add(Material.WOOD_SWORD);
        mats.add(Material.STONE_AXE);
        mats.add(Material.STONE_HOE);
        mats.add(Material.STONE_PICKAXE);
        mats.add(Material.STONE_SPADE);
        mats.add(Material.STONE_SWORD);
        mats.add(Material.IRON_AXE);
        mats.add(Material.IRON_HOE);
        mats.add(Material.IRON_PICKAXE);
        mats.add(Material.IRON_SPADE);
        mats.add(Material.IRON_SWORD);
        mats.add(Material.GOLD_AXE);
        mats.add(Material.GOLD_HOE);
        mats.add(Material.GOLD_PICKAXE);
        mats.add(Material.GOLD_SPADE);
        mats.add(Material.GOLD_SWORD);
        mats.add(Material.DIAMOND_AXE);
        mats.add(Material.DIAMOND_HOE);
        mats.add(Material.DIAMOND_PICKAXE);
        mats.add(Material.DIAMOND_SPADE);
        mats.add(Material.DIAMOND_SWORD);
        mats.add(Material.LEATHER_HELMET);
        mats.add(Material.LEATHER_CHESTPLATE);
        mats.add(Material.LEATHER_LEGGINGS);
        mats.add(Material.LEATHER_BOOTS);
        mats.add(Material.IRON_HELMET);
        mats.add(Material.IRON_CHESTPLATE);
        mats.add(Material.IRON_LEGGINGS);
        mats.add(Material.IRON_BOOTS);
        mats.add(Material.GOLD_HELMET);
        mats.add(Material.GOLD_CHESTPLATE);
        mats.add(Material.GOLD_LEGGINGS);
        mats.add(Material.GOLD_BOOTS);
        mats.add(Material.DIAMOND_HELMET);
        mats.add(Material.DIAMOND_CHESTPLATE);
        mats.add(Material.DIAMOND_LEGGINGS);
        mats.add(Material.DIAMOND_BOOTS);
        mats.add(Material.BOW);
        mats.add(Material.FISHING_ROD);
        mats.add(Material.SHEARS);
        mats.add(Material.BLAZE_ROD);
        mats.add(Material.STICK);
        mats.add(Material.GREEN_RECORD);
        mats.add(Material.GOLD_RECORD);
        mats.add(Material.RECORD_3);
        mats.add(Material.RECORD_4);
        mats.add(Material.RECORD_5);
        mats.add(Material.RECORD_6);
        mats.add(Material.RECORD_7);
        mats.add(Material.RECORD_8);
        mats.add(Material.RECORD_9);
        mats.add(Material.RECORD_10);
        mats.add(Material.RECORD_12);
        mats.add(Material.RECORD_11);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] text) {
        Player player = hero.getPlayer();
        if(text.length == 0){
            Messaging.send(player, "/skill engrave <Text>");
            return SkillResult.CANCELLED;
        }

        if(player.getItemInHand() == null){
            Messaging.send(player, "You must be holding an item in order to use this skill.");
            return SkillResult.CANCELLED;
        }
        ItemStack is = player.getItemInHand();

        for(Material mat : mats){
            if(is.getType().equals(mat)){
                double chance = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.CHANCE.node(), 1.0, false) +
                        (SkillConfigManager.getUseSetting(hero, this, SkillSetting.CHANCE_LEVEL.node(), 0.0, false) * hero.getSkillLevel(this)));
                chance = chance > 0 ? chance : 0;
                if(Math.random()<=chance){
                    String str = StringUtils.join(text, " "); //Thanks to NodinChan and blha303 and Gummy
                    ItemMeta im = is.getItemMeta();
                    im.setDisplayName(str);
                    is.setItemMeta(im);
                    hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ANVIL_LAND , 0.6F, 1.0F);
                    broadcastExecuteText(hero);
                    return SkillResult.NORMAL;
                }
                else return SkillResult.FAIL;
            }
        }
        Messaging.send(player, "You must be holding a tool or an armor in order to use this skill.");
        return SkillResult.CANCELLED;
    }
}
