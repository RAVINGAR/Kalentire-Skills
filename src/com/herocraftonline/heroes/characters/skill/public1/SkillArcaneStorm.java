package com.herocraftonline.heroes.characters.skill.public1;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.base.SkillBaseSphere;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillArcaneStorm extends SkillBaseSphere {

	public SkillArcaneStorm(Heroes plugin) {
		super(plugin, "ArcaneStorm");
		setDescription("Call upon the forces of the arcane to damage and knock back enemies within $1 blocks for $2 every $3 seconds for $4 seconds. $5 $6");
		setUsage("/skill arcanestorm");
		setIdentifiers("skill arcanestorm");
		setArgumentRange(0, 0);
		setTypes(SkillType.MULTI_GRESSIVE, SkillType.AREA_OF_EFFECT, SkillType.DAMAGING, SkillType.FORCE, SkillType.NO_SELF_TARGETTING, SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_MAGICAL);
	}

	@Override
	public String getDescription(Hero hero) {
		final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5d, false);
		long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);
		long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1000, false);

		final double damageTick = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 100d, false)
				+ SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 2d, false) * hero.getAttributeValue(AttributeType.INTELLECT);

		int mana = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 0, false);
		long cooldown = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 0, false);

		return getDescription()
				.replace("$1", Util.decFormat.format(radius))
				.replace("$2", Util.decFormat.format(damageTick))
				.replace("$3", Util.decFormat.format((double) period / 1000))
				.replace("$4", Util.decFormat.format((double) duration / 1000))
				.replace("$5", mana > 0 ? "Mana: " + mana : "")
				.replace("$6", cooldown > 0 ? "C: " + Util.decFormat.format((double) cooldown / 1000) : "");
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.RADIUS.node(), 5d);
		node.set(SkillSetting.DURATION.node(), 6000);
		node.set(SkillSetting.PERIOD.node(), 1000);
		node.set(SkillSetting.DAMAGE_TICK.node(), 100d);
		node.set(SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT.node(), 1d);

		return node;
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
					+ SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 2d, false) * hero.getAttributeValue(AttributeType.INTELLECT);

			applyAreaSphereEffect(hero, period, duration, radius, new SphereActions() {

				@Override
				public void sphereTickAction(Hero hero, AreaSphereEffect effect) {
					renderSphere(hero.getPlayer().getEyeLocation(), radius, ParticleEffect.SPELL_MOB, Color.AQUA);
					hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), CompatSound.ENTITY_GENERIC_BURN.value(), 0.5f, 0.000001f);
					hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), CompatSound.ENTITY_ENDERMEN_TELEPORT.value(), 0.5f, 0.000001f);
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
