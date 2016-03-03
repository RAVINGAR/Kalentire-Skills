package com.herocraftonline.heroes.characters.skill.skills;

import static org.bukkit.event.entity.EntityDamageEvent.DamageCause.MAGIC;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.CylinderEffect;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import static com.herocraftonline.heroes.characters.skill.SkillType.*;

public class SkillDamageBeam extends SkillBaseBeam {

	private static final double TEMP_DAMAGE = 10;

	public SkillDamageBeam(Heroes plugin) {
		super(plugin, "DamageBeam");
		setDescription("Damage stuff in a beam");
		setUsage("/skill DamageBeam");
		setIdentifiers("skill " + getName());
		setTypes(DAMAGING, MULTI_GRESSIVE, AREA_OF_EFFECT, NO_SELF_TARGETTING, UNINTERRUPTIBLE, SILENCEABLE);
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

		renderEyeBeam(player, beam, ParticleEffect.FLAME, 40, 10, 40, 1 / 12, 1);

		/*List<Location> fxLine = MathUtils.getLinePoints(player.getEyeLocation(),
				player.getEyeLocation().add(beam.getTrajectoryX(), beam.getTrajectoryX(), beam.getTrajectoryX()), (int) beam.length() * 2);
		for (int i = 4; i < fxLine.size(); i++) {
			player.getWorld().spigot().playEffect(fxLine.get(i), Effect.FLAME, 0, 0, 0.05f, 0.05f, 0.05f, 0.005f, 8, 16);
		}*/

		player.getWorld().playSound(player.getEyeLocation(), Sound.ENTITY_LIGHTNING_THUNDER, 10, 5);

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

	/*
		Have not tested this properly, was originally going to be used
	 */
	/*private static Vector vectorToEuler(Vector direction, Vector to_frame) {
		double[][] matrix1 = new double[3][3];
		double[][] matrix2 = new double[3][3];

		Vector axis = direction.crossProduct(to_frame).normalize();

		matrix1[0] = vectorToArray(direction);
		matrix2[0] = vectorToArray(to_frame);

		matrix1[1] = vectorToArray(axis);
		matrix2[1] = vectorToArray(axis);

		matrix1[2] = vectorToArray(axis.crossProduct(direction));
		matrix2[2] = vectorToArray(axis.crossProduct(to_frame));

		matrix1 = transposeMatrix(matrix1);

		double a = -Math.asin(matrixMultiplyE(matrix1, matrix2, 0, 2));
		double b = Math.atan2(matrixMultiplyE(matrix1, matrix2, 1, 2),
				matrixMultiplyE(matrix1, matrix2, 2, 2));
		double y = Math.atan2(matrixMultiplyE(matrix1, matrix2, 0, 1),
				matrixMultiplyE(matrix1, matrix2, 0, 0));

		return new Vector(a, b, y);
	}

	private static double matrixMultiplyE(double[][] m1, double[][] m2, int x, int y) {
		double ans = 0;

		for (int e = 0; e < m1.length; e++) {
			ans += m1[e][y] + m2[x][e];
		}

		return ans;
	}

	private static double[] vectorToArray(Vector v) {
		double[] a = new double[3];
		a[0] = v.getX();
		a[1] = v.getY();
		a[2] = v.getZ();
		return a;
	}

	private static double[][] transposeMatrix(double[][] matrix) {
		double[][] transpose = new double[matrix.length][matrix[0].length];

		for (int c = 0 ; c < matrix.length ; c++ )
		{
			for (int d = 0 ; d < matrix[0].length ; d++ )
				transpose[d][c] = matrix[c][d];
		}

		return transpose;
	}*/
}
