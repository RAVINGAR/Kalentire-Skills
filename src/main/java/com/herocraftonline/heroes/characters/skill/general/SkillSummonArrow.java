package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;

public class SkillSummonArrow extends ActiveSkill {

    public SkillSummonArrow(Heroes plugin) {
        super(plugin, "SummonArrow");
        setDescription("You summon $1 arrows.");
        setUsage("/skill summonarrow");
        setArgumentRange(0, 0);
        setIdentifiers("skill summonarrow");
        setTypes(SkillType.ITEM_CREATION, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        int amount = SkillConfigManager.getUseSetting(hero, this, SkillSetting.AMOUNT, 16, false);
        return getDescription().replace("$1", amount + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.AMOUNT.node(), 16);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int amount = SkillConfigManager.getUseSetting(hero, this, SkillSetting.AMOUNT, 5, false);
        final World world = player.getWorld();
        PlayerInventory inventory = player.getInventory();

        HashMap<Integer, ItemStack> leftOvers = inventory.addItem(new ItemStack(Material.ARROW, amount));
        for (java.util.Map.Entry<Integer, ItemStack> entry : leftOvers.entrySet()) {
            world.dropItemNaturally(player.getLocation(), entry.getValue());
            player.sendMessage("Items have been dropped at your feet!");
        }

        world.playSound(hero.getPlayer().getLocation(), Sound.BLOCK_WOOD_HIT, 0.8F, 0.2F);
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }
}
