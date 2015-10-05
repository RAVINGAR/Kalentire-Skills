package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import org.bukkit.configuration.ConfigurationSection;

public class SkillGreatChasm extends SkillBaseBlockWave {

	public SkillGreatChasm(Heroes plugin) {
		super(plugin, "GreatChasm");
		setDescription("");
		setUsage("/skill " + getName().toLowerCase());
		setIdentifiers("skill " + getName().toLowerCase());
		setArgumentRange(0, 0);
	}

	@Override
	public String getDescription(Hero hero) {
		return getDescription();
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.RADIUS.node(), 8);
		node.set(HEIGHT_NODE, 3);
		node.set(DEPTH_NODE, 5);
		node.set(EXPANSION_RATE_NODE, 1);
		node.set(WAVE_ARC_NODE, 60d);
		node.set(LAUNCH_FORCE_NODE, 0.2);
		node.set(HIT_LIMIT_NODE, 1);

		node.set(SkillSetting.DAMAGE.node(), 100d);
		node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 1);

		return node;
	}

	@Override
	public SkillResult use(Hero hero, String[] strings) {
		return null;
	}
}
