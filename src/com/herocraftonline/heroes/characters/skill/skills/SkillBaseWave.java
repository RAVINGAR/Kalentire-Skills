package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import org.bukkit.util.Vector;

public abstract class SkillBaseWave extends ActiveSkill {

	public SkillBaseWave(Heroes plugin, String name) {
		super(plugin, name);
	}

	protected static final class Wave {

		private final Vector origin;        // Wave origin vector.
		private final double velocity;      // Wave outward velocity
		private final double thickness;     // Wave thickness
		

		private Wave(Vector origin, double velocity, double thickness) {
			this.origin = origin.clone();
			this.velocity = velocity;
			this.thickness = thickness;
		}

		public Vector getOrigin() {
			return origin.clone();
		}

		public double getVelocity() {
			return velocity;
		}

		public double getThickness() {
			return thickness;
		}
	}
}
