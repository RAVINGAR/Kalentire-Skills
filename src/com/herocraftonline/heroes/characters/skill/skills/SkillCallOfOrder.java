package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillCallOfOrder extends SkillBaseSphere {

	public SkillCallOfOrder(Heroes plugin) {
		super(plugin, "CallOfOrder");
		setDescription("");
		setUsage("/skill calloforder");
		setIdentifiers("skill calloforder");
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
		node.set(SkillSetting.HEALING_TICK.node(), 100d);
		node.set(SkillSetting.HEALING_TICK_INCREASE_PER_WISDOM.node(), 1d);

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

			final double healTick = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_TICK, 100d, false)
					+ SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_TICK_INCREASE_PER_WISDOM, 2d, false) * hero.getAttributeValue(AttributeType.WISDOM);

			applyAreaSphereEffect(hero, period, duration, radius, new SphereActions() {

				@Override
				public void sphereTickAction(Hero hero, AreaSphereEffect effect) {
					renderSphere(hero.getPlayer().getEyeLocation(), radius, ParticleEffect.PORTAL);
					hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.COW_HURT, 0.25f, 0.00001f);
				}

				@Override
				public void sphereTargetAction(Hero hero, Entity target) {
					if (target instanceof Player) {
						Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
						if (hero.hasParty() && hero.getParty().isPartyMember(targetHero)) {
							targetHero.heal(healTick);
						}
					}
				}
			});

			return SkillResult.NORMAL;
		}
	}
}
