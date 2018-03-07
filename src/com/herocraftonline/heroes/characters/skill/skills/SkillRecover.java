package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.util.GeometryUtil;
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
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillRecover extends TargettedSkill
{
	public SkillRecover(Heroes plugin) 
	{
		super(plugin, "Recover");
		setDescription("You heal your target (within 8 blocks) for $1 health.");
		setUsage("/skill recover");
		setArgumentRange(0, 1);
		setTypes(SkillType.SILENCEABLE, SkillType.HEALING, SkillType.ABILITY_PROPERTY_LIGHT);
		setIdentifiers("skill recover");
	}

	@Override
	public ConfigurationSection getDefaultConfig() 
	{
		ConfigurationSection node = super.getDefaultConfig();
		node.set("healing", 30);
		node.set("healing-increase-per-wisdom", 1);
		node.set(SkillSetting.MAX_DISTANCE.node(), 8);
		return node;
	}
	
	public String getDescription(Hero hero)
	{
		double healAmount = SkillConfigManager.getUseSetting(hero, this, "healing", 30, false);
		healAmount += SkillConfigManager.getUseSetting(hero, this, "healing-increase-per-wisdom", 1, false)
				* hero.getAttributeValue(AttributeType.WISDOM);
		
		return getDescription().replace("$1", healAmount + "");
	}

	@Override
	public SkillResult use(Hero hero, LivingEntity target, String[] args) 
	{
		Player player = hero.getPlayer();
		
		if (!(target instanceof Player)) return SkillResult.INVALID_TARGET;
		
		Hero tHero = plugin.getCharacterManager().getHero((Player)target);
		
		double healAmount = SkillConfigManager.getUseSetting(hero, this, "healing", 30, false);
		healAmount += SkillConfigManager.getUseSetting(hero, this, "healing-increase-per-wisdom", 1, false)
				* hero.getAttributeValue(AttributeType.WISDOM);
		
		if (target == player) healAmount *= 2;
		
        double targetHealth = target.getHealth();
        if (targetHealth >= target.getMaxHealth()) 
        {
            if (player.equals(tHero.getPlayer())) 
            {
                player.sendMessage(" You are already at full health!");
            }
            else {
                player.sendMessage(" Target is at full health!");
            }
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        
		HeroRegainHealthEvent hrh = new HeroRegainHealthEvent(tHero, healAmount, this, hero);
		
        plugin.getServer().getPluginManager().callEvent(hrh);
        if (hrh.isCancelled()) 
        {
            player.sendMessage(" Unable to heal target.");
            return SkillResult.CANCELLED;
        }

        tHero.heal(healAmount);
		
		tHero.getPlayer().getWorld().playSound(tHero.getPlayer().getLocation(), Sound.ENTITY_FIREWORK_LAUNCH, 1.0F, 1.0F);
		tHero.getPlayer().getWorld().playSound(tHero.getPlayer().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 0.8F);
		for (Location l : GeometryUtil.helix(tHero.getPlayer().getLocation(), 20, 1.5, 2.5, 0.05)) {
			l.getWorld().spigot().playEffect(l, Effect.FIREWORKS_SPARK, 0, 0, 0.0f, 0.0f, 0.0f, 0.0f, 1, 128);
		}
		tHero.getPlayer().getWorld().spigot().playEffect(tHero.getPlayer().getLocation().add(0, 0.5, 0), Effect.HAPPY_VILLAGER, 0, 0, 0.5F, 0.5F, 0.5F, 0.5F, 50, 16);
		tHero.getPlayer().getWorld().spigot().playEffect(tHero.getPlayer().getLocation().add(0, 0.5, 0), Effect.HEART, 0, 0, 0.5F, 0.5F, 0.5F, 0.5F, 50, 16);
		broadcastExecuteText(hero, target);
		return SkillResult.NORMAL;
	}
}