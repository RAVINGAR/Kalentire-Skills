package com.herocraftonline.heroes.characters.skill.skills;

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

public class SkillHealBeam extends SkillBaseBeam {

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
		Beam beam = createObstructedBeam(player.getEyeLocation(), 20, 2);

		broadcastExecuteText(hero);

		EffectManager em = new EffectManager(plugin);

		LineEffect line = new LineEffect(em);
		line.setLocation(player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(2)));
		line.setTargetLocation(player.getEyeLocation().add(beam.getTrajectory()));
		line.asynchronous = true;
		line.particles = (int) beam.length() * 2;
		line.particle = ParticleEffect.VILLAGER_HAPPY;
		line.start();
		em.disposeOnTermination();

		/*List<Location> fxLine = MathUtils.getLinePoints(player.getEyeLocation(),
				player.getEyeLocation().add(beam.getTrajectoryX(), beam.getTrajectoryX(), beam.getTrajectoryX()), (int) beam.length() * 2);
		for (int i = 4; i < fxLine.size(); i++) {
			player.getWorld().spigot().playEffect(fxLine.get(i), Effect.HAPPY_VILLAGER, 0, 0, 0.05f, 0.05f, 0.05f, 0.005f, 8, 16);
		}*/

		player.getWorld().playSound(player.getEyeLocation(), Sound.ENTITY_LIGHTNING_THUNDER, 6, 2);

		castBeam(hero, beam, new TargetHandler() {
			@Override
			public void handle(Hero hero, LivingEntity target, Beam.PointData pointData) {
				if (target instanceof Player){
					Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
					targetHero.heal(TEMP_HEAL);
				}
			}
		});

		return SkillResult.NORMAL;
	}
}
