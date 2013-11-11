package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;

public class SkillForge extends PassiveSkill {
	public SkillForge(Heroes plugin) {
		super(plugin, "Forge");
		setDescription("Passive: Grants ability to use the anvil to improve weapons/tools/armor's sharpness/efficiency/protection respectively.");
		setEffectTypes(new EffectType[] { EffectType.BENEFICIAL });
	}

	@Override 
	public String getDescription(Hero hero) {
		return getDescription();
	}
	
}