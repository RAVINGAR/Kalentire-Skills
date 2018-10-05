package com.herocraftonline.heroes.characters.skill.unusedskills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class SkillEngrave extends ActiveSkill {

    public SkillEngrave(Heroes plugin) {
        super(plugin, "Engrave");
        setDescription("$1 of chance of renaming the item in your hand with a custom text.");
        setUsage("/skill Engrave <Text>");
        setArgumentRange(1, 99);
        setIdentifiers("skill Engrave");
        setTypes(SkillType.ITEM_MODIFYING, SkillType.UNBINDABLE);
    }

    @Override
    public String getDescription(Hero hero) {

        double chance = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.CHANCE.node(), 1.0, false) + (SkillConfigManager.getUseSetting(hero, this, SkillSetting.CHANCE_PER_LEVEL.node(), 0.0, false) * hero.getHeroLevel(this))) * 100;
        chance = chance > 0 ? chance : 0;

        return getDescription().replace("$1", chance + "%");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.CHANCE.node(), 1.0);
        node.set(SkillSetting.CHANCE_PER_LEVEL.node(), 0.0);

        List<String> itemList = Util.weapons;
        itemList.addAll(Util.armors);
        itemList.addAll(Util.tools);
        itemList.add(Material.NOTE_BLOCK.toString());
        //FIXME It's in unused so I don't care but should be fixed.
//        itemList.add(Material.JUKEBOX.toString());
//        itemList.add(Material.RECORD_3.toString());
//        itemList.add(Material.RECORD_4.toString());
//        itemList.add(Material.RECORD_5.toString());
//        itemList.add(Material.RECORD_6.toString());
//        itemList.add(Material.RECORD_7.toString());
//        itemList.add(Material.RECORD_8.toString());
//        itemList.add(Material.RECORD_9.toString());
//        itemList.add(Material.RECORD_10.toString());
//        itemList.add(Material.RECORD_11.toString());
//        itemList.add(Material.RECORD_12.toString());
//        itemList.add(Material.GREEN_RECORD.toString());
//        itemList.add(Material.GOLD_RECORD.toString());
//        itemList.add(Material.STICK.toString());
//        itemList.add(Material.RAW_FISH.toString());
//        itemList.add(Material.BLAZE_ROD.toString());

        node.set("possible-items", itemList);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] text) {
        Player player = hero.getPlayer();

        if (text.length == 0) {
            player.sendMessage("/skill engrave <Text>");
            return SkillResult.CANCELLED;
        }

        if (NMSHandler.getInterface().getItemInMainHand(player.getInventory()) == null) {
            player.sendMessage("You must be holding an item in order to use this skill.");
            return SkillResult.CANCELLED;
        }

        ItemStack item = NMSHandler.getInterface().getItemInMainHand(player.getInventory());
        Material type = item.getType();
        if (!SkillConfigManager.getUseSetting(hero, this, "possible-items", Util.weapons).contains(type.name())) {
            player.sendMessage("You cannot engrave that item!");
            return SkillResult.FAIL;
        }

        double chance = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.CHANCE.node(), 1.0, false) + (SkillConfigManager.getUseSetting(hero, this, SkillSetting.CHANCE_PER_LEVEL.node(), 0.0, false) * hero.getHeroLevel(this)));
        chance = chance > 0 ? chance : 0;
        if (Math.random() <= chance) {

            String str = StringUtils.join(text, " "); //Thanks to NodinChan and blha303 and Gummy
            ItemMeta im = item.getItemMeta();

            im.setDisplayName(str);
            item.setItemMeta(im);

            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6F, 1.0F);

            broadcastExecuteText(hero);

            return SkillResult.NORMAL;
        }
        else {
            player.sendMessage("You failed to engrave that item!");
            return SkillResult.FAIL;
        }
    }
}
