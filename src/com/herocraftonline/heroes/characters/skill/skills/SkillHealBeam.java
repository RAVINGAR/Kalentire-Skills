package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.utils.Beam;
import com.herocraftonline.heroes.util.MathUtils;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;

public class SkillHealBeam extends ActiveSkill implements Beam.TargetFunction<Player> {

	private static final double TEMP_HEAL = 10;

	public SkillHealBeam(Heroes plugin) {
		super(plugin, "HealBeam");
		setDescription("Heal stuff in a beam");
		setUsage("/skill HealBeam");
		setIdentifiers("skill " + getName());
		setTypes(SkillType.HEALING);
		setArgumentRange(0, 0);
	}

	@Override
	public String getDescription(Hero hero) {
		return getDescription();
	}

	@Override
	public SkillResult use(Hero hero, String[] strings) {
		Player player = hero.getPlayer();
		Beam beam = new Beam(player, Util.transparentBlocks, 20, 5);

		broadcastExecuteText(hero);

		List<Location> fxLine = MathUtils.getLinePoints(player.getEyeLocation(),
				player.getEyeLocation().add(beam.getDirectionX(), beam.getDirectionY(), beam.getDirectionZ()), (int) beam.calculateLength() * 4);
		for (int i = 4; i < fxLine.size(); i++) {
			player.getWorld().spigot().playEffect(fxLine.get(i), Effect.HAPPY_VILLAGER, 0, 0, 0.05f, 0.05f, 0.05f, 0.005f, 8, 16);
		}

		player.getWorld().playSound(player.getEyeLocation(), Sound.AMBIENCE_THUNDER, 6, 2);

		Beam.castOnPlayers(hero, beam, this);
		return SkillResult.NORMAL;
	}

	@Override
	public void handle(Hero hero, Player target, Beam.PointData pointData) {
		Hero targetHero = plugin.getCharacterManager().getHero(target);
		targetHero.heal(TEMP_HEAL);
	}
}
