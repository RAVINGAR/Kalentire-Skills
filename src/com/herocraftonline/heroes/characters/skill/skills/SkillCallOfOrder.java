package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import org.bukkit.configuration.ConfigurationSection;

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
		return null;
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
		return null;
	}
}
