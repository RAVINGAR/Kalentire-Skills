package com.herocraftonline.heroes.characters.skill.pack4;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;

public class SkillSummonSword extends ActiveSkill {

    public SkillSummonSword(Heroes plugin) {
        super(plugin, "SummonSword");
        setDescription("You conjure a weapon.");
        setUsage("/skill summonsword");
        setArgumentRange(0, 0);
        setIdentifiers("skill summonsword");
        setTypes(SkillType.ITEM_CREATION, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        int amount = SkillConfigManager.getUseSetting(hero, this, SkillSetting.AMOUNT, 1, false);
        return getDescription().replace("$1", amount + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.AMOUNT.node(), 1);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int amount = SkillConfigManager.getUseSetting(hero, this, SkillSetting.AMOUNT, 1, false);

        PlayerInventory inventory = player.getInventory();
        HashMap<Integer, ItemStack> leftOvers = inventory.addItem(new ItemStack(Material.IRON_SWORD, amount));
        for (java.util.Map.Entry<Integer, ItemStack> entry : leftOvers.entrySet()) {
            player.getWorld().dropItemNaturally(player.getLocation(), entry.getValue());
            player.sendMessage("Item(s) have been dropped at your feet!");
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8F, 1.0F);
        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }
}
