package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillHealBeam extends SkillBaseBeam {

	private static final double TEMP_HEAL = 10;

	public SkillHealBeam(Heroes plugin) {
		super(plugin, "HealBeam");
		setDescription("Heal stuff in a beam");
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
		if (target instanceof Player) {
			Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
			targetHero.heal(TEMP_HEAL);
		}
	}
}
