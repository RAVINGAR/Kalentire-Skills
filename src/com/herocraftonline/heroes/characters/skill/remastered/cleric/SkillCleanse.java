package com.herocraftonline.heroes.characters.skill.remastered.cleric;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.SpeedEffect;
import com.herocraftonline.heroes.characters.effects.common.interfaces.Burning;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillCleanse extends TargettedSkill {

	public SkillCleanse(Heroes plugin) {
		super(plugin, "Cleanse");
		setDescription("Cleanse your target of any negative effects and briefly grant them movement speed.");
		setUsage("/skill cleanse");
		setArgumentRange(0, 0);
		setIdentifiers("skill cleanse");
		setTypes(SkillType.ABILITY_PROPERTY_LIGHT, SkillType.SILENCEABLE, SkillType.DISPELLING);
	}

	@Override
	public String getDescription(Hero hero) {
		return getDescription();
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection config = super.getDefaultConfig();
		config.set("speed-amplifier", 1);
		config.set("speed-duration", 2000);
		config.set("dispel-fire", true);
		return config;
	}

	@Override
	public SkillResult use(Hero hero, LivingEntity target, String[] args) {
		if (!(target instanceof Player))
			return SkillResult.INVALID_TARGET_NO_MSG;

		Player player = hero.getPlayer();
		Hero targetHero = plugin.getCharacterManager().getHero((Player) target);

		int speedAmplifier = SkillConfigManager.getUseSettingInt(hero, this, "speed-amplifier", false);
		int speedDuration = SkillConfigManager.getUseSettingInt(hero, this, "speed-duration", false);
		boolean dispelFire = SkillConfigManager.getUseSetting(hero, this, "dispel-fire", true);

		boolean dispelled = false;

		// Dispel fire
		if (dispelFire) {
			for (Effect effect : targetHero.getEffects()) {
				if (effect instanceof Burning) {
					effect.removeFromHero(hero);
					dispelled = true;
				}
			}

			if (targetHero.getPlayer().getFireTicks() > 0) {
				dispelled = true;
				targetHero.getPlayer().setFireTicks(0);
			}
		}

		// Dispel negative effects
		for (Effect effect : targetHero.getEffects()) {
			if (effect.isType(EffectType.HARMFUL) && effect.isType(EffectType.DISPELLABLE)) {
				dispelled = true;
				targetHero.removeEffect(effect);
			}
		}

		// Apply short speed boost (e.g. to get out of stuns etc.)
		if (speedAmplifier >= 0 && speedDuration > 0) {
			targetHero.addEffect(new SpeedEffect(this, player, speedDuration, speedAmplifier));
		}

		// Run sound and announce skill use only if there was something to dispel and a speed to apply
		if (dispelled && speedAmplifier >= 0) {
			broadcastExecuteText(hero, target);
			target.getWorld().playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.0F);
			return SkillResult.NORMAL;
		} else {
			player.sendMessage("Your target has nothing to dispel!");
			return SkillResult.INVALID_TARGET_NO_MSG;
		}
	}
}
