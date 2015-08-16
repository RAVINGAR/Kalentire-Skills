package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillHealBeam extends SkillBaseBeam {

	private static final double TEMP_HEAL = 10;

	public SkillHealBeam(Heroes plugin) {
		super(plugin, "Heal_Beam");
		setDescription("Heal stuff in a beam");
	}

	@Override
	public String getDescription(Hero hero) {
		return getDescription();
	}

	@Override
	protected void onTargetHit(Hero hero, LivingEntity target) {
		if (target instanceof Player) {
			Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
			targetHero.heal(TEMP_HEAL);
		}
	}
}
