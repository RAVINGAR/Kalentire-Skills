package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public abstract class SkillBaseBeam extends ActiveSkill {

	private static int TEMP_DISTANCE = 15;
	private static double TEMP_RADIUS = 2;

	public SkillBaseBeam(Heroes plugin, String name) {
		super(plugin, name);
	}

	@Override
	public final SkillResult use(Hero hero, String[] strings) {
		final Player player = hero.getPlayer();

		Vector beamOrigin = player.getEyeLocation().toVector();
		Vector beamVec = player.getEyeLocation().getDirection();

		Block tempBlock;
		BlockIterator blockIterator;
		double beamLength = TEMP_DISTANCE;

		try {
			blockIterator = new BlockIterator(player.getWorld(), beamOrigin, beamVec, 0, TEMP_DISTANCE);
		}
		catch (IllegalStateException ex) {
			return SkillResult.INVALID_TARGET_NO_MSG;
		}

		while (blockIterator.hasNext()) {
			tempBlock = blockIterator.next();

			if (!Util.transparentBlocks.contains(tempBlock.getType())) {
				beamLength = beamOrigin.distance(tempBlock.getLocation().toVector());
				break;
			}
		}

		beamVec.multiply(beamLength);
		double lengthSq = beamVec.lengthSquared();
		double radiusSq = TEMP_RADIUS * TEMP_RADIUS;

		broadcastExecuteText(hero);

		double checkRadius = beamLength + TEMP_RADIUS;
		List<Entity> possibleTargets = player.getNearbyEntities(checkRadius, checkRadius, checkRadius);

		for (Entity entity : possibleTargets) {
			if (entity instanceof LivingEntity) {
				LivingEntity target = (LivingEntity) entity;
				if (isWithinBeam(beamOrigin, beamVec, lengthSq, radiusSq, target.getLocation().toVector())
						&& isWithinBeam(beamOrigin, beamVec, lengthSq, radiusSq, target.getEyeLocation().toVector())) {
					onTargetHit(hero, (LivingEntity) entity);
				}
			}
		}

		return SkillResult.NORMAL;
	}

	/*
		This function is copied from http://www.flipcode.com/archives/Fast_Point-In-Cylinder_Test.shtml

		For more information about it check there. For a simple description, it tests if a point is within
		a cylinder given information about its size and orientation.

		This method is messy in order to be fast.
	 */
	private boolean isWithinBeam(Vector beamOrigin, Vector beamVec, double lengthSq, double radiusSq, Vector testPoint) {
		double pdx, pdy, pdz;
		double dot;

		pdx = testPoint.getX() - beamOrigin.getX();
		pdy = testPoint.getY() - beamOrigin.getY();
		pdz = testPoint.getZ() - beamOrigin.getZ();

		dot = pdx * beamVec.getX() + pdy * beamVec.getY() + pdz * beamVec.getZ();

		if (dot < 0 || dot > lengthSq) {
			return false;
		}
		else {
			// Accessing this value can make variable effects based on distance from beam line.
			double dsq = (pdx * pdx + pdy * pdy + pdz * pdz) - dot * dot / lengthSq;
			return dsq <= radiusSq;
		}
	}

	protected abstract void onTargetHit(Hero hero, LivingEntity target);
}
