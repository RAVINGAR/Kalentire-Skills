package com.herocraftonline.heroes.characters.skill.skills;

import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;

public class SkillAssemblePickaxe extends ActiveSkill {

    public SkillAssemblePickaxe(Heroes plugin) {
        super(plugin, "AssemblePickaxe");
        setDescription("You gather spare parts and assemble a pickaxe.");
        setUsage("/skill assemblepickaxe");
        setArgumentRange(0, 0);
        setIdentifiers("skill assemblepickaxe", "skill summonpickaxe", "skill pickaxe");
        setTypes(SkillType.ITEM_CREATION, SkillType.SILENCABLE);
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

        broadcastExecuteText(hero);

        PlayerInventory inventory = player.getInventory();
        HashMap<Integer, ItemStack> leftOvers = inventory.addItem(new ItemStack[] { new ItemStack(Material.STONE_PICKAXE, amount) });
        for (java.util.Map.Entry<Integer, ItemStack> entry : leftOvers.entrySet()) {
            player.getWorld().dropItemNaturally(player.getLocation(), entry.getValue());
            Messaging.send(player, "Items have been dropped at your feet!");
        }

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_BREAK, 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }
}
