package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillBloodBond extends ActiveSkill
{
	public SkillBloodBond(Heroes plugin)
	{
		super(plugin, "BloodBond");
		setDescription("Form a Blood Bond with your party. While bound, you convert $1% of your magic damage into healh for you and all party members within a $2 block radius. Costs $4 health to use, and $3 mana per second to maintain the effect.");
		setUsage("/skill bloodbond");
		setArgumentRange(0, 0);
		setIdentifiers("skill bloodbond");
		setTypes(SkillType.BUFF, SkillType.SILENCABLE, SkillType.HEAL);
		Bukkit.getServer().getPluginManager().registerEvents(new BloodBondListener(this), plugin);
	}

	@Override
	public String getDescription(Hero hero)
	{
		double healPercent = SkillConfigManager.getUseSetting(hero, this, "heal-percent", 0.15, false);
		int manaTick = SkillConfigManager.getUseSetting(hero, this, "mana-tick", 8, false);
		int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS.node(), 12, false);
		int healthCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST.node(), 25, false);

		return getDescription().replace("$1", (int) (healPercent * 100) + "").replace("$2", radius + "").replace("$3", manaTick + "").replace("$4", healthCost + "");
	}

	@Override
	public ConfigurationSection getDefaultConfig()
	{
		ConfigurationSection node = super.getDefaultConfig();

		node.set("heal-percent", 0.15);
		node.set(SkillSetting.RADIUS.node(), 12);
		node.set("mana-tick", 8);
		node.set("mana-tick-period", 3000);
		node.set("toggle-on-text", "§7[§2Skill§7] %hero% has formed a §lBloodBond§r!");
		node.set("toggle-off-text", "§7[§2Skill§7] %hero% has broken his §lBloodBonds§r!");

		return node;
	}

	@Override
	public SkillResult use(Hero hero, String args[])
	{
		if (hero.hasEffect("BloodBond"))
		{
			hero.removeEffect(hero.getEffect("BloodBond"));

			return SkillResult.FAIL;			// Default to fail so that it does not cost mana/health to remove.
		}

		// Get config values for the effect
		int manaTick = SkillConfigManager.getUseSetting(hero, this, "mana-tick", 8, false);
		int manaTickPeriod = SkillConfigManager.getUseSetting(hero, this, "mana-tick-period", 3000, false);

		// Get config values for text values
		String applyText = SkillConfigManager.getRaw(this, "toggle-on-text", "§7[§2Skill§7] %hero% has formed a §lBloodBond§r!").replace("%hero%", "$1");
		String expireText = SkillConfigManager.getRaw(this, "toggle-off-text", "§7[§2Skill§7] %hero% has broken his §lBloodBonds§r!").replace("%hero%", "$1");

		hero.addEffect(new BloodBondEffect(this, manaTick, manaTickPeriod, applyText, expireText));

		hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.WITHER_SPAWN, 0.5F, 1.0F);
		return SkillResult.NORMAL;
	}

	// Primary listener for bloodbond healing
	public class BloodBondListener implements Listener
	{
		private final Skill skill;

		public BloodBondListener(Skill skill)
		{
			this.skill = skill;
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onEntityDamageByEntity(EntityDamageByEntityEvent event)
		{
			// Pre-checks
			if (event.getCause().equals(DamageCause.MAGIC) && event.getDamager() instanceof Player)
			{
				// Make sure the hero has the bloodbond effect
				Hero hero = plugin.getCharacterManager().getHero((Player) event.getDamager());
				if (hero.hasEffect("BloodBond"))
				{
					healHeroParty(hero, event.getDamage());
				}
			}
		}

		@EventHandler(priority = EventPriority.MONITOR)
		public void onSkillDamage(SkillDamageEvent event)
		{
			// Pre-checks
			if (!(event.isCancelled()) && (event.getDamager() instanceof Player))
			{
				// Make sure the hero has the bloodbond effect
				Hero hero = plugin.getCharacterManager().getHero((Player) event.getDamager());
				if (hero.hasEffect("BloodBond"))
				{
					healHeroParty(hero, event.getDamage());
				}
			}
		}

		// Heals the hero and his party based on the specified damage
		private void healHeroParty(Hero hero, int damage)
		{
			// Set the healing amount
			double healPercent = SkillConfigManager.getUseSetting(hero, skill, "heal-percent", 0.15, false);
			int healAmount = (int) (healPercent * damage);

			// Set the distance variables 
			int radius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS, 20, false);
			int radiusSquared = radius * radius;

			// Check if the hero has a party
			if (hero.hasParty())
			{
				// Loop through the player's party members and heal as necessary
				for (Hero member : hero.getParty().getMembers())
				{
					// Check to see if they are close enough to the player to receive healing
					if (member.getPlayer().getLocation().distanceSquared(hero.getPlayer().getLocation()) <= radiusSquared)
					{
						// Heal the party member
						member.heal(healAmount);
					}
				}
			}
			else
			{
				// Heal the player
				hero.heal(healAmount);
			}
		}
	}

	// Bloodbond effect
	public class BloodBondEffect extends PeriodicEffect
	{
		private String applyText = "";
		private String expireText = "";

		private final int manaTick;
		private boolean firstTime = true;

		public BloodBondEffect(SkillBloodBond skill, int manaTick, int period, String applyText, String expireText)
		{
			super(skill, "BloodBond", period);

			this.manaTick = manaTick;
			this.applyText = applyText;
			this.expireText = expireText;

			this.types.add(EffectType.DISPELLABLE);
			this.types.add(EffectType.BENEFICIAL);
			this.types.add(EffectType.HEAL);
		}

		@Override
		public void applyToHero(Hero hero)
		{
			firstTime = true;
			super.applyToHero(hero);
			Player player = hero.getPlayer();
			broadcast(player.getLocation(), applyText, player.getDisplayName(), "BloodBond");
		}

		@Override
		public void removeFromHero(Hero hero)
		{
			super.removeFromHero(hero);
			Player player = hero.getPlayer();
			broadcast(player.getLocation(), expireText, player.getDisplayName(), "BloodBond");
		}

		@Override
		public void tickHero(Hero hero)
		{
			super.tickHero(hero);

			if (firstTime)		// Don't drain mana on first tick
			{
				firstTime = false;
			}
			else
			{
				// Remove the effect if they don't have enough mana
				if (hero.getMana() < manaTick)
				{
					hero.removeEffect(this);
				}
				else
				// They have enough mana--continue
				{
					// Drain the player's mana
					hero.setMana(hero.getMana() - manaTick);
				}
			}
		}
	}
}