package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.CompatSound;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;

public class SkillAssemblePickaxe extends ActiveSkill {

    public SkillAssemblePickaxe(Heroes plugin) {
        super(plugin, "AssemblePickaxe");
        setDescription("You gather spare parts and assemble a pickaxe.");
        setUsage("/skill assemblepickaxe");
        setArgumentRange(0, 0);
        setIdentifiers("skill assemblepickaxe");
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

        broadcastExecuteText(hero);

        PlayerInventory inventory = player.getInventory();
        HashMap<Integer, ItemStack> leftOvers = inventory.addItem(new ItemStack(Material.IRON_PICKAXE, amount));
        for (java.util.Map.Entry<Integer, ItemStack> entry : leftOvers.entrySet()) {
            player.getWorld().dropItemNaturally(player.getLocation(), entry.getValue());
            player.sendMessage("Item(s) have been dropped at your feet!");
        }

        player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_ITEM_BREAK.value(), 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }
}
