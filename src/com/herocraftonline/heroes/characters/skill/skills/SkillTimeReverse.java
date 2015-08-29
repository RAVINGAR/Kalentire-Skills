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

public class SkillTimeReverse extends SkillBaseMarkedTeleport {

	private static final String HEALING_PERCENTAGE_NODE = "healing-percentage";

	public SkillTimeReverse(Heroes plugin) {
		super(plugin, "TimeReverse", true, new EffectType[] {
				EffectType.HEALING,
				EffectType.BENEFICIAL,
		}, ParticleEffect.REDSTONE, new Color[] {
				Color.BLUE,
				Color.WHITE
		});


		setUsage("/skill timereverse");
		setIdentifiers("skill timereverse");

		setTypes(SkillType.HEALING, SkillType.TELEPORTING);
	}

	@Override
	public String getDescription(Hero hero) {
		return getDescription();
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.DURATION.node(), 10000);
		node.set(HEALING_PERCENTAGE_NODE, 0.25d);

		node.set(PRESERVE_LOOK_DIRECTION_NODE, true);
		node.set(PRESERVE_VELOCITY_NODE, true);

		return node;
	}

	@Override
	protected void onMarkerActivate(SkillBaseMarkedTeleport.Marker marker, long activateTime) {
		double healing = SkillConfigManager.getUseSetting(marker.getHero(), this, SkillSetting.DAMAGE, 250d, false);

		double totalDuration = SkillConfigManager.getUseSetting(marker.getHero(), this, SkillSetting.DURATION, 10000, false);
		double healScale = 1 - ((activateTime - marker.getCreateTime()) / totalDuration);
		if (healScale < 0) {
			healScale = 0;
		}

		((Hero) marker.getTarget()).heal(healing * healScale);
	}

	private double getMaxHealAmount(Hero hero) {
		double healPercentage = SkillConfigManager.getUseSetting(hero, this, HEALING_PERCENTAGE_NODE, 0.25d, false);
		return hero.getPlayer().getMaxHealth() * healPercentage;
	}
}
