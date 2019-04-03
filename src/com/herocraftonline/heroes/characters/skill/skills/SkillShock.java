package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.StunEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.herocraftonline.heroes.util.GeometryUtil.circle;

public class SkillShock extends ActiveSkill 
{
	public SkillShock(Heroes plugin) 
	{
		super(plugin, "Shock");
		setDescription("Shocks all targets within $1 blocks, dealing $2 damage. Has a $3% chance to stun them for $4 second(s).");
		setUsage("/skill shock");
		setArgumentRange(0, 0);
		setIdentifiers("skill shock");
		setTypes(SkillType.DAMAGING);
	}

	@Override
	public String getDescription(Hero hero) 
	{
		int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 4, false);

		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 20, false);
		double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.3, false);
		damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

		String formattedDamage = Util.decFormat.format(damage);

		long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 1000, false);
		String formattedDuration = String.valueOf(duration / 1000);

		int stunChance = SkillConfigManager.getUseSetting(hero, this, "stun-chance-percent", 20, true);

		return getDescription().replace("$1", radius + "").replace("$2", formattedDamage).replace("$3", stunChance + "").replace("$4", formattedDuration);
	}

	@Override
	public ConfigurationSection getDefaultConfig() 
	{
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.DAMAGE.node(), 20);
		node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.3);
		node.set(SkillSetting.RADIUS.node(), Integer.valueOf(4));
		node.set(SkillSetting.DURATION.node(), 1000);
		node.set("stun-chance-percent", 20);

		return node;
	}

	@Override
	public SkillResult use(Hero hero, String[] args) 
	{
		final Player player = hero.getPlayer();

		final int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 4, false);

		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 20, false);
		double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.3, false);
		damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

		long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 1000, false);

		int stunChance = SkillConfigManager.getUseSetting(hero, this, "stun-chance-percent", 20, true);


		new BukkitRunnable()
		{
			double rad = 0.5;
			public void run()
			{
				if (rad < radius)
				{
					List<Location> aCircle = circle(player.getLocation().add(0, 0.5, 0), 36, rad);
					for (Location l : aCircle)
					{
						//l.getWorld().spigot().playEffect(l, Effect.VILLAGER_THUNDERCLOUD, 0, 0, 0.1F, 0.1F, 0.1F, 0.0F, 1, 16);
						l.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, l, 1, 0.1, 0.1, 0.1, 0);
					}
					rad += 0.5;
				}
				else
				{
					cancel();
				}
			}
		}.runTaskTimer(plugin, 0, 2);
		
		List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
		for (Entity entity : entities) 
		{
			if (!(entity instanceof LivingEntity)) 
			{
				continue;
			}

			if (!damageCheck(player, (LivingEntity) entity)) {
				continue;
			}

			LivingEntity target = (LivingEntity) entity;
			CharacterTemplate targCT = plugin.getCharacterManager().getCharacter(target);
			StunEffect stun = new StunEffect(this, player, duration);
			Random random = new Random();
			if (random.nextInt(101) <= stunChance) targCT.addEffect(stun);

			addSpellTarget(target, hero);
			damageEntity(target, player, damage, DamageCause.MAGIC, false);

			//target.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), Effect.EXPLOSION_LARGE, 0, 0, 0.3F, 0.3F, 0.3F, 0.0F, 3, 16);
			target.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, target.getLocation().add(0, 0.5, 0), 3, 0.3, 0.3, 0.3, 0);
		}

		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_TNT_PRIMED, 2.0F, 0.7F);
		player.getWorld().spigot().strikeLightningEffect(player.getLocation(), true);

		broadcastExecuteText(hero);

		return SkillResult.NORMAL;
	}
}