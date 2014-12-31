package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;

public class SkillSummonFood extends ActiveSkill {

    public SkillSummonFood(Heroes plugin) {
        super(plugin, "SummonFood");
        setDescription("You summon $1 $2 at your feet.");
        setUsage("/skill summonfood");
        setArgumentRange(0, 0);
        setIdentifiers("skill summonfood");
        setTypes(SkillType.ITEM_CREATION, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        int amount = SkillConfigManager.getUseSetting(hero, this, SkillSetting.AMOUNT, 1, false);
        String name = SkillConfigManager.getUseSetting(hero, this, "food-type", "BREAD");
        name = name.toLowerCase().replace("_", " ");

        return getDescription().replace("$1", amount + "").replace("$2", name);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("food-type", "BREAD");
        node.set(SkillSetting.AMOUNT.node(), 1);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        int amount = SkillConfigManager.getUseSetting(hero, this, SkillSetting.AMOUNT, 5, false);
        Material foodType = Material.matchMaterial(SkillConfigManager.getUseSetting(hero, this, "food-type", "BREAD"));

        PlayerInventory inventory = player.getInventory();
        HashMap<Integer, ItemStack> leftOvers = inventory.addItem(new ItemStack(foodType, amount));
        for (java.util.Map.Entry<Integer, ItemStack> entry : leftOvers.entrySet()) {
            player.getWorld().dropItemNaturally(player.getLocation(), entry.getValue());
            Messaging.send(player, "Items have been dropped at your feet!");
        }
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.BURP, 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }
}
