package com.herocraftonline.heroes.characters.skill.skills;

import java.util.Iterator;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.party.HeroParty;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.util.Setting;

public class SkillHealingBloom extends ActiveSkill {

	public SkillHealingBloom(Heroes plugin) {
		super(plugin, "HealingBloom");
		setDescription("HealingBlooms your party, healing them for $1$2 per $3s for $4s");
		setUsage("/skill healingbloom");
		setIdentifiers("skill healingbloom");
		setArgumentRange(0,0);
	}
	public class RejuvinationEffect extends PeriodicExpirableEffect {
		int mode;
		double amountHealed;
		public RejuvinationEffect(Skill skill, Heroes plugin,
				long period, long duration, int mode, double amountHealed) {
			super(skill, plugin, "HealingBloom", period, duration);
			this.mode = mode;
			this.amountHealed = amountHealed;
		}

		@Override
		public void tickHero(Hero hero) {
		    Player player = hero.getPlayer();
			switch(mode) {
			case 0:
				HeroRegainHealthEvent event = new HeroRegainHealthEvent(hero, (int) amountHealed, skill);
				Bukkit.getPluginManager().callEvent(event);
				if(!event.isCancelled()) {
					//new - hero.heal(hero.getPlayer().getHealth() + hrhEvent.getAmount()); 
					hero.heal(event.getAmount());
				}
				break;
			case 1:
				double multiplier = amountHealed * 0.01;
				HeroRegainHealthEvent event1 = new HeroRegainHealthEvent(hero, (int) (player.getMaxHealth() * multiplier), skill);
				Bukkit.getPluginManager().callEvent(event1);
				if(!event1.isCancelled()) {
					hero.heal(event1.getAmount());
				}
				break;
			case 2:
				double multiplier1 = amountHealed * 0.01;
				HeroRegainHealthEvent event2 = new HeroRegainHealthEvent(hero, (int) ((player.getMaxHealth() - player.getHealth())*multiplier1), skill);
				Bukkit.getPluginManager().callEvent(event2);
				if(!event2.isCancelled()) {
					hero.heal(event2.getAmount());
				    //player.setHealth(player.getHealth() + event2.getAmount());
				}
				break;
			}
		}

		@Override
		public void tickMonster(Monster arg0) {
			// TODO Auto-generated method stub
			
		}		
	}
	@Override
	public SkillResult use(Hero h, String[] args) {
		//Load Skill Mode
		boolean amount = SkillConfigManager.getUseSetting(h, this, "AmountMode", true);
		boolean percentMax = SkillConfigManager.getUseSetting(h, this, "PercentMaxHealthMode", true);
		boolean percentMissing = SkillConfigManager.getUseSetting(h, this, "PercentMissingHealthMode", true);
		int mode = 0;
		if(percentMax) {
			mode = 1;
		}
		if(percentMissing) {
			mode = 2;
		}
		if((!(amount || percentMax || percentMissing)) || (amount && percentMax) || (amount && percentMissing) || (percentMax && percentMissing)) {
			mode = 0;
			Bukkit.getServer().getLogger().log(Level.SEVERE, "[SkillHealingBloom] Invalid mode selection, defaulting to amount mode");
		}
		//
		
		HeroParty hParty = h.getParty();
		if(hParty == null) {
			h.getPlayer().sendMessage("You are not in a party. Coding for selfishness is not included in this skill!");
			return SkillResult.INVALID_TARGET_NO_MSG;
		}
		double amountHealed = SkillConfigManager.getUseSetting(h, this, "amount", 5, false);
		double period = SkillConfigManager.getUseSetting(h, this, "period", 1000, false);
		double duration = SkillConfigManager.getUseSetting(h, this, Setting.DURATION.node(), 30000, false);
		this.broadcast(h.getPlayer().getLocation(), h.getName() + " used HealingBloom!");
		Vector v = h.getPlayer().getLocation().toVector();
		Iterator<Hero> partyMembers = hParty.getMembers().iterator();
		int range = SkillConfigManager.getUseSetting(h, this, "maxrange", 0, false);
		boolean skipRangeCheck = (range == 0);						//0 for no maximum range
		while(partyMembers.hasNext()) {
			Hero h2 = partyMembers.next();
			if(skipRangeCheck || h2.getPlayer().getLocation().toVector().distanceSquared(v) < Math.pow(range, 2)); {
				h2.addEffect(new RejuvinationEffect(this, plugin, (long) period, (long)duration, mode, amountHealed));
			}
			continue;
		}
		return SkillResult.NORMAL;
	}

	@Override
	public String getDescription(Hero h) {
		boolean amount = SkillConfigManager.getUseSetting(h, this, "AmountMode", true);
		boolean percentMax = SkillConfigManager.getUseSetting(h, this, "PercentMaxHealthMode", false);
		boolean percentMissing = SkillConfigManager.getUseSetting(h, this, "PercentMissingHealthMode", false);
		int mode = 0;
		if(percentMax) {
			mode = 1;
		}
		if(percentMissing) {
			mode = 2;
		}
		if((!(amount || percentMax || percentMissing)) || (amount && percentMax) || (amount && percentMissing) || (percentMax && percentMissing)) {
			mode = 0;
			Bukkit.getServer().getLogger().log(Level.SEVERE, "[SkillHealingBloom] Invalid mode selection, defaulting to amount mode");
		}
		String modeOut = "ERROR: Skill getDescription() failed!";
		switch(mode) {
		case 0:
			modeOut = " health";
			break;
		case 1:
			modeOut = "% of their maximum health";
			break;
		case 2:
			modeOut = "% of their missing health";
			break;
		}
		double amountHealed = SkillConfigManager.getUseSetting(h, this, "amount", 5, false);
		double period = SkillConfigManager.getUseSetting(h, this, "period", 1000, false)*0.001;
		double duration = SkillConfigManager.getUseSetting(h, this, Setting.DURATION.node(), 30000, false)*0.001;

		return getDescription()
				.replace("$1",amountHealed + "")
				.replace("$2", modeOut)
				.replace("$3", period + "")
				.replace("$4", duration + "");
	}
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();
		node.set(Setting.DURATION.node(), Integer.valueOf(30000));
		node.set("period", Integer.valueOf(1000));
		node.set("amount", Integer.valueOf(5));
		node.set("AmountMode", true);
		node.set("PercentMaxHealthMode", false);
		node.set("PercentMissingHealthMode", false);
		node.set("maxrange", Integer.valueOf(0));
		return node;
	}
}