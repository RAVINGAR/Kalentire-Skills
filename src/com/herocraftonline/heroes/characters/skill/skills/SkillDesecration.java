package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;

public class SkillDesecration extends SkillBaseGroundEffect {

	public SkillDesecration(Heroes plugin) {
		super(plugin, "Desecration");
		setDescription("");
		setUsage("/skill desecration");
		setIdentifiers("skill desecration");
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
