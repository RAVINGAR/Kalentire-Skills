package com.herocraftonline.heroes.characters.skill.reborn.pathfinder;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SkillSummonArrow extends ActiveSkill {

    public SkillSummonArrow(Heroes plugin) {
        super(plugin, "SummonArrow");
        this.setDescription("You summon 16 arrows.");
        this.setUsage("/skill summonarrow");
        this.setArgumentRange(0, 0);
        this.setIdentifiers("skill summonarrow", "skill sarrow");
        this.setTypes(SkillType.ITEM_CREATION, SkillType.SUMMONING, SkillType.SILENCEABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.AMOUNT.node(), 16);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();
        final World world = player.getWorld();
        final ItemStack dropItem = new ItemStack(Material.ARROW, SkillConfigManager.getUseSetting(hero, this, "amount", 2, false));
        player.getInventory().addItem(dropItem);
        this.broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        final int amount = SkillConfigManager.getUseSetting(hero, this, SkillSetting.AMOUNT, 2, false);
        return this.getDescription().replace("$1", amount + "");
    }

}
