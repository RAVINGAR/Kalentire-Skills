package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;

public class SkillDamageBeamShot extends SkillBaseBeamShot {

	public SkillDamageBeamShot(Heroes plugin) {
		super(plugin, "DamageBeamShot");
	}

	@Override
	public String getDescription(Hero hero) {
		return null;
	}

	@Override
	public SkillResult use(Hero hero, String[] strings) {
		return null;
	}
}
