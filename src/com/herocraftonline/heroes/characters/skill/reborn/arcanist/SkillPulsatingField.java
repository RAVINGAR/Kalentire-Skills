package com.herocraftonline.heroes.characters.skill.reborn.arcanist;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseSphere;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillPulsatingField extends SkillBaseSphere {

	public SkillPulsatingField(Heroes plugin) {
		super(plugin, "PulsatingField");
		setDescription("Call upon the forces of the arcane to damage enemies within $1 blocks for $2 every $3 seconds for $4 second(s).");
		setUsage("/skill pulsatingfield");
		setIdentifiers("skill pulsatingfield");
		setArgumentRange(0, 0);
		setTypes(SkillType.AREA_OF_EFFECT, SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_MAGICAL);
	}

	@Override
	public String getDescription(Hero hero) {
		final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5d, false);
		long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);
		long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1000, false);

		final double damageTick = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 100d, false)
				+ SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 2d, false) * hero.getAttributeValue(AttributeType.INTELLECT);

		return getDescription()
				.replace("$1", Util.decFormat.format(radius))
				.replace("$2", Util.decFormat.format(damageTick))
				.replace("$3", Util.decFormat.format((double) period / 1000))
				.replace("$4", Util.decFormat.format((double) duration / 1000));
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection config = super.getDefaultConfig();
		config.set(SkillSetting.RADIUS.node(), 5d);
		config.set(SkillSetting.DURATION.node(), 5000);
		config.set(SkillSetting.PERIOD.node(), 1000);
		config.set(SkillSetting.DAMAGE_TICK.node(), 100d);
		config.set(SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT.node(), 1d);
		return config;
	}

	@Override
	public SkillResult use(Hero hero, String[] strings) {
		if (isAreaSphereApplied(hero)) {
			return SkillResult.INVALID_TARGET_NO_MSG;
		} else {
			broadcastExecuteText(hero);

			final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5d, false);
			long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);
			long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1000, false);

			final double damageTick = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 100d, false)
					+ SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 1d, false)
					* hero.getAttributeValue(AttributeType.INTELLECT);

			applyAreaSphereEffect(hero, period, duration, radius, new SphereActions() {

				@Override
				public void sphereTickAction(Hero hero, AreaSphereEffect effect) {
					Player player = hero.getPlayer();
					Location location = player.getLocation();
					World world = location.getWorld();
					renderSphere(player.getEyeLocation(), radius, Particle.ENCHANTMENT_TABLE);
					world.playSound(location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 0.533f);
				}

				@Override
				public void sphereTargetAction(Hero hero, Entity target) {
					Player player = hero.getPlayer();
					if (target instanceof LivingEntity && !target.equals(player)) {
						LivingEntity livingTarget = (LivingEntity) target;
						if (damageCheck(player, livingTarget)) {
							damageEntity(livingTarget, player, damageTick, EntityDamageEvent.DamageCause.MAGIC, true);
						}
					}
				}
			}, EffectType.DISPELLABLE);

			return SkillResult.NORMAL;
		}
	}
}
