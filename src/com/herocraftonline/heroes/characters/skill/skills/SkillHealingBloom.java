package com.herocraftonline.heroes.characters.skill.skills;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillHealingBloom extends ActiveSkill {
	public SkillHealingBloom(Heroes plugin) {
		super(plugin, "HealingBloom");
		setDescription("Blooms your party, healing them for $1$2 per $3s for $4s");
		setUsage("/skill healingbloom");
		setIdentifiers(new String[] { "skill healingbloom" });
		setTypes(new SkillType[] { SkillType.SILENCABLE, SkillType.HEAL, SkillType.LIGHT });
		setArgumentRange(0, 0);
	}

	public SkillResult use(Hero hero, String[] args) {
		Player player = hero.getPlayer();
		
		boolean amount = SkillConfigManager.getUseSetting(hero, this, "AmountMode", true);
		boolean percentMax = SkillConfigManager.getUseSetting(hero, this, "PercentMaxHealthMode", true);
		boolean percentMissing = SkillConfigManager.getUseSetting(hero, this, "PercentMissingHealthMode", true);
		
		int mode = 0;
		if (percentMax) {
			mode = 1;
		}
		if (percentMissing) {
			mode = 2;
		}
		if (((!amount) && (!percentMax) && (!percentMissing)) || ((amount) && (percentMax)) || ((amount) && (percentMissing)) || ((percentMax) && (percentMissing))) {
			mode = 0;
			Bukkit.getServer().getLogger().log(Level.SEVERE, "[SkillHealingBloom] Invalid mode selection, defaulting to amount mode");
		}

		int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);
		int radiusSquared = radius * radius;
		double amountHealed = SkillConfigManager.getUseSetting(hero, this, "amount", 5, false);
		double period = SkillConfigManager.getUseSetting(hero, this, "period", 1000, false);
		double duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 30000, false);

		broadcastExecuteText(hero);

		// Check if the hero has a party
		if (hero.hasParty()) {
			Location playerLocation = player.getLocation();
			// Loop through the player's party members and add the effect as necessary
			for (Hero member : hero.getParty().getMembers()) {
				// Ensure the party member is in the same world.
				if (member.getPlayer().getLocation().getWorld().equals(playerLocation.getWorld())) {
					// Check to see if they are close enough to the player to receive the buff
					if (member.getPlayer().getLocation().distanceSquared(playerLocation) <= radiusSquared) {
						// Add the effect
						member.addEffect(new HealingBloomEffect(this, period, duration, mode, amountHealed));
					}
				}
			}
		}
		else {
			// Add the effect to just the player
			hero.addEffect(new HealingBloomEffect(this, period, duration, mode, amountHealed));
		}

		return SkillResult.NORMAL;
	}

	public String getDescription(Hero h) {
		boolean amount = SkillConfigManager.getUseSetting(h, this, "AmountMode", true);
		boolean percentMax = SkillConfigManager.getUseSetting(h, this, "PercentMaxHealthMode", false);
		boolean percentMissing = SkillConfigManager.getUseSetting(h, this, "PercentMissingHealthMode", false);
		int mode = 0;
		if (percentMax) {
			mode = 1;
		}
		if (percentMissing) {
			mode = 2;
		}
		if (((!amount) && (!percentMax) && (!percentMissing)) || ((amount) && (percentMax)) || ((amount) && (percentMissing)) || ((percentMax) && (percentMissing))) {
			mode = 0;
			Bukkit.getServer().getLogger().log(Level.SEVERE, "[SkillHealingBloom] Invalid mode selection, defaulting to amount mode");
		}
		String modeOut = "ERROR: Skill getDescription() failed!";
		switch (mode) {
		case 0:
			modeOut = " health";
			break;
		case 1:
			modeOut = "% of their maximum health";
			break;
		case 2:
			modeOut = "% of their missing health";
		}

		double amountHealed = SkillConfigManager.getUseSetting(h, this, "amount", 5, false);
		double period = SkillConfigManager.getUseSetting(h, this, "period", 1000, false) * 0.001D;
		double duration = SkillConfigManager.getUseSetting(h, this, SkillSetting.DURATION.node(), 30000, false) * 0.001D;

		return getDescription().replace("$1", amountHealed + "").replace("$2", modeOut).replace("$3", period + "").replace("$4", duration + "");
	}

	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.DURATION.node(), Integer.valueOf(30000));
		node.set("period", Integer.valueOf(1000));
		node.set("amount", Integer.valueOf(5));
		node.set("AmountMode", Boolean.valueOf(true));
		node.set("PercentMaxHealthMode", Boolean.valueOf(false));
		node.set("PercentMissingHealthMode", Boolean.valueOf(false));
		node.set("maxrange", Integer.valueOf(0));

		return node;
	}

	public class HealingBloomEffect extends PeriodicExpirableEffect {
		int mode;
		double amountHealed;

		public HealingBloomEffect(Skill skill, double period, double duration, int mode, double amountHealed) {
			super(skill, "HealingBloomEffect", (long) period, (long) duration);
			this.mode = mode;
			this.amountHealed = amountHealed;
		}

		public void tickHero(Hero hero) {
			Player player = hero.getPlayer();
			int amount = (int) amountHealed;
			switch (this.mode) {
			case 1:
				amount = (int) (player.getMaxHealth() * amountHealed * 0.01D);
				break;
			case 2:
				amount = (int) ((player.getMaxHealth() - player.getHealth()) * amountHealed * 0.01D);
				break;
			}

			HeroRegainHealthEvent event = new HeroRegainHealthEvent(hero, amount, skill);
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled())
				hero.heal(event.getAmount());
		}

		public void tickMonster(Monster arg0) {
		}
	}
}