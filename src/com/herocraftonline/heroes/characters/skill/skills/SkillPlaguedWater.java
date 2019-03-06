package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
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

import static com.herocraftonline.heroes.util.GeometryUtil.circle;

public class SkillPlaguedWater extends ActiveSkill
{
	public SkillPlaguedWater(Heroes plugin) 
	{
		super(plugin, "PlaguedWater");
		setDescription("For $1 seconds, you release a wave of foul moisture. Enemies within $2 blocks of you will take $3 damage every $4 second(s).");
		setUsage("/skill plaguedwater");
		setArgumentRange(0, 0);
		setIdentifiers("skill plaguedwater");
		setTypes(SkillType.DAMAGING, SkillType.SILENCEABLE);
	}

	@Override
	public String getDescription(Hero hero) 
	{
		int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, Integer.valueOf(8), true);
		int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 3000, true);
		int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 12000, true);

		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 15, true);
		double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.2, true);
		damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

		String formattedDuration = String.valueOf(duration / 1000);
		String formattedPeriod = String.valueOf(period / 1000);

		return getDescription().replace("$1", formattedDuration).replace("$2", radius + "").replace("$3", damage + "").replace("$4", formattedPeriod);
	}

	@Override
	public ConfigurationSection getDefaultConfig() 
	{
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.PERIOD.node(), 3000);
		node.set(SkillSetting.DURATION.node(), 12000);

		node.set(SkillSetting.DAMAGE.node(), 5);
		node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.2);
		node.set(SkillSetting.RADIUS.node(), 8);

		return node;
	}

	@Override
	public SkillResult use(Hero hero, String[] args) 
	{
		Player player = hero.getPlayer();

		int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 3000, false);
		int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 12000, false);

		hero.addEffect(new PlaguedWaterEffect2(this, player, duration, period));
		broadcastExecuteText(hero);

		return SkillResult.NORMAL;
	}


	public class PlaguedWaterEffect2 extends PeriodicExpirableEffect
	{		
		private Skill skill;
		final ArrayList<Location> waterburstLocs = new ArrayList<Location>();

		public PlaguedWaterEffect2(Skill skill, Player applier, long duration, long period) 
		{
			super(skill, "PlaguedWaterEffect2", applier, period, duration);
			this.skill = skill;
		}

		public void applyToHero(Hero hero) 
		{
			super.applyToHero(hero);

			Player player = hero.getPlayer();
			
			final Location pLoc = player.getLocation().add(0, 0.5, 0);

			final int radius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS, Integer.valueOf(8), false);

			new BukkitRunnable()
			{
				private int ticks = 0;
				private int maxTicks = 40;
				private Random rand = new Random();

				public void run()
				{					
					for (int i = 0; i < radius; i++)
						waterburstLocs.addAll(circle(pLoc, 12, (double) i));
					
					if (ticks < maxTicks)
					{
						int index = rand.nextInt(waterburstLocs.size() + 1);
						Location loc = waterburstLocs.get(index).clone().setDirection(new Vector(0, 1, 0));
						//loc.getWorld().spigot().playEffect(loc, Effect.TILE_BREAK, Material.WATER.getId(), 0, 0.2F, 0.2F, 0.2F, 0.5F, 35, 25);
						loc.getWorld().spawnParticle(Particle.BLOCK_CRACK, loc, 35, 0.2, 0.2, 0.2, 0.5, Bukkit.createBlockData(Material.WATER), true);
						//loc.getWorld().spigot().playEffect(loc, Effect.SPLASH, 0, 0, 0.2F, 0.2F, 0.2F, 0.5F, 55, 25);
						loc.getWorld().spawnParticle(Particle.WATER_SPLASH, loc, 55, 0.2, 0.2, 0.2, 0.5, true);
						loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_SPLASH, 1.0F, 0.8F);
						ticks++;
					}
					else
						cancel();
				}
			}.runTaskTimer(plugin, 0, 6);
			
			broadcast(player.getLocation(), (" §f%hero% §7releases a wave of foul moisture!").replace("%hero%", hero.getName()));
		}

		@Override
		public void removeFromHero(Hero hero)
		{
			super.removeFromHero(hero);
			Player player = hero.getPlayer();
			broadcast(player.getLocation(), (" The air clears once more."));
		}

		public void tickHero(Hero hero)
		{
			waterBurstTick(hero);
		}

		public void tickMonster(Monster monster) {}

		public void waterBurstTick(Hero hero)
		{
			Player player = hero.getPlayer();
			
			final int radius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS, Integer.valueOf(8), true);

			int period = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.PERIOD, 3000, true);

			double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 15, true);
			double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.2, true);
			damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));
			
			waterburstLocs.clear();
			
			for (int i = 0; i < radius; i++)
				waterburstLocs.addAll(circle(player.getLocation().add(0, 0.5, 0), 8, (double) i));
			
			for (Location l : waterburstLocs)
				//l.getWorld().spigot().playEffect(l, Effect.SPLASH, 0, 0, 1.0F, 1.0F, 1.0F, 1.0F, 15, 16);
				l.getWorld().spawnParticle(Particle.WATER_SPLASH, l, 15, 1, 1, 1, 1);
			
			player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 1.0F, 1.0F);
			
			List<Entity> entities = player.getNearbyEntities(radius, radius, radius);
			for (Entity entity : entities) 
			{
				if (!(entity instanceof LivingEntity) || entity == player)
				{
					continue;
				}
				LivingEntity target = (LivingEntity) entity;

				if (!damageCheck(player, target))
				{
					continue;
				}

				addSpellTarget(target, hero);
				damageEntity(target, player, damage, DamageCause.MAGIC, false);
			}
		}
	}
}