package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import org.bukkit.entity.Projectile;

public abstract class SkillBaseProjectile extends ActiveSkill {

	public SkillBaseProjectile(Heroes plugin, String name) {
		super(plugin, name);
	}
}
