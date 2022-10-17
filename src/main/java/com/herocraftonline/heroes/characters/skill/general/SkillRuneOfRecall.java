package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseMarkedTeleport;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;

public class SkillRuneOfRecall extends SkillBaseMarkedTeleport {
	public SkillRuneOfRecall(Heroes plugin) {
		super(plugin, "RuneOfRecall", true, new EffectType[]{
				EffectType.HEALING,
				EffectType.BENEFICIAL,
		}, Particle.GLOW_SQUID_INK, new Color[]{
				Color.MAROON,
				Color.WHITE
		});
		setDescription("Mark your current position for the next $1 second(s). At any point during that time you may re activate the skill to teleport" +
				" your self back to that location.");
		setUsage("/skill timerune");
		setIdentifiers("skill timerune");

		setTypes(SkillType.HEALING, SkillType.TELEPORTING);
	}

	@Override
	public String getDescription(Hero hero) {
		double totalDuration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);

		return getDescription()
				.replace("$1", Util.decFormat.format(totalDuration / 1000));
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.DURATION.node(), 300000);

		node.set(PRESERVE_LOOK_DIRECTION_NODE, true);
		node.set(PRESERVE_VELOCITY_NODE, true);

		return node;
	}

	@Override
	protected void onMarkerActivate(Marker marker, long activateTime) {

	}
}
