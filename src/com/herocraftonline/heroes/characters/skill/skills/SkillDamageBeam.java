package com.herocraftonline.heroes.characters.skill.skills;

import static org.bukkit.event.entity.EntityDamageEvent.DamageCause.MAGIC;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.LineEffect;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.Color;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

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
		Beam beam = createObstructedBeam(player.getEyeLocation(), 20, 2, Util.transparentBlocks);

		broadcastExecuteText(hero);

		EffectManager em = new EffectManager(plugin);

		LineEffect line = new LineEffect(em);
		line.asynchronous = true;
		line.particles = (int) beam.length() * 2;
		line.isZigZag = true;
		line.zigZags = (int) beam.length();
		line.particle = ParticleEffect.FLAME;
		line.start();
		em.disposeOnTermination();

		/*List<Location> fxLine = MathUtils.getLinePoints(player.getEyeLocation(),
				player.getEyeLocation().add(beam.getTrajectoryX(), beam.getTrajectoryX(), beam.getTrajectoryX()), (int) beam.length() * 2);
		for (int i = 4; i < fxLine.size(); i++) {
			player.getWorld().spigot().playEffect(fxLine.get(i), Effect.FLAME, 0, 0, 0.05f, 0.05f, 0.05f, 0.005f, 8, 16);
		}*/

		player.getWorld().playSound(player.getEyeLocation(), Sound.AMBIENCE_THUNDER, 6, 2);

		castBeam(hero, beam, new TargetHandler() {
			@Override
			public void handle(Hero hero, LivingEntity target, Beam.PointData pointData) {
				if (damageCheck(hero.getPlayer(), target)) {
					target.setVelocity(pointData.calculateVectorFromBeam().normalize().multiply(2));
					addSpellTarget(target, hero);
					damageEntity(target, hero.getPlayer(), TEMP_DAMAGE, MAGIC);
				}
			}
		});

		return SkillResult.NORMAL;
	}
}
