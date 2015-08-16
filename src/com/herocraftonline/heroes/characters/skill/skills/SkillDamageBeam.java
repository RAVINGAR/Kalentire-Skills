package com.herocraftonline.heroes.characters.skill.skills;

import static org.bukkit.event.entity.EntityDamageEvent.DamageCause.MAGIC;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

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
	public SkillResult use(Hero hero, String[] strings) {
		Player player = hero.getPlayer();
		Beam beam = new Beam(player, Util.transparentBlocks, 20, 5);
		castBeam(hero, beam);
		return SkillResult.NORMAL;
	}

	@Override
	protected void onTargetHit(Hero hero, LivingEntity target, Beam.PointData pointData) {
		if (damageCheck(hero.getPlayer(), target)) {
			hero.getPlayer().setVelocity(pointData.calculateVectorFromBeam().normalize().multiply(5));
			addSpellTarget(target, hero);
			damageEntity(target, hero.getPlayer(), TEMP_DAMAGE, MAGIC);
		}
	}
}
