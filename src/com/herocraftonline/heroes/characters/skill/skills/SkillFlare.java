package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class SkillFlare extends ActiveSkill
{
	private ArrayList<Player> Flares = new ArrayList<Player>();
	
	public SkillFlare(Heroes plugin)
	{
		super(plugin, "Flare");
		setDescription("You summon a powerful Flare for 6 seconds that rains $1 bolts of flame down over a radius of $2 blocks. Each bolt deals $4 damage to any target within $5 blocks and ignites them for $6 second(s).");
		setUsage("/skill Flare");
		setArgumentRange(0, 0);
		setIdentifiers("skill Flare");
		setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_FIRE);
	}

	public String getDescription(Hero hero)
	{
		int firebolts = SkillConfigManager.getUseSetting(hero, this, "firebolts", 30, false);
		firebolts += SkillConfigManager.getUseSetting(hero, this, "firebolts-per-level", 2, false) * hero.getHeroLevel(this);
		int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 12, false);
		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 20, false);
		damage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.3, false) * hero.getAttributeValue(AttributeType.INTELLECT);
		double blastRadius = SkillConfigManager.getUseSetting(hero, this, "blast-radius", 2, false);
		int ignitionSeconds = SkillConfigManager.getUseSetting(hero, this, "ignition-time", 4, false);
		return getDescription().replace("$1", firebolts + "").replace("$2", radius + "").replace("$4", damage + "").replace("$5", blastRadius + "").replace("$6", ignitionSeconds + "");
	}
	public ConfigurationSection getDefaultConfig() 
	{
		ConfigurationSection node = super.getDefaultConfig();
		node.set(SkillSetting.RADIUS.node(), 12);

		node.set(SkillSetting.DAMAGE.node(), 20);

		node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.3);

		node.set("firebolts", 30);

		node.set("firebolts-per-level", 2);

		node.set("blast-radius", 2);

		node.set("ignition-time", 4);

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

	@SuppressWarnings("deprecation")
	public SkillResult use(Hero hero, String[] args)
	{
		final int firebolts = SkillConfigManager.getUseSetting(hero, this, "firebolts", 30, false) + ((SkillConfigManager.getUseSetting(hero, this, "firebolts-per-level", 2, false) * hero.getHeroLevel(this)));
		final int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 12, false);
		int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 20, false);
		final double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 20, false) + SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.3, false) * hero.getAttributeValue(AttributeType.INTELLECT);
		final double blastRadius = SkillConfigManager.getUseSetting(hero, this, "blast-radius", 2, false);
		final int ignitionSeconds = SkillConfigManager.getUseSetting(hero, this, "ignition-time", 4, false);
		
		int boltsPerSec = firebolts / 6;
		int boltInterval = 20 / boltsPerSec;

		ArrayList<Location> boltLocs = new ArrayList<Location>();

		Player player = hero.getPlayer();

		//FIXME data use
//		Block targetBlock = player.getTargetBlock((HashSet<Byte>) null, distance);
//		Location center = targetBlock.getLocation().clone().add(0, 10, 0);
//
//		for (int i = 1; i <= radius; i++)
//		{
//			ArrayList<Location> concentric = circle(center, 12, (double) i);
//			boltLocs.addAll(concentric);
//		}
//
//		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0F, 0.5F);
//
//		final ArrayList<Location> finalLocs = boltLocs;
//		final Player p = player;
//		final Hero h = hero;
//
//		Flares.add(p);
//
//		new BukkitRunnable() // visual
//		{
//			public void run()
//			{
//				if (!Flares.contains(p)) cancel();
//				for (Location l : finalLocs)
//				{
//					//l.getWorld().spigot().playEffect(l, Effect.EXPLOSION_LARGE, 0, 0, 1.0F, 1.0F, 1.0F, 0.0F, 1, 250);
//					l.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, l, 1, 1, 1, 1, 0, true);
//				}
//			}
//		}.runTaskTimer(plugin, 0, 5);
//
//		new BukkitRunnable() // This is the actual storm.
//		{
//			private Random rand = new Random();
//			private int boltsStruck = 0;
//
//			public void run()
//			{
//				if (boltsStruck < firebolts)
//				{
//					int index = rand.nextInt(finalLocs.size());
//					Location boltLocation = finalLocs.get(index).clone().setDirection(new Vector(0, -1, 0));
//					BlockIterator iterator;
//					Block temp;
//
//					// everything here is visual
//					try
//					{
//						iterator = new BlockIterator(boltLocation, 10);
//					}
//					catch (IllegalStateException ise)
//					{
//						return;
//					}
//					while (iterator.hasNext())
//					{
//						temp = iterator.next();
//						Material tempBlockType = temp.getType();
//						if (Util.transparentBlocks.contains(tempBlockType))
//						{
//							final Location targetLocation = temp.getLocation().clone().add(new Vector(.5, 0, .5));
//							//temp.getWorld().spigot().playEffect(targetLocation, Effect.FLAME, 0, 0, 0.2F, 0.5F, 0.2F, 0.05F, 3, 250);
//							temp.getWorld().spawnParticle(Particle.FLAME, targetLocation, 3, 0.2, 0.5, 0.2, 0.05, true);
//						}
//						else
//						{
//							//temp.getWorld().spigot().playEffect(temp.getLocation().add(0.5, 0.3, 0.5), Effect.LAVA_POP, 0, 0, 0.5F, 0.2F, 0.5F, 0.0F, 10, 16);
//							temp.getWorld().spawnParticle(Particle.LAVA, temp.getLocation().add(0.5, 0.3, 0.5), 10, 0.5, 0.2, 0.5, 0);
//							//temp.getWorld().spigot().playEffect(temp.getLocation().add(0.5, 0.3, 0.5), Effect.EXPLOSION_LARGE, 0, 0, 0.5F, 0.2F, 0.5F, 0.0F, 3, 50);
//							temp.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, temp.getLocation().add(0.5, 0.3, 0.5), 3, 0.5, 0.2, 0.5, 0, true);
//							temp.getWorld().playSound(temp.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.0F);
//
//							Snowball test = temp.getWorld().spawn(temp.getLocation().add(0.5, 1.0, 0.5), Snowball.class);
//							List<Entity> affected = test.getNearbyEntities(blastRadius, blastRadius, blastRadius);
//							test.remove();
//							for (Entity entity : affected)
//							{
//								if (!(entity instanceof LivingEntity)) return;
//								LivingEntity target2 = (LivingEntity) entity;
//
//								if (entity.getLocation().distanceSquared(temp.getLocation()) <= blastRadius*2)
//								{
//									if (!damageCheck(p, target2))
//									{
//										continue;
//									}
//									addSpellTarget(target2, h);
//									damageEntity(target2, p, damage, EntityDamageEvent.DamageCause.MAGIC);
//									target2.setFireTicks(ignitionSeconds * 20);
//								}
//							}
//							boltsStruck++;
//							break;
//						}
//					}
//				}
//				else
//				{
//					Flares.remove(p);
//					cancel();
//				}
//			}
//		}.runTaskTimer(plugin, 1, boltInterval);
//
//		broadcast(player.getLocation(), ChatComponents.GENERIC_SKILL + ChatColor.WHITE + hero.getName() + ChatColor.GRAY + " unleashes a Flare!");

		return SkillResult.NORMAL;
	}
}

