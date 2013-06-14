package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.ClassChangeEvent;
import com.herocraftonline.heroes.api.events.HeroChangeLevelEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.classes.HeroClass;
import com.herocraftonline.heroes.characters.effects.BloodUnionEffect;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;

public class SkillBloodUnion extends PassiveSkill {

	//ScoreboardManager manager;
	//Scoreboard board;
	//HashMap<Hero, ScoreboardManager> bloodUnionManager;

	public SkillBloodUnion(Heroes plugin) {
		super(plugin, "BloodUnion");
		setDescription("Passive: Your damaging abilities form a Blood Union with your opponents. Blood Union allows you to use certain abilities, and also increases the effectiveness of others. Maximum Blood Union is $1. BloodUnion resets upon switching from monsters to players, and will expire by 1 every $2 seconds.");

		//ScoreboardManager manager = Bukkit.getScoreboardManager();
		//manager.getNewScoreboard();

		//bloodUnionManager = new HashMap<Hero, ScoreboardManager>();

		Bukkit.getPluginManager().registerEvents(new BloodUnionListener(this), plugin);
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.PERIOD.node(), 25000);
		node.set("max-blood-union", 4);

		return node;
	}

	@Override
	public String getDescription(Hero hero) {

		double period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 25000, false) / 1000;
		int maxBloodUnion = SkillConfigManager.getUseSetting(hero, this, "max-blood-union", 4, false);

		return getDescription().replace("$1", maxBloodUnion + "").replace("$2", period + "");
	}

	private class BloodUnionListener implements Listener {
		private final Skill skill;

		public BloodUnionListener(Skill skill) {
			this.skill = skill;

		}

		// Manipulate the RuneList hashmap on player death
		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onPlayerRespawn(PlayerRespawnEvent event) {
			Hero hero = skill.plugin.getCharacterManager().getHero(event.getPlayer());

			HeroClass heroClass = hero.getHeroClass();

			// This appears to be getting a null result at times. Check it first to avoid exceptions
			if (heroClass != null) {
				// Check if the class actually has the skill available
				if (heroClass.hasSkill(skill.getName())) {
					// The class does have the skill. Check to see if the hero is allowed to have it yet.
					int level = hero.getLevel(heroClass);
					int levelReq = SkillConfigManager.getSetting(heroClass, skill, SkillSetting.LEVEL.node(), 1);
					if (level >= levelReq) {
						// They are high enough level

						// Add
						if (hero.hasEffect("BloodUnionEffect")) {
							BloodUnionEffect buEffect = (BloodUnionEffect) hero.getEffect("BloodUnionEffect");
							buEffect.setBloodUnionLevel(0);
						}
						else {
							int bloodUnionResetPeriod = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.PERIOD, 25000, false);
							int maxBloodUnion = SkillConfigManager.getUseSetting(hero, skill, "max-blood-union", 4, false);
							hero.addEffect(new BloodUnionEffect(skill, bloodUnionResetPeriod, maxBloodUnion));
						}

						return;
					}
				}
			}
		}

		// Manipulate the Rune hashmap on player world change
		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onPlayerWorldChange(PlayerChangedWorldEvent event) {

			Hero hero = skill.plugin.getCharacterManager().getHero(event.getPlayer());
			HeroClass heroClass = hero.getHeroClass();
			int level = hero.getLevel(heroClass);

			// Check if the player's class actually has the skill available
			if (heroClass.hasSkill(skill.getName())) {
				// The class does have the skill. Check to see if the hero is high enough level to use it.
				int levelReq = SkillConfigManager.getSetting(heroClass, skill, SkillSetting.LEVEL.node(), 1);
				if (level >= levelReq) {
					// Add
					if (hero.hasEffect("BloodUnionEffect")) {
						BloodUnionEffect buEffect = (BloodUnionEffect) hero.getEffect("BloodUnionEffect");
						buEffect.setBloodUnionLevel(0);
					}
					else {
						int bloodUnionResetPeriod = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.PERIOD, 25000, false);
						int maxBloodUnion = SkillConfigManager.getUseSetting(hero, skill, "max-blood-union", 4, false);
						hero.addEffect(new BloodUnionEffect(skill, bloodUnionResetPeriod, maxBloodUnion));
					}
				}
			}

			return;
		}

		// Manipulate the RuneList hashmap on player join
		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onPlayerJoin(PlayerJoinEvent event) {

			Hero hero = skill.plugin.getCharacterManager().getHero(event.getPlayer());
			HeroClass heroClass = hero.getHeroClass();
			int level = hero.getLevel(heroClass);

			// Check if the player's class actually has the skill available
			if (heroClass.hasSkill(skill.getName())) {
				// The class does have the skill. Check to see if the hero is high enough level to use it.
				int levelReq = SkillConfigManager.getSetting(heroClass, skill, SkillSetting.LEVEL.node(), 1);
				if (level >= levelReq) {
					// Add
					if (hero.hasEffect("BloodUnionEffect")) {
						BloodUnionEffect buEffect = (BloodUnionEffect) hero.getEffect("BloodUnionEffect");
						buEffect.setBloodUnionLevel(0);
					}
					else {
						int bloodUnionResetPeriod = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.PERIOD, 25000, false);
						int maxBloodUnion = SkillConfigManager.getUseSetting(hero, skill, "max-blood-union", 4, false);
						hero.addEffect(new BloodUnionEffect(skill, bloodUnionResetPeriod, maxBloodUnion));
					}
				}
			}

			return;
		}

		// Manipulate the HashMap upon player logout
		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onPlayerQuit(PlayerQuitEvent event) {
			Hero hero = skill.plugin.getCharacterManager().getHero(event.getPlayer());

			// Remove
			// Check to see if the player has the effect
			if (!hero.hasEffect("BloodUnionEffect"))
				return;

			hero.removeEffect(hero.getEffect("BloodUnionEffect"));

			return;
		}

		// Manipulate the HashMap on hero level changes
		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onHeroChangeLevel(HeroChangeLevelEvent event) {
			// Prep variables
			Hero hero = event.getHero();
			HeroClass heroClass = hero.getHeroClass();

			// Check if the player's class actually has the skill available
			if (!heroClass.hasSkill(skill.getName()))
				return;					// Class does not have the skill. Do nothing.

			// Check to see if the player has the effect or not (He could be if he is being de-leveled)
			if (!hero.hasEffect("BloodUnionEffect")) {
				// Player is not on the hashmap. Check to see if he should be.

				int toLevel = event.getTo();

				// Check to see if the hero is high enough level to get the skill
				int levelReq = SkillConfigManager.getSetting(event.getHeroClass(), skill, SkillSetting.LEVEL.node(), 1);
				if (toLevel >= levelReq) {
					int bloodUnionResetPeriod = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.PERIOD, 25000, false);
					int maxBloodUnion = SkillConfigManager.getUseSetting(hero, skill, "max-blood-union", 4, false);
					hero.addEffect(new BloodUnionEffect(skill, bloodUnionResetPeriod, maxBloodUnion));
				}

				return;
			}
			else {
				// Player is on the hashmap. Check to see if we should remove him.

				int toLevel = event.getTo();

				// Check to see if the hero is high enough level to get the skill
				int levelReq = SkillConfigManager.getSetting(event.getHeroClass(), skill, SkillSetting.LEVEL.node(), 1);
				if (toLevel < levelReq) {
					// Remove
					hero.removeEffect(hero.getEffect("BloodUnionEffect"));
				}

				return;
			}
		}

		// Determine hash map manipulation on class switch
		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onClassChange(ClassChangeEvent event) {

			Hero hero = event.getHero();
			HeroClass to = event.getTo();

			// This appears to be getting a null result at times. Check it first to avoid exceptions
			if (to != null) {
				// Check if the class actually has the skill available
				if (!to.hasSkill(skill.getName())) {
					// The class does not have the skill. Remove effect.

					// Check to see if the player has the effect
					if (!hero.hasEffect("BloodUnionEffect"))
						return;

					hero.removeEffect(hero.getEffect("BloodUnionEffect"));

					return;
				}
				else {
					// The class does have the skill. Check to see if the hero is allowed to have it yet.
					int toLevel = hero.getLevel(to);
					int levelReq = SkillConfigManager.getSetting(to, skill, SkillSetting.LEVEL.node(), 1);
					if (toLevel < levelReq) {
						// They aren't high enough level

						// Remove
						// Check to see if the player has the effect
						if (!hero.hasEffect("BloodUnionEffect"))
							return;

						hero.removeEffect(hero.getEffect("BloodUnionEffect"));

						return;
					}
					else {
						// They are high enough level

						// Add
						if (hero.hasEffect("BloodUnionEffect")) {
							BloodUnionEffect buEffect = (BloodUnionEffect) hero.getEffect("BloodUnionEffect");
							buEffect.setBloodUnionLevel(0);
						}
						else {
							int bloodUnionResetPeriod = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.PERIOD, 25000, false);
							int maxBloodUnion = SkillConfigManager.getUseSetting(hero, skill, "max-blood-union", 4, false);
							hero.addEffect(new BloodUnionEffect(skill, bloodUnionResetPeriod, maxBloodUnion));
						}

						return;
					}
				}
			}
			else {
				Player player = hero.getPlayer();
				broadcast(player.getLocation(), "ToClass is null. WHYYYYYYY", player.getDisplayName());
			}
		}
	}
}