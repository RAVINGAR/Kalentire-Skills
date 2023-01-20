package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.herocraftonline.heroes.util.GeometryUtil.circle;

public class SkillSummonVines extends ActiveSkill
{
	public SkillSummonVines(Heroes plugin) 
	{
		super(plugin, "SummonVines");
		setDescription("For $1 seconds, you raise a field of thorny vines to impede and damage enemies. Enemies within $2 blocks of you will take $3 damage every $4 seconds and be slowed for $4 second(s).");
		setUsage("/skill summonvines");
		setArgumentRange(0, 0);
		setIdentifiers("skill summonvines");
		setTypes(SkillType.DAMAGING, SkillType.DEBUFFING, SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT);
	}

	@Override
	public String getDescription(Hero hero) 
	{
		int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 8, false);
		int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 3000, false);
		int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 12000, false);

		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 15, false);
		double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.2, false);
		damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

		String formattedDuration = String.valueOf(duration / 1000);
		String formattedPeriod = String.valueOf(period / 1000);

		return getDescription().replace("$1", formattedDuration).replace("$2", radius + "").replace("$3", damage + "").replace("$4", formattedPeriod);
	}

	@Override
	public ConfigurationSection getDefaultConfig() 
	{
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.DAMAGE.node(), 15);
		node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.2);
		node.set(SkillSetting.RADIUS.node(), 8);
		node.set(SkillSetting.PERIOD.node(), 3000);
		node.set(SkillSetting.DURATION.node(), 12000);

		return node;
	}

	@Override
	public SkillResult use(Hero hero, String[] args) 
	{
		Player player = hero.getPlayer();

		int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 3000, false);
		int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 12000, false);

		hero.addEffect(new VinesEffect(this, player, duration, period));
		broadcastExecuteText(hero);

		return SkillResult.NORMAL;
	}


	public class VinesEffect extends PeriodicExpirableEffect
	{		
		private final Skill skill;
		final ArrayList<Location> vineLocs = new ArrayList<>();

		public VinesEffect(Skill skill, Player applier, long duration, long period) 
		{
			super(skill, "VinesEffect", applier, period, duration);
			this.skill = skill;
		}

		public void applyToHero(Hero hero) 
		{
			super.applyToHero(hero);

			Player player = hero.getPlayer();
			
			Location pLoc = player.getLocation().add(0, 0.5, 0);

			final int radius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS, 8, false);
			
			for (int i = 0; i < radius; i++)
				vineLocs.addAll(circle(pLoc, 12, (double) i));

			new BukkitRunnable()
			{
				private int ticks = 0;
				private final int maxTicks = 40;
				private final Random rand = new Random();

				public void run()
				{
					if (ticks < maxTicks)
					{
						int index = rand.nextInt(vineLocs.size());
						Location loc = vineLocs.get(index).clone().setDirection(new Vector(0, 1, 0));
						//loc.getWorld().spigot().playEffect(loc, Effect.TILE_BREAK, Material.VINE.getId(), 0, 0.2F, 2.5F, 0.2F, 0.5F, 75, 25);
						loc.getWorld().spawnParticle(Particle.BLOCK_CRACK, loc, 75, 0.2, 2.5, 0.2, 0.5, Bukkit.createBlockData(Material.VINE));
						ticks++;
					}
					else
					{
						cancel();
					}
				}
			}.runTaskTimer(plugin, 0, 6);
			
			broadcast(player.getLocation(), (" �f%hero% �7raises a field of vines!").replace("%hero%", hero.getName()));
		}

		@Override
		public void removeFromHero(Hero hero)
		{
			super.removeFromHero(hero);
			Player player = hero.getPlayer();
			broadcast(player.getLocation(), (" �f%hero%�7's vines have withered.").replace("%hero%", hero.getName()));
		}

		public void tickHero(Hero hero)
		{
			vineTick(hero);
		}

		public void tickMonster(Monster monster) {}

		public void vineTick(Hero hero)
		{
			Player player = hero.getPlayer();
			
			final int radius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS, 8, false);

			int period = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.PERIOD, 3000, false);

			double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 15, false);
			double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.2, false);
			damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));
			
			vineLocs.clear();
			
			for (int i = 0; i < radius; i++)
				vineLocs.addAll(circle(player.getLocation().add(0, 0.5, 0), 12, (double) i));
			
			List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
			for (Entity entity : entities) 
			{
				if (!(entity instanceof LivingEntity) || entity == player)
					return;
				LivingEntity target = (LivingEntity) entity;

				if (!damageCheck(player, target))
					return;

				addSpellTarget(target, hero);
				damageEntity(target, player, damage, DamageCause.MAGIC, false);
				CharacterTemplate targCT = plugin.getCharacterManager().getCharacter(target);
				targCT.addEffect(new SlowEffect(skill, "Vined", applier, period, 2, ChatColor.WHITE + targCT.getName()
						+ ChatColor.GRAY + " has been slowed by " + ChatColor.WHITE + hero.getName() + "'s vines!",
						ChatColor.WHITE + targCT.getName() + ChatColor.GRAY + " has been released!"));
			}
		}
	}
}