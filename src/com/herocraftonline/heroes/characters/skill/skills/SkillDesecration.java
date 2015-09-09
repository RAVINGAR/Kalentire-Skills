package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillDesecration extends SkillBaseGroundEffect {

	private static final String SLOW_DEBUFF_DURATION_NODE = "slow-debuff-duration";
	private static final String SLOW_DEBUFF_AMPLIFIER_NODE = "slow-debuff-amplifier";

	public SkillDesecration(Heroes plugin) {
		super(plugin, "Desecration");
		setDescription("");
		setUsage("/skill desecration");
		setIdentifiers("skill desecration");
		setArgumentRange(0, 0);
		setTypes(SkillType.MULTI_GRESSIVE, SkillType.AREA_OF_EFFECT, SkillType.DAMAGING, SkillType.NO_SELF_TARGETTING, SkillType.SILENCEABLE, SkillType.MOVEMENT_SLOWING);
	}

	@Override
	public String getDescription(Hero hero) {
		return getDescription();
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.RADIUS.node(), 5d);
		node.set(HEIGHT_NODE, 2d);
		node.set(SkillSetting.DURATION.node(), 5000);
		node.set(SkillSetting.PERIOD.node(), 500);
		node.set(SkillSetting.DAMAGE_TICK.node(), 50d);
		node.set(SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT.node(), 1d);

		return node;
	}

	@Override
	public SkillResult use(Hero hero, String[] strings) {
		if (isAreaGroundEffectApplied(hero)) {
			return SkillResult.INVALID_TARGET_NO_MSG;
		} else {
			broadcastExecuteText(hero);

			final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5d, false);
			double height = SkillConfigManager.getUseSetting(hero, this, HEIGHT_NODE, 2d, false);
			long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);
			long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1000, false);

			final double damageTick = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 100d, false)
					+ SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 2d, false) * hero.getAttributeValue(AttributeType.INTELLECT);

			applyAreaGroundEffectEffect(hero, period, duration, hero.getPlayer().getLocation(), radius, height, new GroundEffectActions() {
				@Override
				public void groundEffectTickAction(Hero hero, AreaGroundEffectEffect effect) {

				}

				@Override
				public void groundEffectTargetAction(Hero hero, LivingEntity target) {
					Player player = hero.getPlayer();
					if (damageCheck(player, target)) {
						damageEntity(target, player, damageTick, EntityDamageEvent.DamageCause.MAGIC, false);
					}

					//CharacterTemplate targetCt = plugin.getCharacterManager().getCharacter(target);
					//targetCt.addEffect(new SlowEffect(SkillConsecration.this, player, 0, 0));
				}
			});

			return SkillResult.NORMAL;
		}
	}
}
