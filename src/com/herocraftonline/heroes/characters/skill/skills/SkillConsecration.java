package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SpeedEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillConsecration extends SkillBaseGroundEffect {

	private static final String SPEED_BUFF_DURATION_NODE = "speed-buff-duration";
	private static final String SPEED_BUFF_AMPLIFIER_NODE = "speed-buff-amplifier";

	public SkillConsecration(Heroes plugin) {
		super(plugin, "Consecration");
		setDescription("");
		setUsage("/skill consecration");
		setIdentifiers("skill consecration");
		setArgumentRange(0, 0);
		setTypes(SkillType.MULTI_GRESSIVE, SkillType.AREA_OF_EFFECT, SkillType.DAMAGING, SkillType.NO_SELF_TARGETTING, SkillType.SILENCEABLE, SkillType.MOVEMENT_INCREASING);
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
			final Player player = hero.getPlayer();

			broadcastExecuteText(hero);

			final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5d, false);
			double height = SkillConfigManager.getUseSetting(hero, this, HEIGHT_NODE, 2d, false);
			long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);
			long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1000, false);

			final double damageTick = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 100d, false)
					+ SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 2d, false) * hero.getAttributeValue(AttributeType.INTELLECT);

			applyAreaGroundEffectEffect(hero, period, duration, player.getLocation(), radius, height, new GroundEffectActions() {
				
				@Override
				public void groundEffectTickAction(Hero hero, AreaGroundEffectEffect effect) {

				}

				@Override
				public void groundEffectTargetAction(Hero hero, LivingEntity target) {
					Player player = hero.getPlayer();
					if (damageCheck(player, target)) {
						damageEntity(target, player, damageTick, EntityDamageEvent.DamageCause.MAGIC, false);
					}

					CharacterTemplate targetCt = plugin.getCharacterManager().getCharacter(target);
					targetCt.addEffect(new SpeedEffect(SkillConsecration.this, player, 0, 0));
				}
			});

			return SkillResult.NORMAL;
		}
	}
}
