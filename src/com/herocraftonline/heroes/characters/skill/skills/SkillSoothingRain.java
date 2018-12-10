package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainManaEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;


public class SkillSoothingRain extends ActiveSkill
{
	public SkillSoothingRain(Heroes plugin) 
	{
		super(plugin, "SoothingRain");
		setDescription("Calls up a soothing rain to refresh your party (within 12 blocks), granting them $1 mana every 2 seconds for 16 seconds.");
		setUsage("/skill soothingrain");
		setArgumentRange(0, 0);
		setIdentifiers("skill soothingrain", "skill rain");
		setTypes(SkillType.BUFFING);
	}
	
	public String getDescription(Hero hero)
	{
		int manaRestored = SkillConfigManager.getUseSetting(hero, this, "mana-restored", 13, false);
		manaRestored += SkillConfigManager.getUseSetting(hero, this, "mana-restored-increase-per-level", 0.05, false);
		return getDescription().replace("$1", manaRestored + "");
	}
	
    public SkillResult use(Hero hero, String[] args) 
    {
        Player player = hero.getPlayer();

        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 16000, false);
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 12, false);

        int manaRestoreTick = SkillConfigManager.getUseSetting(hero, this, "mana-restored", 13, false);
        double manaRestoreTickIncrease = SkillConfigManager.getUseSetting(hero, this, "mana-restored-increase-per-level", 0.05, false);
        manaRestoreTick += (int) (manaRestoreTickIncrease * hero.getHeroLevel(this));

        SoothingRainEffect srEffect = new SoothingRainEffect(this, hero.getPlayer(), period, duration, radius, manaRestoreTick);
        hero.addEffect(srEffect);

        broadcastExecuteText(hero);
        
        player.getWorld().playSound(player.getLocation(), Sound.WEATHER_RAIN, 2.0F, 1.0F);

        return SkillResult.NORMAL;
    }

	public class SoothingRainEffect extends PeriodicExpirableEffect 
	{
		private final int radius;
		private final int manaHealedPerTick;
		private final Player applier;

		public SoothingRainEffect(Skill skill, Player applier, int period, int duration, int radius, int manaHealedPerTick) 
		{
			super(skill, "SoothingRain", applier, period, duration);

			this.radius = radius;
			this.manaHealedPerTick = manaHealedPerTick;
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
					private double time = 0;

					@SuppressWarnings("deprecation")
					public void run() 
					{
						Location location = p.getLocation();
						if (time < 1.6) 
						{
							//p.getWorld().spigot().playEffect(location, Effect.SPLASH, 0, 0, 6.3F, 0.2F, 6.3F, 0.0F, 60, 16);
							p.getWorld().spawnParticle(Particle.WATER_SPLASH, location, 60, 6.3, 0.2, 6.3, 0);
						} 
						else 
						{
							cancel();
						}
						time += 0.01;
					}
				}.runTaskTimer(plugin, 1, 2);
				
				new BukkitRunnable() // sound
				{ 
					private int effectTicks = 0;

					@SuppressWarnings("deprecation")
					public void run() 
					{
						Location location = p.getLocation();
						if (effectTicks < 16) 
						{
							p.getWorld().playSound(p.getLocation(), Sound.WEATHER_RAIN, 1.2F, 1.0F);
							effectTicks++;
						} 
						else 
						{
							cancel();
						}
					}
				}.runTaskTimer(plugin, 0, 20);
			}

			player.sendMessage(" You are refreshed by a Soothing Rain!");
		}
		
		public void removeFromHero(Hero hero) 
		{
			super.removeFromHero(hero);
			Player player = hero.getPlayer();
			player.sendMessage(" The Soothing Rain has ended.");
		}
		
		public void tickHero(Hero hero) 
		{
			Player player = hero.getPlayer();

			if (hero.hasParty()) {
				int radiusSquared = radius * radius;
				Location playerLocation = player.getLocation();
				for (Hero partyMember : hero.getParty().getMembers()) 
				{
					Location memberLocation = partyMember.getPlayer().getLocation();
					if (memberLocation.getWorld().equals(playerLocation.getWorld())) 
					{
						if (memberLocation.distanceSquared(playerLocation) <= radiusSquared) 
						{
							if (partyMember.getMana() < partyMember.getMaxMana()) 
							{
								HeroRegainManaEvent heal = new HeroRegainManaEvent(partyMember, manaHealedPerTick, skill);
								plugin.getServer().getPluginManager().callEvent(heal);
								if (!heal.isCancelled()) 
								{
									partyMember.setMana(manaHealedPerTick + partyMember.getMana());
									//partyMember.getPlayer().getWorld().spigot().playEffect(partyMember.getPlayer().getLocation().add(0, 0.5, 0), Effect.MAGIC_CRIT, 0, 0, 0.5F, 0.5F, 0.5F, 0, 20, 16);
									partyMember.getPlayer().getWorld().spawnParticle(Particle.CRIT_MAGIC, partyMember.getPlayer().getLocation().add(0, 0.5, 0), 20, 0.5, 0.5, 0.5, 0);
								}
							}
						}
					}
				}
			}
			else 
			{
				if (hero.getMana() < hero.getMaxMana()) 
				{
					HeroRegainManaEvent heal = new HeroRegainManaEvent(hero, manaHealedPerTick, skill);
					plugin.getServer().getPluginManager().callEvent(heal);
					if (!heal.isCancelled()) {
						hero.setMana(manaHealedPerTick + hero.getMana());
						//player.getWorld().spigot().playEffect(player.getLocation().add(0, 0.5, 0), Effect.MAGIC_CRIT, 0, 0, 0.5F, 0.5F, 0.5F, 0, 20, 16);
						player.getWorld().spawnParticle(Particle.CRIT_MAGIC, player.getLocation().add(0, 0.5, 0), 20, 0.5, 0.5, 0.5, 0);
					}
				}
			}
		}

		public void tickMonster(Monster monster) {}
	}
}
