package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.Listenable;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;

public class SkillBloodbath extends PassiveSkill implements Listenable
{
	private final Listener listener;
	public SkillBloodbath(Heroes plugin) 
	{
		super(plugin, "Bloodbath");
		setDescription("You glory in the blood of the fallen, regaining $1 health for every kill you make against non-players, whilst player kills restore $2");
		setTypes(SkillType.HEALING);
		setEffectTypes(EffectType.HEALING, EffectType.BENEFICIAL, EffectType.MAGIC);
		this.listener = new BloodbathListener(this);
	}

	@Override
	public String getDescription(Hero hero) 
	{
		double healingAmount = SkillConfigManager.getScaledUseSettingDouble(hero, this, "heal-per-kill", 10, false);
		double healingAmountPvp = SkillConfigManager.getScaledUseSettingDouble(hero, this, "heal-per-kill-pvp", 20, false);

		return getDescription().replace("$1", healingAmount + "").replace("$2", "" + healingAmountPvp);
	}

	public ConfigurationSection getDefaultConfig() 
	{
		ConfigurationSection node = super.getDefaultConfig();

		node.set("heal-per-kill", 10);
		node.set("heal-per-kill-pvp", 20);
		return node;
	}

	@NotNull
	@Override
	public Listener getListener() {
		return listener;
	}

	private class BloodbathListener implements Listener 
	{
		private final Skill skill;

		public BloodbathListener(Skill skill) 
		{
			this.skill = skill;
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onEntityDeath(EntityDeathEvent event) {
			LivingEntity entity = event.getEntity();
			Player killer = entity.getKiller();
			if(killer == null) {
				return;
			}

			Hero hero = plugin.getCharacterManager().getHero(killer);

			if (!hero.canUseSkill(skill)) {
				return;
			}

			double healAmount = entity instanceof Player
					? SkillConfigManager.getScaledUseSettingDouble(hero, skill, "heal-per-kill-pvp", 20, true)
					: SkillConfigManager.getScaledUseSettingDouble(hero, skill, "heal-per-kill", 20, true);

			HeroRegainHealthEvent heal = new HeroRegainHealthEvent(hero, healAmount, skill);
			Bukkit.getPluginManager().callEvent(heal);
			if (!heal.isCancelled()) 
			{
				hero.heal(healAmount);
			}
			killer.getWorld().spawnParticle(Particle.REDSTONE, killer.getLocation().add(0, 1, 0), 65, 0.4, 1, 0.4, 0);
			killer.getWorld().spawnParticle(Particle.SMOKE_LARGE, killer.getLocation().add(0, 1, 0), 35, 0.4, 1, 0.4, 0);
			killer.getWorld().playSound(killer.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1.0F, 1.2F);
		}
	}
}
