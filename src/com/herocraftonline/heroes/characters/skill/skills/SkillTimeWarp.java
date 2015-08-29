package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillTimeWarp extends SkillBaseMarkedTeleport {

	public SkillTimeWarp(Heroes plugin) {
		super(plugin, "TimeWarp", false, new EffectType[] {
				EffectType.DAMAGING,
				EffectType.HARMFUL
		}, ParticleEffect.REDSTONE, new Color[] {
				Color.PURPLE,
				Color.BLACK
		});


		setUsage("/skill timewarp");
		setIdentifiers("skill timewarp");

		setTypes(SkillType.AGGRESSIVE, SkillType.DAMAGING, SkillType.NO_SELF_TARGETTING, SkillType.TELEPORTING);
	}

	@Override
	public String getDescription(Hero hero) {
		return getDescription();
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.DURATION.node(), 10000);
		node.set(SkillSetting.DAMAGE.node(), 250d);

		node.set(PRESERVE_LOOK_DIRECTION_NODE, true);
		node.set(PRESERVE_VELOCITY_NODE, true);

		return node;
	}

	@Override
	protected void onMarkerActivate(Marker marker, long activateTime) {
		if (damageCheck(marker.getHero().getPlayer(), marker.getTarget().getEntity())) {
			double damage = SkillConfigManager.getUseSetting(marker.getHero(), this, SkillSetting.DAMAGE, 250d, false);

			double totalDuration = SkillConfigManager.getUseSetting(marker.getHero(), this, SkillSetting.DURATION, 10000, false);
			double damageScale = 1 - (activateTime - marker.getCreateTime() / totalDuration);
			if (damageScale < 0) {
				damageScale = 0;
			}

			damageEntity(marker.getTarget().getEntity(), marker.getHero().getPlayer(), damage * damageScale, EntityDamageEvent.DamageCause.MAGIC);
		}
	}
}
