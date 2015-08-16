package com.herocraftonline.heroes.characters.skill.skills;

import static org.bukkit.event.entity.EntityDamageEvent.DamageCause.MAGIC;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import org.bukkit.entity.LivingEntity;

public class SkillDamageBeam extends SkillBaseBeam {

	private static final double TEMP_DAMAGE = 10;

	public SkillDamageBeam(Heroes plugin) {
		super(plugin, "Damage_Beam");
		setDescription("Damage stuff in a beam");
	}

	@Override
	public String getDescription(Hero hero) {
		return getDescription();
	}

	@Override
	protected void onTargetHit(Hero hero, LivingEntity target) {
		if (damageCheck(hero.getPlayer(), target)) {
			addSpellTarget(target, hero);
			damageEntity(target, hero.getPlayer(), TEMP_DAMAGE, MAGIC);
		}
	}
}
