package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SkillCyclone extends ActiveSkill 
{

	public SkillCyclone(Heroes plugin) 
	{
		super(plugin, "Cyclone");
		setDescription("You summon a powerful cyclone that deals $1 damage to all enemies within $2 blocks and slows them for $3 second(s).");
		setUsage("/skill cyclone");
		setArgumentRange(0, 0);
		setIdentifiers("skill cyclone");
		setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_WATER, SkillType.SILENCEABLE);
	}

	@Override
	public String getDescription(Hero hero) 
	{
		int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 4, false);

		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 75, false);
		double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.5, false);
		damage += (damageIncrease * hero.getLevel());

		String formattedDamage = Util.decFormat.format(damage);
		
		long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);
		String formattedDuration = String.valueOf(duration / 1000);

		return getDescription().replace("$2", radius + "").replace("$1", formattedDamage).replace("$3", formattedDuration);
	}

	@Override
	public ConfigurationSection getDefaultConfig() 
	{
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.DAMAGE.node(), 75);
		node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.5);
		node.set(SkillSetting.RADIUS.node(), Integer.valueOf(4));
		node.set(SkillSetting.DURATION.node(), 6000);

		return node;
	}

	public ArrayList<Location> circle(Location centerPoint, int particleAmount, double circleRadius)
	{
		World world = centerPoint.getWorld();

		double increment = (2 * Math.PI) / particleAmount;

		ArrayList<Location> locations = new ArrayList<Location>();

		for (int i = 0; i < particleAmount; i++)
		{
			double angle = i * increment;
			double x = centerPoint.getX() + (circleRadius * Math.cos(angle));
			double z = centerPoint.getZ() + (circleRadius * Math.sin(angle));
			locations.add(new Location(world, x, centerPoint.getY(), z));
		}
		
		return locations;
	}

	@Override
	public SkillResult use(Hero hero, String[] args) 
	{
		final Player player = hero.getPlayer();

		final int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 4, false);

		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 75, false);
		double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.5, false);
		damage += (damageIncrease * hero.getHeroLevel());
		
		final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);

		List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
		for (Entity entity : entities) 
		{
			if (!(entity instanceof LivingEntity))
				continue;

			if (!damageCheck(player, (LivingEntity) entity))
				continue;

			LivingEntity target = (LivingEntity) entity;
			CharacterTemplate targCT = plugin.getCharacterManager().getCharacter(target);
			SlowEffect slow = new SlowEffect(this, "CycloneSlow", player, duration, 0, "", "");
			targCT.addEffect(slow);

			addSpellTarget(target, hero);
			damageEntity(target, player, damage, DamageCause.MAGIC, false);
			
			//target.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), Effect.SPLASH, 0, 0, 0.3F, 0.3F, 0.3F, 0.0F, 10, 16);
			target.getWorld().spawnParticle(Particle.WATER_SPLASH, target.getLocation().add(0, 0.5, 0), 10, 0.3, 0.3, 0.3, 0);
			//target.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), Effect.TILE_BREAK, Material.WATER.getId(), 0, 0.3F, 0.3F, 0.3F, 0.0F, 10, 16);
			target.getWorld().spawnParticle(Particle.BLOCK_CRACK, target.getLocation().add(0, 0.5, 0), 10, 0.3, 0.3, 0.3, 0, Bukkit.createBlockData(Material.WATER));
			target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 1.0F, 1.0F);
		}
		
		new BukkitRunnable() // This is the visual effect, should iterate though all points in a circle. Should, I say.
		{
			int point = 0;
	
			int maxTicks = (int) ((duration/1000)*(20/5)); // every 2 ticks means this runs 10 times a second, a total of 10 times a second for 6 seconds = 60 ticks
			int ticks = 0;
			
			public void run()
			{
				ArrayList<Location> surrounding = circle(player.getLocation().add(0, 0.5, 0), 24, radius); // This is down here to make sure it updates
				if (point < surrounding.size()) // making sure we're staying within index boundaries
				{
					//player.getWorld().spigot().playEffect(surrounding.get(point), Effect.TILE_BREAK, Material.WATER.getId(), 0, 1.2F, 1.2F, 1.2F, 0.0F, 3,16);
					player.getWorld().spawnParticle(Particle.BLOCK_CRACK, surrounding.get(point), 3, 1.2, 1.2, 1.2, 0, Bukkit.createBlockData(Material.WATER));
					//player.getWorld().spigot().playEffect(surrounding.get(point), Effect.SPLASH, 0, 0, 1.2F, 1.2F, 1.2F, 0.0F, 10, 16);
					player.getWorld().spawnParticle(Particle.WATER_SPLASH, surrounding.get(point), 10, 1.2, 1.2, 1.2, 0);
					player.getWorld().playSound(player.getLocation(), Sound.WEATHER_RAIN, 0.5F, 0.7F);
					point++; // next point
				}
				else
				{
					point = 0; // reset the circle
					//player.getWorld().spigot().playEffect(surrounding.get(point), Effect.TILE_BREAK, Material.WATER.getId(), 0, 1.2F, 1.2F, 1.2F, 0.0F, 3,16);
					player.getWorld().spawnParticle(Particle.BLOCK_CRACK, surrounding.get(point), 3, 1.2, 1.2, 1.2, 0, Bukkit.createBlockData(Material.WATER));
					//player.getWorld().spigot().playEffect(surrounding.get(point), Effect.SPLASH, 0, 0, 1.2F, 1.2F, 1.2F, 0.0F, 10, 16);
					player.getWorld().spawnParticle(Particle.WATER_SPLASH, surrounding.get(point), 10, 1.2, 1.2, 1.2, 0);
					point++; // next point
				}
				ticks += 1;
				if (ticks >= maxTicks) // if the effect has played for 6 seconds
				{
					cancel(); // cancel the visual
				}
			}
		}.runTaskTimer(plugin, 0, 5);
		
		new BukkitRunnable() // This is the visual effect, should iterate though all points in a circle. Should, I say.
		{
			int point = 0;
			int maxTicks = 110; // every 2 ticks means this runs 10 times a second, a total of 10 times a second for 5.5 seconds = 55 ticks
			int ticks = 0;
			
			public void run()
			{
				ArrayList<Location> surrounding = circle(player.getLocation().add(0, 1, 0), 24, radius); // This is down here to make sure it updates
				if (point < surrounding.size()) // making sure we're staying within index boundaries
				{
					//player.getWorld().spigot().playEffect(surrounding.get(point), Effect.TILE_BREAK, Material.WATER.getId(), 0, 1.2F, 1.2F, 1.2F, 0.0F, 3,16);
					player.getWorld().spawnParticle(Particle.BLOCK_CRACK, surrounding.get(point), 3, 1.2, 1.2, 1.2, 0, Bukkit.createBlockData(Material.WATER));
					//player.getWorld().spigot().playEffect(surrounding.get(point), Effect.SPLASH, 0, 0, 1.2F, 1.2F, 1.2F, 0.0F, 10, 16);
					player.getWorld().spawnParticle(Particle.WATER_SPLASH, surrounding.get(point), 10, 1.2, 1.2, 1.2, 0);
					point++; // next point
				}
				else
				{
					point = 0; // reset the circle
					//player.getWorld().spigot().playEffect(surrounding.get(point), Effect.TILE_BREAK, Material.WATER.getId(), 0, 1.2F, 1.2F, 1.2F, 0.0F, 3,16);
					player.getWorld().spawnParticle(Particle.BLOCK_CRACK, surrounding.get(point), 3, 1.2, 1.2, 1.2, 0, Bukkit.createBlockData(Material.WATER));
					//player.getWorld().spigot().playEffect(surrounding.get(point), Effect.SPLASH, 0, 0, 1.2F, 1.2F, 1.2F, 0.0F, 10, 16);
					player.getWorld().spawnParticle(Particle.WATER_SPLASH, surrounding.get(point), 10, 1.2, 1.2, 1.2, 0);
					point++; // next point
				}
				ticks += 1;
				if (ticks >= maxTicks) // if the effect has played for 6 seconds
				{
					cancel(); // cancel the visual
				}
			}
		}.runTaskTimer(plugin, 5, 1);
		
		new BukkitRunnable() // This is the visual effect, should iterate though all points in a circle. Should, I say.
		{
			int point = 0;
			int maxTicks = 100; // every 2 ticks means this runs 10 times a second, a total of 10 times a second for 5 seconds = 50 ticks
			int ticks = 0;
			
			public void run()
			{
				ArrayList<Location> surrounding = circle(player.getLocation().add(0, 1.5, 0), 24, radius); // This is down here to make sure it updates
				if (point < surrounding.size()) // making sure we're staying within index boundaries
				{
					//player.getWorld().spigot().playEffect(surrounding.get(point), Effect.TILE_BREAK, Material.WATER.getId(), 0, 1.2F, 1.2F, 1.2F, 0.0F, 3,16);
					player.getWorld().spawnParticle(Particle.BLOCK_CRACK, surrounding.get(point), 3, 1.2, 1.2, 1.2, 0, Bukkit.createBlockData(Material.WATER));
					//player.getWorld().spigot().playEffect(surrounding.get(point), Effect.SPLASH, 0, 0, 1.2F, 1.2F, 1.2F, 0.0F, 10, 16);
					player.getWorld().spawnParticle(Particle.WATER_SPLASH, surrounding.get(point), 10, 1.2, 1.2, 1.2, 0);
					point++; // next point
				}
				else
				{
					point = 0; // reset the circle
					//player.getWorld().spigot().playEffect(surrounding.get(point), Effect.TILE_BREAK, Material.WATER.getId(), 0, 1.2F, 1.2F, 1.2F, 0.0F, 3,16);
					player.getWorld().spawnParticle(Particle.BLOCK_CRACK, surrounding.get(point), 3, 1.2, 1.2, 1.2, 0, Bukkit.createBlockData(Material.WATER));
					//player.getWorld().spigot().playEffect(surrounding.get(point), Effect.SPLASH, 0, 0, 1.2F, 1.2F, 1.2F, 0.0F, 10, 16);
					player.getWorld().spawnParticle(Particle.WATER_SPLASH, surrounding.get(point), 10, 1.2, 1.2, 1.2, 0);
					point++; // next point
				}
				ticks += 1;
				if (ticks >= maxTicks) // if the effect has played for 6 seconds
				{
					cancel(); // cancel the visual
				}
			}
		}.runTaskTimer(plugin, 3,1);
		
		new BukkitRunnable() // This is the visual effect, should iterate though all points in a circle. Should, I say.
		{
			int point = 0;
			int maxTicks = 90; // every 2 ticks means this runs 10 times a second, a total of 10 times a second for 4.5 seconds = 45 ticks
			int ticks = 0;
			
			public void run()
			{
				ArrayList<Location> surrounding = circle(player.getLocation().add(0, 2, 0), 24, radius); // This is down here to make sure it updates
				if (point < surrounding.size()) // making sure we're staying within index boundaries
				{
					//player.getWorld().spigot().playEffect(surrounding.get(point), Effect.TILE_BREAK, Material.WATER.getId(), 0, 1.2F, 1.2F, 1.2F, 0.0F, 3,16);
					player.getWorld().spawnParticle(Particle.BLOCK_CRACK, surrounding.get(point), 3, 1.2, 1.2, 1.2, 0, Bukkit.createBlockData(Material.WATER));
					//player.getWorld().spigot().playEffect(surrounding.get(point), Effect.SPLASH, 0, 0, 1.2F, 1.2F, 1.2F, 0.0F, 10, 16);
					player.getWorld().spawnParticle(Particle.WATER_SPLASH, surrounding.get(point), 10, 1.2, 1.2, 1.2, 0);
					point++; // next point
				}
				else
				{
					point = 0; // reset the circle
					//player.getWorld().spigot().playEffect(surrounding.get(point), Effect.TILE_BREAK, Material.WATER.getId(), 0, 1.2F, 1.2F, 1.2F, 0.0F, 3,16);
					player.getWorld().spawnParticle(Particle.BLOCK_CRACK, surrounding.get(point), 3, 1.2, 1.2, 1.2, 0, Bukkit.createBlockData(Material.WATER));
					//player.getWorld().spigot().playEffect(surrounding.get(point), Effect.SPLASH, 0, 0, 1.2F, 1.2F, 1.2F, 0.0F, 10, 16);
					player.getWorld().spawnParticle(Particle.WATER_SPLASH, surrounding.get(point), 10, 1.2, 1.2, 1.2, 0);
					point++; // next point
				}
				ticks += 1;
				if (ticks >= maxTicks) // if the effect has played for 6 seconds
				{
					cancel(); // cancel the visual
				}
			}
		}.runTaskTimer(plugin, 15, 1);
		
		new BukkitRunnable() // This is the visual effect, should iterate though all points in a circle. Should, I say.
		{
			int point = 0;
			int maxTicks = 80; // every 2 ticks means this runs 10 times a second, a total of 10 times a second for 4 seconds = 40 ticks
			int ticks = 0;
			
			public void run()
			{
				ArrayList<Location> surrounding = circle(player.getLocation().add(0, 2.5, 0), 24, radius); // This is down here to make sure it updates
				if (point < surrounding.size()) // making sure we're staying within index boundaries
				{
					//player.getWorld().spigot().playEffect(surrounding.get(point), Effect.TILE_BREAK, Material.WATER.getId(), 0, 1.2F, 1.2F, 1.2F, 0.0F, 3,16);
					player.getWorld().spawnParticle(Particle.BLOCK_CRACK, surrounding.get(point), 3, 1.2, 1.2, 1.2, 0, Bukkit.createBlockData(Material.WATER));
					//player.getWorld().spigot().playEffect(surrounding.get(point), Effect.SPLASH, 0, 0, 1.2F, 1.2F, 1.2F, 0.0F, 10, 16);
					player.getWorld().spawnParticle(Particle.WATER_SPLASH, surrounding.get(point), 10, 1.2, 1.2, 1.2, 0);
					point++; // next point
				}
				else
				{
					point = 0; // reset the circle
					//player.getWorld().spigot().playEffect(surrounding.get(point), Effect.TILE_BREAK, Material.WATER.getId(), 0, 1.2F, 1.2F, 1.2F, 0.0F, 3,16);
					player.getWorld().spawnParticle(Particle.BLOCK_CRACK, surrounding.get(point), 3, 1.2, 1.2, 1.2, 0, Bukkit.createBlockData(Material.WATER));
					//player.getWorld().spigot().playEffect(surrounding.get(point), Effect.SPLASH, 0, 0, 1.2F, 1.2F, 1.2F, 0.0F, 10, 16);
					player.getWorld().spawnParticle(Particle.WATER_SPLASH, surrounding.get(point), 10, 1.2, 1.2, 1.2, 0);
					point++; // next point
				}
				ticks += 1;
				if (ticks >= maxTicks) // if the effect has played for 6 seconds
				{
					cancel(); // cancel the visual
				}
			}
		}.runTaskTimer(plugin, 10, 1);
		
		new BukkitRunnable() // This is the visual effect, should iterate though all points in a circle. Should, I say.
		{
			int point = 0;
			int maxTicks = 70; // every 2 ticks means this runs 10 times a second, a total of 10 times a second for 3.5 seconds = 35 ticks
			int ticks = 0;
			
			public void run()
			{
				ArrayList<Location> surrounding = circle(player.getLocation().add(0, 3, 0), 24, radius); // This is down here to make sure it updates
				if (point < surrounding.size()) // making sure we're staying within index boundaries
				{
					//player.getWorld().spigot().playEffect(surrounding.get(point), Effect.TILE_BREAK, Material.WATER.getId(), 0, 1.2F, 1.2F, 1.2F, 0.0F, 3,16);
					player.getWorld().spawnParticle(Particle.BLOCK_CRACK, surrounding.get(point), 3, 1.2, 1.2, 1.2, 0, Bukkit.createBlockData(Material.WATER));
					//player.getWorld().spigot().playEffect(surrounding.get(point), Effect.SPLASH, 0, 0, 1.2F, 1.2F, 1.2F, 0.0F, 10, 16);
					player.getWorld().spawnParticle(Particle.WATER_SPLASH, surrounding.get(point), 10, 1.2, 1.2, 1.2, 0);
					point++; // next point
				}
				else
				{
					point = 0; // reset the circle
					//player.getWorld().spigot().playEffect(surrounding.get(point), Effect.TILE_BREAK, Material.WATER.getId(), 0, 1.2F, 1.2F, 1.2F, 0.0F, 3,16);
					player.getWorld().spawnParticle(Particle.BLOCK_CRACK, surrounding.get(point), 3, 1.2, 1.2, 1.2, 0, Bukkit.createBlockData(Material.WATER));
					//player.getWorld().spigot().playEffect(surrounding.get(point), Effect.SPLASH, 0, 0, 1.2F, 1.2F, 1.2F, 0.0F, 10, 16);
					player.getWorld().spawnParticle(Particle.WATER_SPLASH, surrounding.get(point), 10, 1.2, 1.2, 1.2, 0);
					point++; // next point
				}
				ticks += 1;
				if (ticks >= maxTicks) // if the effect has played for 6 seconds
				{
					cancel(); // cancel the visual
				}
			}
		}.runTaskTimer(plugin, 25, 1);
		
		new BukkitRunnable()
		{
			private int ticks = 0;
			private int maxTicks = 60;
			private Random rand = new Random();

			public void run()
			{
				ArrayList<Location> waterburstLocs = new ArrayList<Location>();
				
				for (int i = 0; i < radius; i++)
				{
					waterburstLocs.addAll(circle(player.getLocation().add(0, 0.5, 0), 12, (double) i));
				}
				
				if (ticks < maxTicks)
				{
					int index = rand.nextInt(waterburstLocs.size());
					Location loc = waterburstLocs.get(index).clone().setDirection(new Vector(0, 1, 0));
					//loc.getWorld().spigot().playEffect(loc, Effect.TILE_BREAK, Material.WATER.getId(), 0, 0.2F, 0.2F, 0.2F, 0.5F, 3, 25);
					loc.getWorld().spawnParticle(Particle.BLOCK_CRACK, loc, 3, 0.2, 0.2, 0.2, 0.5, Bukkit.createBlockData(Material.WATER), true);
					//loc.getWorld().spigot().playEffect(loc, Effect.SPLASH, 0, 0, 0.2F, 0.2F, 0.2F, 0.5F, 15, 25);
					loc.getWorld().spawnParticle(Particle.WATER_SPLASH, loc, 15, 0.2, 0.2, 0.2, 0.5, true);
					loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_SPLASH, 1.0F, 0.8F);
					ticks++;
				}
				else
				{
					cancel();
				}
			}
		}.runTaskTimer(plugin, 0, 2);
		
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 1.0F, 0.9F);
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0F, 1.3F);

		broadcastExecuteText(hero);

		return SkillResult.NORMAL;
	}
}
