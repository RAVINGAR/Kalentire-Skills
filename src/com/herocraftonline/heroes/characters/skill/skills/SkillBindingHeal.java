package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillBindingHeal extends TargettedSkill
{
	public SkillBindingHeal(Heroes plugin) 
	{
		super(plugin, "BindingHeal");
		setDescription("Your touch heals your target (within 10 blocks) for $1 health. This skill must be used on another player.");
		setUsage("/skill bindingheal");
		setArgumentRange(0, 1);
		setTypes(SkillType.SILENCEABLE, SkillType.NO_SELF_TARGETTING, SkillType.HEALING);
		setIdentifiers("skill bindingheal", "skill bheal");
	}

	@Override
	public ConfigurationSection getDefaultConfig() 
	{
		ConfigurationSection node = super.getDefaultConfig();
		node.set("healing", 50);
		node.set("healing-increase-per-wisdom", 2);
		node.set(SkillSetting.MAX_DISTANCE.node(), 10);
		return node;
	}

	public String getDescription(Hero hero)
	{
		double healAmount = SkillConfigManager.getUseSetting(hero, this, "healing", 50, false);
		healAmount += SkillConfigManager.getUseSetting(hero, this, "healing-increase-per-wisdom", 2, false) * hero.getAttributeValue(AttributeType.WISDOM);

		return getDescription().replace("$1", healAmount + "");
	}

	@Override
	public SkillResult use(Hero hero, LivingEntity target, String[] args) 
	{
		Player player = hero.getPlayer();
		
		if (!(target instanceof Player)) return SkillResult.INVALID_TARGET;
		if ((Player)target == player) 
		{
			player.sendMessage(" You must target another player!");
			return SkillResult.INVALID_TARGET_NO_MSG;
		}

		Hero tHero = plugin.getCharacterManager().getHero((Player)target);

		double healAmount = SkillConfigManager.getUseSetting(hero, this, "healing", 50, false);
		healAmount += SkillConfigManager.getUseSetting(hero, this, "healing-increase-per-wisdom", 2, false) * hero.getAttributeValue(AttributeType.WISDOM);

		double targetHealth = target.getHealth();
		if (targetHealth >= target.getMaxHealth() && hero.getPlayer().getHealth() >= hero.getPlayer().getMaxHealth()) 
		{
			player.sendMessage(" You are both at full health!");
			return SkillResult.INVALID_TARGET_NO_MSG;
		}

		HeroRegainHealthEvent hrhUser = new HeroRegainHealthEvent(tHero, healAmount, this, hero);
		HeroRegainHealthEvent hrhTarget = new HeroRegainHealthEvent(hero, healAmount, this, hero);

		plugin.getServer().getPluginManager().callEvent(hrhUser);
		plugin.getServer().getPluginManager().callEvent(hrhTarget);
		if (hrhUser.isCancelled() || hrhTarget.isCancelled()) 
		{
			player.sendMessage(" Unable to heal target.");
			return SkillResult.CANCELLED;
		}

		hero.heal(healAmount);
		tHero.heal(healAmount);

		hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.2F, 0.75F);
		hero.getPlayer().getWorld().spigot().playEffect(hero.getPlayer().getLocation(), Effect.FIREWORKS_SPARK, 0, 0, 1.5F, 6.5F, 1.5F, 0.5F, 150, 16);
		tHero.getPlayer().getWorld().playSound(tHero.getPlayer().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.2F, 0.75F);
		tHero.getPlayer().getWorld().spigot().playEffect(tHero.getPlayer().getLocation(), Effect.FIREWORKS_SPARK, 0, 0, 1.5F, 6.5F, 1.5F, 0.5F, 150, 16);
		broadcastExecuteText(hero, target);
		return SkillResult.NORMAL;
	}
}