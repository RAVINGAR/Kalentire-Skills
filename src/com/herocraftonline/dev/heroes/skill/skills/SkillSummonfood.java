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
import com.herocraftonline.dev.heroes.util.Setting;

public class SkillSummonfood extends ActiveSkill {

    public SkillSummonfood(Heroes plugin) {
        super(plugin, "Summonfood");
        setDescription("You summon $1 $2 at your feet.");
        setUsage("/skill summonfood");
        setArgumentRange(0, 0);
        setIdentifiers("skill summonfood", "skill sfood");
        setTypes(SkillType.ITEM, SkillType.SUMMON, SkillType.SILENCABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("food-type", "BREAD");
        node.set(Setting.AMOUNT.node(), 1);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        World world = player.getWorld();
        int amount = SkillConfigManager.getUseSetting(hero, this, Setting.AMOUNT, 1, false);
        ItemStack dropItem = new ItemStack(Material.matchMaterial(SkillConfigManager.getUseSetting(hero, this, "food-type", "BREAD")), amount);
        world.dropItem(player.getLocation(), dropItem);
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        int amount = SkillConfigManager.getUseSetting(hero, this, Setting.AMOUNT, 1, false);
        String name = SkillConfigManager.getUseSetting(hero, this, "food-type", "BREAD");
        name = name.toLowerCase().replace("_", " ");
        return getDescription().replace("$1", amount + "").replace("$2", name);
    }

}
