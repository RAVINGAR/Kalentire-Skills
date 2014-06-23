package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class SkillLabelChest extends ActiveSkill {

    public SkillLabelChest(Heroes plugin) {
        super(plugin, "LabelChest");
        setDescription("$1 of chance of renaming the item in your hand with a custom text.");
        setUsage("/skill LabelChest <Text>");
        setArgumentRange(1, 99);
        setIdentifiers("skill LabelChest");
        setTypes(SkillType.ITEM_MODIFYING, SkillType.UNBINDABLE);
    }

    @Override
    public String getDescription(Hero hero) {

        double chance = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.CHANCE.node(), 1.0, false) + (SkillConfigManager.getUseSetting(hero, this, SkillSetting.CHANCE_PER_LEVEL.node(), 0.0, false) * hero.getSkillLevel(this))) * 100;
        chance = chance > 0 ? chance : 0;
        String description = getDescription().replace("$1", chance + "%");

        return description;
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.CHANCE.node(), 1.0);
        node.set(SkillSetting.CHANCE_PER_LEVEL.node(), 0.0);

        List<String> itemList = Util.picks;
        itemList.add(Material.TRAPPED_CHEST.toString());
        itemList.add(Material.ENDER_CHEST.toString());
        itemList.add(Material.CHEST.toString());

        node.set("possible-items", itemList);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] text) {
        Player player = hero.getPlayer();

        if (text.length == 0) {
            Messaging.send(player, "/skill labelchest <Text>");
            return SkillResult.CANCELLED;
        }

        if (player.getItemInHand() == null) {
            Messaging.send(player, "You must be holding a chest in order to use this skill.");
            return SkillResult.CANCELLED;
        }

        ItemStack item = player.getItemInHand();
        Material type = item.getType();
        if (!SkillConfigManager.getUseSetting(hero, this, "possible-items", Util.tools).contains(type.name())) {
            Messaging.send(player, "You cannot label that item!");
            return SkillResult.FAIL;
        }

        double chance = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.CHANCE.node(), 1.0, false) + (SkillConfigManager.getUseSetting(hero, this, SkillSetting.CHANCE_PER_LEVEL.node(), 0.0, false) * hero.getSkillLevel(this)));
        chance = chance > 0 ? chance : 0;
        if (Math.random() <= chance) {

            String str = StringUtils.join(text, " "); //Thanks to NodinChan and blha303 and Gummy
            ItemMeta im = item.getItemMeta();

            im.setDisplayName(str);
            item.setItemMeta(im);

            player.getWorld().playSound(player.getLocation(), Sound.ANVIL_LAND, 0.6F, 1.0F);

            broadcastExecuteText(hero);

            return SkillResult.NORMAL;
        }
        else {
            Messaging.send(player, "You failed to label that item!");
            return SkillResult.FAIL;
        }
    }
}