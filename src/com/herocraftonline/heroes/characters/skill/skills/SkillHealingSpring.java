package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicHealEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;


public class SkillHealingSpring extends ActiveSkill
{
	public SkillHealingSpring(Heroes plugin) 
	{
		super(plugin, "HealingSpring");
		setDescription("A Healing Spring wells up beneath you, restoring $1 health to your party (within $4 blocks) every $2 second(s) for $3 second(s).");
		setUsage("/skill healingspring");
		setArgumentRange(0, 0);
		setIdentifiers("skill healingspring", "skill spring");
		setTypes(SkillType.BUFFING, SkillType.HEALING);
	}
	
	public ConfigurationSection getDefaultConfig()
	{
		ConfigurationSection node = super.getDefaultConfig();
		node.set(SkillSetting.RADIUS.node(), 12);
		node.set(SkillSetting.HEALING.node(), 13);
		node.set(SkillSetting.DURATION.node(), 16000);
		node.set(SkillSetting.PERIOD.node(), 2000);
		node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 0.07);
		return node;
	}
	
	public String getDescription(Hero hero)
	{
		double healthRestored = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING, 15, true);
		healthRestored += SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM, 0.07, true) *
			hero.getAttributeValue(AttributeType.WISDOM);
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 16000, false);
        long radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 12, false);
        
		return getDescription().replace("$1", healthRestored + "")
				.replace("$2", String.valueOf(period / 1000))
				.replace("$3", String.valueOf(duration / 1000))
				.replace("$4", radius + "");
	}
	
    public SkillResult use(Hero hero, String[] args) 
    {
        Player player = hero.getPlayer();

        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 16000, false);
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 12, false);

		double healthRestoreTick = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING, 15, true);
		healthRestoreTick += SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM, 0.07, true) *
				hero.getAttributeValue(AttributeType.WISDOM);
        
		if (hero.hasParty()) 
		{
			int radiusSquared = radius * radius;
			Location playerLocation = player.getLocation();
			for (Hero partyMember : hero.getParty().getMembers()) 
			{
				Location memberLocation = partyMember.getPlayer().getLocation();
				if (memberLocation.getWorld().equals(playerLocation.getWorld()))
					if (memberLocation.distanceSquared(playerLocation) <= radiusSquared)
						partyMember.addEffect(new HealingSpringEffect(this, period, duration, healthRestoreTick, player));
			}
		}
		else
			hero.addEffect(new HealingSpringEffect(this, period, duration, healthRestoreTick, player));

        broadcastExecuteText(hero);
        
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_WATER_AMBIENT, 2.0F, 1.0F);

        return SkillResult.NORMAL;
    }

	public class HealingSpringEffect extends PeriodicHealEffect 
	{
		private final Player applier;

		public HealingSpringEffect(Skill skill, int period, int duration, double healPerTick, Player applier)
		{
			super(skill, "HealingSpring", applier, period, duration, healPerTick);

			this.applier = applier;

			types.add(EffectType.BENEFICIAL);
			types.add(EffectType.MAGIC);
		}

		@Override
		public void applyToHero(Hero hero) 
		{
			super.applyToHero(hero);

			Player player = hero.getPlayer();
			final Player p = player;

			if (player == applier)
			{
				new BukkitRunnable() 
				{ 
					private int effectTicks = 0;
					private int maxEffectTicks = 64;

					public void run() 
					{
						Location location = p.getLocation().add(0, 0.5, 0);
						if (effectTicks < maxEffectTicks) 
						{
							//p.getWorld().spigot().playEffect(location, Effect.SPLASH, 0, 0, 1.6F, 0.3F, 1.6F, 0.0F, 25, 16);
							p.getWorld().spawnParticle(Particle.WATER_SPLASH, location, 25, 1.6, 0.3, 1.6, 0);
							//p.getWorld().spigot().playEffect(location, Effect.SPLASH, 0, 0, 0.3F, 3.0F, 0.3F, 0.0F, 25, 16);
							p.getWorld().spawnParticle(Particle.WATER_SPLASH, location, 25, 0.3, 3, 0.3, 0);
							//p.getWorld().spigot().playEffect(location.add(0, 3.0F, 0), Effect.SPLASH, 0, 0, 0.7F, 1.0F, 0.7F, 0.0F, 25, 16);
							p.getWorld().spawnParticle(Particle.WATER_SPLASH, location, 25, 0.7, 1, 0.7, 0);
							//p.getWorld().spigot().playEffect(location.add(0, 1.0F, 0), Effect.SPLASH, 0, 0, 1.4F, 1.0F, 1.4F, 0.0F, 25, 16);
							p.getWorld().spawnParticle(Particle.WATER_SPLASH, location, 25, 1.4, 1, 1.4, 0);
							effectTicks++;
						} 
						else 
						{
							cancel();
						}
					}
				}.runTaskTimer(plugin, 1, 5);
				
				new BukkitRunnable() // sound
				{ 
					private int effectTicks = 0;

					public void run() 
					{
						Location location = p.getLocation();
						if (effectTicks < 16) 
						{
							p.getWorld().playSound(location, Sound.BLOCK_WATER_AMBIENT, 1.2F, 1.0F);
							effectTicks++;
						} 
						else 
						{
							cancel();
						}
					}
				}.runTaskTimer(plugin, 0, 20);
			}

			player.sendMessage(" " + ChatColor.WHITE + plugin.getCharacterManager().getHero(applier).getName() + ChatColor.GRAY + "'s " + ChatColor.WHITE + "Healing Spring" + ChatColor.GRAY + " soothes your injuries.");
		}
		
		public void removeFromHero(Hero hero) 
		{
			super.removeFromHero(hero);
			Player player = hero.getPlayer();
			player.sendMessage(" The Healing Spring has dried up.");
		}
		
		public void tickHero(Hero hero) 
		{
			super.tickHero(hero);
		}

		public void tickMonster(Monster monster) {}
	}
}
