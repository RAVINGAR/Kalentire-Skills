package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillSummonPickaxe extends ActiveSkill {

    public SkillSummonPickaxe(Heroes plugin) {
        super(plugin, "SummonPickaxe");
        setDescription("You summon a mysterious pickaxe.");
        setUsage("/skill summonpickaxe");
        setArgumentRange(0, 0);
        setIdentifiers("skill summonpickaxe", "skill pickaxe");
        setTypes(SkillType.ITEM, SkillType.SUMMON, SkillType.SILENCABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.AMOUNT.node(), 2);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        World world = player.getWorld();
        ItemStack dropItem = new ItemStack(Material.STONE_PICKAXE, SkillConfigManager.getUseSetting(hero, this, "amount", 2, false));
        world.dropItem(player.getLocation(), dropItem);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ITEM_BREAK , 0.8F, 1.0F); 
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        int amount = SkillConfigManager.getUseSetting(hero, this, SkillSetting.AMOUNT, 2, false);
        return getDescription().replace("$1", amount + "");
    }

}
