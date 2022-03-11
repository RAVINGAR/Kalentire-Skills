package com.herocraftonline.heroes.characters.skill.public1;

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
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

public class SkillSummonfood extends ActiveSkill {

    public SkillSummonfood(Heroes plugin) {
        super(plugin, "Summonfood");
        this.setDescription("You summon $1 $2 at your feet.");
        this.setUsage("/skill summonfood");
        this.setArgumentRange(0, 0);
        this.setIdentifiers("skill summonfood", "skill sfood");
        this.setTypes(SkillType.ITEM_CREATION, SkillType.SUMMONING, SkillType.SILENCEABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set("food-type", "BREAD");
        node.set(SkillSetting.AMOUNT.node(), 1);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();
        final World world = player.getWorld();
        final int amount = SkillConfigManager.getUseSetting(hero, this, SkillSetting.AMOUNT, 1, false);
        final ItemStack dropItem = new ItemStack(Material.matchMaterial(SkillConfigManager.getUseSetting(hero, this, "food-type", "BREAD")), amount);
        world.dropItem(player.getLocation(), dropItem);
        this.broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        final int amount = SkillConfigManager.getUseSetting(hero, this, SkillSetting.AMOUNT, 1, false);
        String name = SkillConfigManager.getUseSetting(hero, this, "food-type", "BREAD");
        name = name.toLowerCase().replace("_", " ");
        return this.getDescription().replace("$1", amount + "").replace("$2", name);
    }

}
