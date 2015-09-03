package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.Color;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillCallOfChaos extends SkillBaseSphere {

	public SkillCallOfChaos(Heroes plugin) {
		super(plugin, "CallOfChaos");
		setDescription("");
		setUsage("/skill callofchaos");
		setIdentifiers("skill callofchaos");
		setArgumentRange(0, 0);
	}

	@Override
	public String getDescription(Hero hero) {
		return getDescription();
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
					renderSphere(hero.getPlayer().getEyeLocation(), radius, ParticleEffect.REDSTONE, Color.WHITE);
					hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.FIREWORK_TWINKLE, 0.25f, 2);
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
			});

			return SkillResult.NORMAL;
		}
	}
}
