package com.herocraftonline.heroes.characters.skill.skills.codari;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;

public abstract class SkillBaseWave extends ActiveSkill {

	public SkillBaseWave(Heroes plugin, String name) {
		super(plugin, name);
	}

	protected static final class Wave {

		private final double ox, oy, oz;        // Wave origin vector.

		private Wave(double ox, double oy, double oz) {
			this.ox = ox;
			this.oy = oy;
			this.oz = oz;
		}
	}
}
