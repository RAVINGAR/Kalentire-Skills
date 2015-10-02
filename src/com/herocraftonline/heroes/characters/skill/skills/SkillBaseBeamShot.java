package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.nms.physics.NMSPhysics;

public abstract class SkillBaseBeamShot extends ActiveSkill {

	private static NMSPhysics physics = NMSHandler.getInterface().getNMSPhysics();

	public SkillBaseBeamShot(Heroes plugin, String name) {
		super(plugin, name);
	}

	protected void fireBeamShot(Hero hero, double radius, double velocity) {

	}
}
