package com.herocraftonline.heroes.characters.skill.skills;

import static org.bukkit.event.entity.EntityDamageEvent.DamageCause.MAGIC;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.MathUtils;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;

public class SkillDamageBeam extends SkillBaseBeam {

	private static final double TEMP_DAMAGE = 10;

	public SkillDamageBeam(Heroes plugin) {
		super(plugin, "DamageBeam");
		setDescription("Damage stuff in a beam");
		setUsage("/skill DamageBeam");
		setIdentifiers("skill " + getName());
		setTypes(SkillType.DAMAGING);
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
				player.getEyeLocation().add(beam.getDirectionX(), beam.getDirectionY(), beam.getDirectionZ()), (int) beam.calculateLength() * 2);
		for (int i = 0; i < fxLine.size(); i++) {
			player.getWorld().spigot().playEffect(fxLine.get(i), Effect.CRIT, 0, 0, 0.4f, 0.4f, 0.4f, 1, 25, 16);
		}

		player.getWorld().playSound(player.getEyeLocation(), Sound.AMBIENCE_THUNDER, 6, 2);

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