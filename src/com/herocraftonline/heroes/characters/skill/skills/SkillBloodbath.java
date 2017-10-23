package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class SkillBloodbath extends PassiveSkill
{
	public SkillBloodbath(Heroes plugin) 
	{
		super(plugin, "Bloodbath");
		setDescription("You glory in the blood of the fallen, regaining $1 health for every kill you make.");
		setTypes(SkillType.HEALING);
		setEffectTypes(EffectType.HEALING, EffectType.BENEFICIAL, EffectType.MAGIC);
		Bukkit.getPluginManager().registerEvents(new BloodbathListener(this), plugin);
	}

	@Override
	public String getDescription(Hero hero) 
	{
		double healingAmount = SkillConfigManager.getUseSetting(hero, this, "heal-per-kill", 20, false);
		double healingIncrease = SkillConfigManager.getUseSetting(hero, this, "heal-per-kill-increase", 0.2, false) * hero.getSkillLevel(this);
		healingAmount += healingIncrease;

		return getDescription().replace("$1", healingAmount + "");
	}

	public ConfigurationSection getDefaultConfig() 
	{
		ConfigurationSection node = super.getDefaultConfig();

		node.set("heal-per-kill", 20);
		node.set("heal-per-kill-increase", 0.2);

		return node;
	}

	private class BloodbathListener implements Listener 
	{
		private Skill skill;

		public BloodbathListener(Skill skill) 
		{
			this.skill = skill;
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onEntityDeath(EntityDeathEvent event) 
		{
			if (!(event instanceof PlayerDeathEvent)) return;
			
			PlayerDeathEvent subEvent = (PlayerDeathEvent) event;
			
			if (!(subEvent.getEntity().getKiller() instanceof Player)) return;
			
			Player killer = (Player) subEvent.getEntity().getKiller();
			Hero hero = plugin.getCharacterManager().getHero(killer);

			if (!hero.canUseSkill(skill)) return;

			double healingAmount = SkillConfigManager.getUseSetting(hero, skill, "heal-per-kill", 20, false);
			double healingIncrease = SkillConfigManager.getUseSetting(hero, skill, "heal-per-kill-increase", 0.2, false) * hero.getSkillLevel(skill);
			healingAmount += healingIncrease;

			HeroRegainHealthEvent heal = new HeroRegainHealthEvent(hero, healingAmount, skill);
			Bukkit.getPluginManager().callEvent(heal);
			if (!heal.isCancelled()) 
			{
				hero.heal(healingAmount);
			}
			killer.getWorld().spigot().playEffect(killer.getLocation().add(0, 1, 0), Effect.COLOURED_DUST, 0, 0, 0.4F, 1.0F, 0.4F, 0.0F, 65, 16);
			killer.getWorld().spigot().playEffect(killer.getLocation().add(0, 1, 0), Effect.LARGE_SMOKE, 0, 0, 0.4F, 1.0F, 0.4F, 0.0F, 35, 16);
			killer.getWorld().playSound(killer.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1.0F, 1.2F);
		}
	}
}
