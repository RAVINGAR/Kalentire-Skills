package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.BloodUnionEffect;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;

public class SkillInfusion extends TargettedSkill {
	public VisualEffect fplayer = new VisualEffect();

	public SkillInfusion(Heroes plugin) {
		super(plugin, "Infusion");
		setDescription("Infuse your target with life, restoring $1 of their health and negating their bleeding. Healing is improved by $2% per level of Blood Union. This ability costs $3 health and $4 mana to use.");
		setUsage("/skill infusion <target>");
		setArgumentRange(0, 1);
		setIdentifiers("skill infusion");
		setTypes(SkillType.HEAL, SkillType.SILENCABLE, SkillType.LIGHT);
	}

	public String getDescription(Hero hero) {

		int health = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH.node(), 115, false);
		int healthcost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST.node(), 50, false);
		int manacost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA.node(), 10, false);
		int healIncrease = (int) (SkillConfigManager.getUseSetting(hero, this, "health-increase-percent-per-blood-union", 0.02, false) * 100);

		return getDescription().replace("$1", health + "").replace("$2", healIncrease + "").replace("$3", healthcost + "").replace("$4", manacost + "");
	}

	public ConfigurationSection getDefaultConfig() {

		ConfigurationSection node = super.getDefaultConfig();

		node.set("health-increase-percent-per-blood-union", 0.02);
		node.set(SkillSetting.HEALTH.node(), 10);
		node.set(SkillSetting.MANA.node(), 25);

		return node;
	}

	public SkillResult use(Hero hero, LivingEntity target, String[] args) {
		Player player = hero.getPlayer();
		if (!(target instanceof Player)) {
			return SkillResult.INVALID_TARGET;
		}

		Hero targetHero = this.plugin.getCharacterManager().getHero((Player) target);

		int targetHealth = target.getHealth();

		// Check to see if they are at full health
		if (targetHealth >= target.getMaxHealth()) {
			if (player.equals(targetHero.getPlayer()))
				Messaging.send(player, "You are already at full health.", new Object[0]);
			else {
				Messaging.send(player, "Target is already at full health.", new Object[0]);
			}

			return SkillResult.INVALID_TARGET_NO_MSG;
		}

		int healAmount = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH, 10, false);

		// Get Blood Union Level
		int bloodUnionLevel = 0;
		if (hero.hasEffect("BloodUnionEffect")) {
			BloodUnionEffect buEffect = (BloodUnionEffect) hero.getEffect("BloodUnionEffect");

			bloodUnionLevel = buEffect.getBloodUnionLevel();
		}

		// Increase healing based on blood union level
		double healIncrease = SkillConfigManager.getUseSetting(hero, this, "health-increase-percent-per-blood-union", 0.02, false);
		healIncrease = 1 + (healIncrease *= bloodUnionLevel);
		healAmount *= healIncrease;

		// Ensure they can be healed.
		HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(targetHero, healAmount, this, hero);
		this.plugin.getServer().getPluginManager().callEvent(hrhEvent);
		if (hrhEvent.isCancelled()) {
			Messaging.send(player, "Unable to heal your target at this time!", new Object[0]);
			return SkillResult.CANCELLED;
		}

		broadcastExecuteText(hero, target);

		// Heal target
		targetHero.heal(hrhEvent.getAmount());

		// Remove bleeds
		for (Effect effect : targetHero.getEffects()) {
			if (effect.isType(EffectType.BLEED)) {
				targetHero.removeEffect(effect);
			}
		}

		// Play effect
		try {
			this.fplayer.playFirework(player.getWorld(), target.getLocation().add(0.0D, 1.5D, 0.0D), FireworkEffect.builder().flicker(false).trail(false).with(FireworkEffect.Type.BURST).withColor(Color.MAROON).withFade(Color.WHITE).build());
		}
		catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return SkillResult.NORMAL;
	}
}