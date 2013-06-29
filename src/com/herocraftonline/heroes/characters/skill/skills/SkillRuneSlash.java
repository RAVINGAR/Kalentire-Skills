package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.skills.absorbrunes.RuneExpireEvent;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillRuneSlash extends TargettedSkill {
	// Default skill values
	private final double defDamageMultiplier = 0.75;
	private final int defRadius = 3;							// Default radius of cleave effect
	private final int defMaxDistance = 5;

	// Default text values
	private final String skillText = "�7[�2Skill�7] ";			// Used to add "[Skill]" text to all skill related messages
	private final String defFailText = skillText + "You must be holding a weapon to use this ability!";

	public SkillRuneSlash(Heroes plugin) {
		// Heroes stuff
		super(plugin, "RuneSlash");
		setDescription("Unleash the power imbued within your blade, slashing your enemies at a distance. Deals $1% damage, and applies your next Rune to all damaged targets. Combusts all remaining Runes on use.");
		setUsage("/skill runeslash");
		setIdentifiers("skill runeslash");
		setTypes(SkillType.HARMFUL, SkillType.DAMAGING, SkillType.SILENCABLE);
		setArgumentRange(0, 0);
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set("weapons", Util.swords);
		node.set("fail-text", defFailText);
		node.set("damage-multiplier", defDamageMultiplier);
		node.set(SkillSetting.MAX_DISTANCE.node(), defMaxDistance);

		return node;
	}

	@Override
	public String getDescription(Hero hero) {
		double damageMultiplier = SkillConfigManager.getUseSetting(hero, this, "damage-multiplier", defDamageMultiplier, false);
		return getDescription().replace("$1", (int) (damageMultiplier * 100) + "");
	}

	@Override
	public SkillResult use(Hero hero, LivingEntity target, String[] args) {
		// Ensure they have a weapon in hand
		Player player = hero.getPlayer();
		Material item = player.getItemInHand().getType();
		;
		if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.swords).contains(item.name())) {
			// Notify them that they don't have a shovel equipped
			String failText = SkillConfigManager.getUseSetting(hero, this, "fail-text", defFailText);
			Messaging.send(player, failText, new Object[0]);

			return SkillResult.FAIL;
		}

		// Get damage value
		double damagePercent = SkillConfigManager.getUseSetting(hero, this, "damage-multiplier", defDamageMultiplier, false);
		int damage = (int) damagePercent * plugin.getDamageManager().getItemDamage(item, player);

		// Get maximum radius
		int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, defRadius, false);

		// Add the cleaving effect to the hero so that the hosting class knows not to expire Runes.
		hero.addEffect(new RuneExpirationImmunityEffect(this, "RuneExpirationImmunityEffect"));

		// Find targets (if any)
		boolean hit = false;
		for (Entity entity : target.getNearbyEntities(radius, radius, radius)) {
			// Check to see if the entity can be damaged
			if (!(entity instanceof LivingEntity) || !damageCheck(player, (LivingEntity) entity))
				continue;

			// Damage the target
			addSpellTarget(target, hero);
			damageEntity((LivingEntity) entity, player, damage, DamageCause.ENTITY_ATTACK);

			hit = true;
		}

		// Remove the cleaving effect so that he can expire Runes once more.
		RuneExpirationImmunityEffect reiEffect = (RuneExpirationImmunityEffect) hero.getEffect("RuneExpirationImmunityEffect");
		hero.removeEffect(reiEffect);

		// Expire a all of the Runeblade's Runes from the player's Rune list, but only if he hit a target
		if (hit == true)
			Bukkit.getServer().getPluginManager().callEvent(new RuneExpireEvent(hero, 99));

		// Play Sound
		hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.IRONGOLEM_THROW, 0.5F, 1.0F);

		// Let the world know that the hero has activated a Rune.
		broadcastExecuteText(hero);

		return SkillResult.NORMAL;
	}

	// Effect required for telling the hosting class not to expire runes before hitting all targets
	private class RuneExpirationImmunityEffect extends Effect {
		public RuneExpirationImmunityEffect(Skill skill, String name) {
			super(skill, name);
		}
	}
}
