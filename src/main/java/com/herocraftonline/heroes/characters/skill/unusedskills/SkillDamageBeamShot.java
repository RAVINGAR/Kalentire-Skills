package com.herocraftonline.heroes.characters.skill.unusedskills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseBeamShot;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.nms.physics.RayCastFlag;
import com.herocraftonline.heroes.nms.physics.collision.AABB;
import com.herocraftonline.heroes.nms.physics.collision.Capsule;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.EnumSet;
import java.util.function.Predicate;

import static com.herocraftonline.heroes.characters.skill.SkillType.*;

public class SkillDamageBeamShot extends SkillBaseBeamShot {

	public SkillDamageBeamShot(Heroes plugin) {
		super(plugin, "DamageBeamShot");
		setDescription("Damage stuff in a beam shot");
		setUsage("/skill damagebeamshot");
		setIdentifiers("skill damagebeamshot");
		setTypes(DAMAGING, AREA_OF_EFFECT, NO_SELF_TARGETTING, UNINTERRUPTIBLE, SILENCEABLE);
		setArgumentRange(0, 0);
	}

	@Override
	public String getDescription(Hero hero) {
		return getDescription();
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.RADIUS.node(), 0.5);
		node.set(SkillSetting.MAX_DISTANCE.node(), 20d);
		node.set(VELOCITY_NODE, 0.2);
		node.set(PENETRATION_NODE, 0);
		node.set("knockback", 0.75);

		return node;
	}

	@Override
	public SkillResult use(Hero hero, String[] strings) {

		double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 0.5, false);
		double range = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 20d, false);
		double velocity = SkillConfigManager.getUseSetting(hero, this, VELOCITY_NODE, 0.2, false);
		int penetration = SkillConfigManager.getUseSetting(hero, this, PENETRATION_NODE, 0, false);
		final double knockback = SkillConfigManager.getUseSetting(hero, this, "knockback", 0.75, false);

		fireBeamShot(hero, range, radius, velocity, penetration, new BeamShotHit() {

			@Override
			public void onHit(Hero hero, LivingEntity target, Location origin, Capsule shot, int count, boolean first, boolean last) {
				if (damageCheck(hero.getPlayer(), target)) {
					damageEntity(target, hero.getPlayer(), 10d, EntityDamageEvent.DamageCause.MAGIC, false);

					AABB targetAABB = NMSHandler.getInterface().getNMSPhysics().getEntityAABB(target);
					target.setVelocity(
							target.getVelocity()
									.add(targetAABB.getCenter()
											.subtract(origin.toVector())
											.normalize().multiply(knockback)
											.add(new Vector(0, 0.25, 0))));
				}
			}

			@Override
			public void onRenderShot(Location origin, Capsule shot, int frame, boolean first, boolean last) {

				if (first) {
					origin.getWorld().playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 0.05f, 0.2f);
				}

				if (last) {
					Location loc = shot.getPoint2().toLocation(origin.getWorld());
					origin.getWorld().playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.05f, 0.2f);
				}

				Location travelSoundLoc = shot.getBounds().getCenter().toLocation(origin.getWorld());
				origin.getWorld().playSound(travelSoundLoc, Sound.ENTITY_GENERIC_BURN, 0.05f, 0.2f);

				renderBeamShotFrame(origin, shot, Particle.FLAME, Color.ORANGE, (int) (shot.getPoint1().distanceSquared(shot.getPoint2()) * 10), 3, 32, 0.5, 1);

				/*boolean render = false;
				Vector originV = origin.toVector();

				Location start = null, end = null;

				if (originV.distanceSquared(shot.getPoint1()) >= 1) {
					start = shot.getPoint1().toLocation(origin.getWorld());
					end = shot.getPoint2().toLocation(origin.getWorld());
					render = true;
				} else if (originV.distanceSquared(shot.getPoint2()) >= 1) {
					start = shot.getPoint1().add(shot.getPoint2().subtract(shot.getPoint1()).normalize()).toLocation(origin.getWorld());
					end = shot.getPoint2().toLocation(origin.getWorld());
					render = true;
				}

				if (render) {
					EffectManager em = new EffectManager(plugin);
					LineEffect line = new LineEffect(em);

					//line.setLocation(shot.getPoint1().toLocation(origin.getWorld()));
					//line.setTargetSupplier(shot.getPoint2().toLocation(origin.getWorld()));
					line.setLocation(start);
					line.setTargetSupplier(end);
					line.asynchronous = true;
					line.particles = (int) (line.getLocation().distance(line.getTarget()) * 10);
					line.particle = ParticleEffect.FLAME;
					line.type = EffectType.INSTANT;
					line.visibleRange = 32;

					em.start(line);
					em.disposeOnTermination();
				}*/
			}
		}, block -> {
            return true;
            //FIXME Don't care about this skill, is test skill I made to test beam skills way back
//				return block.getType() != Material.GLASS && block.getType() !=  Material.STAINED_GLASS &&
//						block.getType() != Material.THIN_GLASS && block.getType() != Material.STAINED_GLASS_PANE;
        }, EnumSet.of(RayCastFlag.BLOCK_IGNORE_NON_SOLID, RayCastFlag.BLOCK_HIGH_DETAIL));

		broadcastExecuteText(hero);
		return SkillResult.NORMAL;
	}
}
