package com.herocraftonline.heroes.characters.skill.skills;

import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;

public class SkillSummonArrow extends ActiveSkill {

    public SkillSummonArrow(Heroes plugin) {
        super(plugin, "SummonArrow");
        setDescription("You summon $1 arrows.");
        setUsage("/skill summonarrow");
        setArgumentRange(0, 0);
		setIdentifiers("skill summonarrow");
        setTypes(SkillType.ITEM, SkillType.SUMMON, SkillType.SILENCABLE);
    }

	@Override
	public String getDescription(Hero hero) {
		int amount = SkillConfigManager.getUseSetting(hero, this, SkillSetting.AMOUNT, 5, false);

		return getDescription().replace("$1", amount + "");
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

		int amount = SkillConfigManager.getUseSetting(hero, this, "amount", 5, false);

		PlayerInventory inventory = player.getInventory();
		HashMap<Integer, ItemStack> leftOvers = inventory.addItem(new ItemStack[] { new ItemStack(Material.ARROW, amount) });
		for (java.util.Map.Entry<Integer, ItemStack> entry : leftOvers.entrySet()) {
			player.getWorld().dropItemNaturally(player.getLocation(), entry.getValue());
			Messaging.send(player, "Items have been dropped at your feet!", new Object[0]);
		}

        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.DIG_WOOD , 0.8F, 0.2F); 
        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }



}
