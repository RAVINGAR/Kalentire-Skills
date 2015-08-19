package com.herocraftonline.heroes.characters.skill.skills;

import static org.bukkit.event.entity.EntityDamageEvent.DamageCause.MAGIC;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.CylinderEffect;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

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

		CylinderEffect cyl = new CylinderEffect(em);
		cyl.setLocation(beam.midPoint().toLocation(player.getWorld()).add(beam.getTrajectory().normalize().multiply(0.5)));
		cyl.asynchronous = true;

		cyl.radius = (float) beam.radius() / 12;
		cyl.height = (float) beam.length() - 1;
		cyl.particles = 40;
		cyl.solid = true;
		cyl.rotationX = Math.toRadians(player.getLocation().getPitch() + 90);
		cyl.rotationY = -Math.toRadians(player.getLocation().getYaw());
		cyl.angularVelocityX = 0;
		cyl.angularVelocityY = 0;
		cyl.angularVelocityZ = 0;
		cyl.iterations = 20;
		cyl.visibleRange = 40;

		cyl.start();
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

	private static Vector vectorToEuler(Vector direction, Vector to_frame) {
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
	}
}
