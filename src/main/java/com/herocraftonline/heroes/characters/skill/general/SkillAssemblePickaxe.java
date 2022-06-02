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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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

        player.sendMessage("Items have been dropped at your feet!");
        for(int i = 0; i < amount; i++) {
            player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(Material.IRON_PICKAXE, 1));
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }
}
