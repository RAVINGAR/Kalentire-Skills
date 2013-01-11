package com.herocraftonline.heroes.characters.skill.skills;

import java.util.Iterator;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.party.HeroParty;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;

public class SkillBalance extends ActiveSkill {

	public SkillBalance(Heroes plugin) {
		super(plugin, "Balance");
		setDescription("On use, balances the percent max health of everyone in the party within a $1 block radius.");
		setUsage("/skill balance");
		setIdentifiers("skill balance");
		setArgumentRange(0,0);
		setTypes(SkillType.SILENCABLE, SkillType.HEAL);
	}
	@Override
	public SkillResult use(Hero h, String[] arg1) {
		HeroParty heroParty = h.getParty();
		if(heroParty == null) {
			h.getPlayer().sendMessage(ChatColor.GRAY + "You are not in a party!");
			return SkillResult.INVALID_TARGET_NO_MSG;
		}
		int maxHealthTotal = 0;
		int currentHealthTotal = 0;
		Iterator<Hero> partyMembers = heroParty.getMembers().iterator();
		Vector v = h.getPlayer().getLocation().toVector();
		int range = SkillConfigManager.getUseSetting(h, this, "maxrange", 0, false);
		boolean skipRangeCheck = (range == 0);						//0 for no maximum range
		while(partyMembers.hasNext()) {
			Hero h2 = partyMembers.next();
			if(skipRangeCheck || h2.getPlayer().getLocation().toVector().distanceSquared(v) < range) {
				maxHealthTotal += h2.getMaxHealth();
				h.getPlayer().sendMessage("Added to maxHealth: " + h2.getName());
				currentHealthTotal += h2.getHealth();
				h.getPlayer().sendMessage("Added to currentHealth: " + h2.getName());
				h.getPlayer().sendMessage("Current currentHealth/MaxHealth " + currentHealthTotal + "/" + maxHealthTotal);

			}
			continue;
		}
		if(maxHealthTotal == h.getMaxHealth()) {
			h.getPlayer().sendMessage("There is noone in range to balance with!");
			return SkillResult.INVALID_TARGET_NO_MSG;
		}
		h.getPlayer().sendMessage("Max Party Health Value " + maxHealthTotal);
		h.getPlayer().sendMessage("Current Party Health Value " + currentHealthTotal);
		
		double healthMultiplier = currentHealthTotal/maxHealthTotal;
		h.getPlayer().sendMessage("Multiplier " + healthMultiplier);
		Iterator<Hero> applyHealthIterator = heroParty.getMembers().iterator();
		while(applyHealthIterator.hasNext()) {
			Hero applyHero = applyHealthIterator.next();
			if(skipRangeCheck || applyHero.getPlayer().getLocation().toVector().distanceSquared(v) < range) {
				applyHero.setHealth((int) (applyHero.getMaxHealth()*healthMultiplier));
				applyHero.syncHealth();
				if(applyHero.getName() == h.getName()) {
					h.getPlayer().sendMessage(ChatColor.GRAY + "You used Balance!");
				} else {
					applyHero.getPlayer().sendMessage(ChatColor.GRAY + h.getName() + " balanced your health with that of your party!");
				}
			}
		}
		return SkillResult.NORMAL;
	}

	@Override
	public String getDescription(Hero h) {
		int range = SkillConfigManager.getUseSetting(h, this, "maxrange", 10, false);
		return getDescription().replace("$1", range + "");
	}
	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();
		node.set("maxrange", Integer.valueOf(0));
		node.set(Setting.COOLDOWN.node(), Integer.valueOf(180000));
		return node;
		
	}
}
