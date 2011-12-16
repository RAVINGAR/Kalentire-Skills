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

public class SkillSummonArrow extends ActiveSkill {

    public SkillSummonArrow(Heroes plugin) {
        super(plugin, "SummonArrow");
        setDescription("Summons you some arrows!");
        setUsage("/skill summonarrow");
        setArgumentRange(0, 0);
        setIdentifiers("skill summonarrow");
        setTypes(SkillType.ITEM, SkillType.SUMMON, SkillType.SILENCABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.AMOUNT.node(), 1);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        World world = player.getWorld();
        ItemStack dropItem = new ItemStack(Material.ARROW, SkillConfigManager.getUseSetting(hero, this, "amount", 1, false));
        world.dropItem(player.getLocation(), dropItem);
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

}
