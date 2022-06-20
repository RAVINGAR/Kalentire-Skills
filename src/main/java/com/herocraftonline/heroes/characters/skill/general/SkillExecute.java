package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillExecute extends TargettedSkill
{
	public SkillExecute(Heroes plugin)
	{
		super(plugin, "Execute");
		setDescription("You seek out and strike your target's weak spot, dealing $1 instant damage plus $2 additional damage per missing $3% of your target's health.");
		setUsage("/skill execute");
		setArgumentRange(0, 0);
		setIdentifiers(new String[] { "skill execute" });
		setTypes(new SkillType[] { SkillType.SILENCEABLE, SkillType.DAMAGING });
	}

	public ConfigurationSection getDefaultConfig()
	{
		ConfigurationSection node = super.getDefaultConfig();
		node.set(SkillSetting.DAMAGE.node(), 5);
		node.set(SkillSetting.DAMAGE_INCREASE_PER_DEXTERITY.node(), 0.5);
		node.set("damage-increase-per-missing-percent", 2);
		node.set("percent-scaling-interval", 5);
		return node;
	}

	public SkillResult use(Hero hero, LivingEntity target, String[] args)
	{
		Player player = hero.getPlayer();
		Hero h = hero;
		Player p = player;
		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 5, true);
		damage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_DEXTERITY, 0.5, true)
				* hero.getAttributeValue(AttributeType.DEXTERITY);
		double damagePercent = SkillConfigManager.getUseSetting(hero, this, "damage-increase-per-missing-percent", 2, true);
		double percentInterval = SkillConfigManager.getUseSetting(hero, this, "percent-scaling-interval", 5, true);
		//get missing health, divide by full value to get decimal, multiply by 100 for percent, divide by 5 for to get every 5 percent 
		double missingHealth = p.getMaxHealth()- p.getHealth();
		double mhPercent = (missingHealth / p.getMaxHealth()) * 100;
        double multiplier = mhPercent / percentInterval;
         
        double additionalDamage = damagePercent * multiplier;
       
		damage += additionalDamage;

		addSpellTarget(target, hero);
		damageEntity(target, hero.getPlayer(), damage, DamageCause.ENTITY_ATTACK, false);

		//target.getWorld().spigot().playEffect(target.getEyeLocation(), Effect.CRIT, 0, 0, 0.2F, 0.2F, 0.2F, 0.3F, 45, 16);
		target.getWorld().spawnParticle(Particle.CRIT, target.getEyeLocation(), 45, 0.2, 0.2, 0.2, 0.3);
		//target.getWorld().spigot().playEffect(target.getEyeLocation(), Effect.TILE_BREAK, Material.NETHER_WARTS.getId(), 0, 0.2F, 0.2F, 0.2F, 0.3F, 25, 16);
		target.getWorld().spawnParticle(Particle.BLOCK_CRACK, target.getEyeLocation(), 25, 0.2, 0.2, 0.2, 0.3, Bukkit.createBlockData(Material.NETHER_WART_BLOCK));
		target.getWorld().playSound(target.getEyeLocation(), Sound.ENTITY_PLAYER_HURT, 1.3F, 1.0F);

		broadcastExecuteText(hero, target);
		return SkillResult.NORMAL;
	}

	public String getDescription(Hero hero)
	{
		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 5, true);
		damage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_DEXTERITY, 0.5, true)
				* hero.getAttributeValue(AttributeType.DEXTERITY);
		double damagePercent = SkillConfigManager.getUseSetting(hero, this, "damage-increase-per-missing-percent", 2, true);
		double percentInterval = SkillConfigManager.getUseSetting(hero, this, "percent-scaling-interval", 5, true);

		return getDescription().replace("$1", damage + "").replace("$2", damagePercent + "")
				.replace("$3", percentInterval + "");
	}
}