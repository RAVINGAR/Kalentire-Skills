package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Effect;
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
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;

public class SkillSummonArrow extends ActiveSkill {

    public SkillSummonArrow(Heroes plugin) {
        super(plugin, "SummonArrow");
        setDescription("You summon $1 arrows.");
        setUsage("/skill summonarrow");
        setArgumentRange(0, 0);
        setIdentifiers("skill summonarrow", "skill sarrow");
        setTypes(SkillType.ITEM, SkillType.SUMMON, SkillType.SILENCABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.AMOUNT.node(), 2);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        World world = player.getWorld();
        ItemStack dropItem = new ItemStack(Material.ARROW, SkillConfigManager.getUseSetting(hero, this, "amount", 2, false));
        world.dropItem(player.getLocation(), dropItem);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ARROW_HIT , 10.0F, 1.0F); 
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        int amount = SkillConfigManager.getUseSetting(hero, this, Setting.AMOUNT, 2, false);
        return getDescription().replace("$1", amount + "");
    }

}
