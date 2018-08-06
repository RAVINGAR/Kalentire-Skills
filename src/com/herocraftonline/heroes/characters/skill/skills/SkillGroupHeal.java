package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Random;

import static com.herocraftonline.heroes.util.GeometryUtil.circle;

public class SkillGroupHeal extends ActiveSkill 
{

	public SkillGroupHeal(Heroes plugin) 
	{
		super(plugin, "GroupHeal");
		setDescription("Heals your party (within $1 blocks) for $2 health.");
		setUsage("/skill groupheal");
		setArgumentRange(0, 0);
		setIdentifiers("skill groupheal", "skill gheal");
		setTypes(SkillType.SILENCEABLE, SkillType.HEALING, SkillType.ABILITY_PROPERTY_LIGHT);
	}

	@Override
	public String getDescription(Hero hero) 
	{
		int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 8, false);

		double healing = SkillConfigManager.getUseSetting(hero, this, "healing", 20, false);
		healing = getScaledHealing(hero, healing);
		double healingIncrease = SkillConfigManager.getUseSetting(hero, this, "healing-increase-per-wisdom", 1, false);
		healing += (healingIncrease * hero.getAttributeValue(AttributeType.WISDOM));

		return getDescription().replace("$1", radius + "").replace("$2", ((int)healing) + "");
	}

	@Override
	public ConfigurationSection getDefaultConfig() 
	{
		ConfigurationSection node = super.getDefaultConfig();

		node.set("healing", 20);
		node.set("healing-increase-per-wisdom", 1);
		node.set(SkillSetting.RADIUS.node(), Integer.valueOf(8));

		return node;
	}

	@Override
	public SkillResult use(Hero hero, String[] args) 
	{
		Player player = hero.getPlayer();

		int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 8, false);

		double healing = SkillConfigManager.getUseSetting(hero, this, "healing", 20, false);
		healing = getScaledHealing(hero, healing);
		double healingIncrease = SkillConfigManager.getUseSetting(hero, this, "healing-increase-per-wisdom", 1, false);
		healing += (healingIncrease * hero.getAttributeValue(AttributeType.WISDOM));

		if (!hero.hasParty()) 
		{
			HeroRegainHealthEvent hrh = new HeroRegainHealthEvent(hero, healing, this, hero);
			plugin.getServer().getPluginManager().callEvent(hrh);
			hero.heal(healing);
		}
		else 
		{
			int radiusSquared = radius * radius;
			Location loc = player.getLocation();
			for (Hero mem : hero.getParty().getMembers()) 
			{
				Player memPlayer = mem.getPlayer();
				if (!memPlayer.getWorld().equals(player.getWorld())) 
				{
					continue;
				}

				if (memPlayer.getLocation().distanceSquared(loc) > radiusSquared) 
				{
					continue;
				}

				HeroRegainHealthEvent hrh = new HeroRegainHealthEvent(mem, healing, this, hero);
				plugin.getServer().getPluginManager().callEvent(hrh);
				mem.heal(healing);
			}
		}		

		player.getWorld().spigot().playEffect(player.getLocation().add(0, 0.3, 0), Effect.FIREWORKS_SPARK, 0, 0, 7.6F, 3.3F, 7.6F, 0.3F, 200, 16);

		final ArrayList<Location> particleLocations = circle(player.getLocation().add(0, 0.5, 0), 24, 8);

		new BukkitRunnable()
		{
			private int ticks = 0;
			private int maxTicks = 25;
			private Random rand = new Random();

			public void run()
			{
				if (ticks < maxTicks)
				{
					int index = rand.nextInt(particleLocations.size());
					Location l = particleLocations.get(index);
					l.getWorld().spigot().playEffect(l, Effect.HEART, 0, 0, 1.5F, 1.5F, 1.5F, 0.0F, 35, 16);
					l.getWorld().playSound(l, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3F, 1.5F);
					ticks++;
				}
				else
					cancel();
			}
		}.runTaskTimer(plugin, 0, 1);

		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERDRAGON_FLAP, 0.7F, 1.25F);
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6F, 0.8F);

		broadcastExecuteText(hero);

		return SkillResult.NORMAL;
	}
}