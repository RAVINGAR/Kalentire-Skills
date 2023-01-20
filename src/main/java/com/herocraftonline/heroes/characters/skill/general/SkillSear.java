package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillSear extends TargettedSkill
{
	public SkillSear(Heroes plugin)
	{
		super(plugin, "Sear");
		setDescription("Use your holy book to sear your target, dealing $1 damage every 2 second(s) for $2 second(s).");
		setUsage("/skill sear");
		setArgumentRange(0, 0);
		setIdentifiers(new String[] { "skill sear" });
		setTypes(new SkillType[] { SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_LIGHT });
	}

	public ConfigurationSection getDefaultConfig()
	{
		ConfigurationSection node = super.getDefaultConfig();
		node.set(SkillSetting.DAMAGE.node(), 10);
		node.set(SkillSetting.MAX_DISTANCE.node(), 2);
		node.set(SkillSetting.PERIOD.node(), 3000);
		node.set(SkillSetting.DAMAGE_TICK.node(), 1);
		node.set(SkillSetting.DAMAGE_INCREASE.node(), 0.2);
		node.set(SkillSetting.DURATION.node(), 8000);
		return node;
	}

	public SkillResult use(Hero hero, LivingEntity target, String[] args)
	{
		Player player = hero.getPlayer();
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 10, false);
		damage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0.2, false) * hero.getHeroLevel(this);
		long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 8000, false);
		
		//target.getWorld().spigot().playEffect(target.getEyeLocation(), Effect.FLAME, 0, 0, 0.2F, 0.2F, 0.2F, 0.4F, 45, 16);
		target.getWorld().spawnParticle(Particle.FLAME, target.getEyeLocation(), 45, 0.2, 0.2, 0.2, 0.4);
		//target.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), Effect.COLOURED_DUST, 0, 0, 0.4F, 0.4F, 0.4F, 0.0F, 15, 16);
		target.getWorld().spawnParticle(Particle.REDSTONE, target.getLocation().add(0, 0.5, 0), 15, 0.4, 0.4, 0.4, 0, new Particle.DustOptions(Color.ORANGE, 1));
		//target.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), Effect.LARGE_SMOKE, 0, 0, 0.4F, 0.4F, 0.4F, 0.0F, 15, 16);
		target.getWorld().spawnParticle(Particle.SMOKE_LARGE, target.getLocation().add(0, 0.5, 0), 15, 0.4, 0.4, 0.4, 0);
		target.getWorld().playEffect(target.getLocation(), Effect.BLAZE_SHOOT, 2);
		target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_INFECT, 1.2F, 0.85F);
		
		CharacterTemplate targCT = plugin.getCharacterManager().getCharacter(target);
		SearEffect sear = new SearEffect(plugin, this, duration, damage, player, hero);
		targCT.addEffect(sear);

		broadcast(player.getLocation(), ChatComponents.GENERIC_SKILL + ChatColor.WHITE + hero.getName() + ChatColor.GRAY + " used " + ChatColor.WHITE + getName() + ChatColor.GRAY + " on " + ChatColor.WHITE + target.getName() + ChatColor.GRAY + "!" );
		return SkillResult.NORMAL;
	}

	public String getDescription(Hero hero)
	{
		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 10, false);
		damage += (int)(SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0.2, false) * hero.getHeroLevel(this));
		long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 8000, false);
		String formattedDuration = String.valueOf(duration / 1000);
		return getDescription().replace("$1", damage + "").replace("$2", formattedDuration);
	}
	
	public static class SearEffect extends PeriodicDamageEffect
	{
		private final Hero applyH;
		public SearEffect(Heroes plugin, Skill skill, long duration, double damage, Player applier, Hero applyH)
		{
			super(skill, "Sear", applier, 2000, duration, damage);
			this.applyH = applyH;
		}
		
		public void applyToHero(Hero hero)
		{
			super.applyToHero(hero);
			hero.getPlayer().sendMessage("    " + ChatComponents.GENERIC_SKILL + "You have been " + ChatColor.WHITE + "Seared" + ChatColor.GRAY + " by " + ChatColor.WHITE + applyH.getName() + ChatColor.GRAY + "!");
		}

		public void removeFromHero(Hero hero)
		{
			super.removeFromHero(hero);
			hero.getPlayer().sendMessage("    " + ChatComponents.GENERIC_SKILL+ "You are no longer seared.");
		}
		
		public void tickHero(Hero hero)
		{
			final Player player = hero.getPlayer();
			//player.getWorld().spigot().playEffect(player.getLocation().add(0, 0.5, 0), Effect.VILLAGER_THUNDERCLOUD, 0, 0, 0.4F, 0.4F, 0.4F, 0.3F, 25, 16);
			player.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, player.getLocation().add(0, 0.5, 0), 25, 0.4, 0.4, 0.4, 0.3);
			super.tickHero(hero);
		}
	}
}
