package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;

public class SkillSummonfood extends ActiveSkill {

    public SkillSummonfood(Heroes plugin) {
        super(plugin, "Summonfood");
        setDescription("You summon $1.");
        setUsage("/skill summonfood");
        setArgumentRange(0, 0);
        setIdentifiers("skill summonfood");
        setTypes(SkillType.ITEM, SkillType.SUMMON, SkillType.SILENCABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("food-type", "BREAD");
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        World world = player.getWorld();
        ItemStack dropItem = new ItemStack(Material.matchMaterial(SkillConfigManager.getUseSetting(hero, this, "food-type", "BREAD")), 1);
        world.dropItem(player.getLocation(), dropItem);
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        String name = SkillConfigManager.getUseSetting(hero, this, "food-type", "BREAD");
        name = name.toLowerCase().replace("_", " ");
        return getDescription().replace("$1", name);
    }

}
