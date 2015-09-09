package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;

public class SkillConsecration extends SkillBaseGroundEffect {

	public SkillConsecration(Heroes plugin) {
		super(plugin, "Consecration");
		setDescription("");
		setUsage("/skill consecration");
		setIdentifiers("skill consecration");
		setArgumentRange(0, 0);
	}

	@Override
	public String getDescription(Hero hero) {
		return getDescription();
	}

	@Override
	public SkillResult use(Hero hero, String[] strings) {
		return null;
	}
}
