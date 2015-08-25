package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.CylinderEffect;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import static com.herocraftonline.heroes.characters.skill.SkillType.*;

public class SkillDisarray extends SkillBaseBeam {

	private static final float PARTICLE_OFFSET_FROM_FACE = 1;
	private static final ParticleEffect BEAM_PARTICLE = ParticleEffect.REDSTONE;
	
	public SkillDisarray(Heroes plugin) {
		super(plugin, "Disarray");
		setDescription("Surfing with chaos, you fire off a beam that deals $1 damage to everything in its path.");
		setUsage("/skill disarray");
		setArgumentRange(0, 0);
		setIdentifiers("skill disarray");
		setTypes(DAMAGING, MULTI_GRESSIVE, AREA_OF_EFFECT, NO_SELF_TARGETTING, UNINTERRUPTIBLE, SILENCEABLE);
	}

	@Override
	public String getDescription(Hero hero) {
		double beamDamage = SkillConfigManager.getUseSetting(hero, SkillDisarray.this, SkillSetting.DAMAGE, 150d, false);
		double beamDamageIncrease = SkillConfigManager.getUseSetting(hero, SkillDisarray.this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 5d, false);
		beamDamage += hero.getAttributeValue(AttributeType.INTELLECT) * beamDamageIncrease;

		return getDescription().replace("$1", beamDamage + "");
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SETTING_BEAM_MAX_LENGTH, 15);
		node.set(SETTING_BEAM_RADIUS, 2d);

		node.set(SkillSetting.DAMAGE.node(), 150d);
		node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1d);

		return node;
	}

	static boolean test;

	@Override
	public SkillResult use(Hero hero, String[] strings) {
		Player player = hero.getPlayer();

		int beamMaxLength = SkillConfigManager.getUseSetting(hero, this, SETTING_BEAM_MAX_LENGTH, 15, false);
		double beamRadius = SkillConfigManager.getUseSetting(hero, this, SETTING_BEAM_RADIUS, 2d, false);
		Beam beam = createObstructedBeam(player.getEyeLocation(), beamMaxLength, beamRadius);

		EffectManager em = new EffectManager(plugin);
		CylinderEffect effect = new CylinderEffect(em);

		effect.setLocation(beam.midPoint().add(beam.getTrajectory().normalize().multiply(PARTICLE_OFFSET_FROM_FACE / 2)).toLocation(player.getWorld()));
		effect.height = (float) beam.length() - PARTICLE_OFFSET_FROM_FACE;
		effect.radius = (float) beamRadius / 8;

		effect.particle = BEAM_PARTICLE;
		effect.particles = 60;
		effect.iterations = 10;
		effect.visibleRange = 40;
		effect.solid = true;

		effect.rotationX = Math.toRadians(player.getLocation().getPitch() + 90);
		effect.rotationY = -Math.toRadians(player.getLocation().getYaw());
		effect.angularVelocityX = 0;
		effect.angularVelocityY = 0;
		effect.angularVelocityZ = 0;

		broadcastExecuteText(hero);

		castBeam(hero, beam, new TargetHandler() {
			@Override
			public void handle(Hero hero, LivingEntity target, Beam.PointData pointData) {
				if (damageCheck(hero.getPlayer(), target)) {
					double beamDamage = SkillConfigManager.getUseSetting(hero, SkillDisarray.this, SkillSetting.DAMAGE, 150d, false);
					double beamDamageIncrease = SkillConfigManager.getUseSetting(hero, SkillDisarray.this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1d, false);
					beamDamage += hero.getAttributeValue(AttributeType.INTELLECT) * beamDamageIncrease;

					damageEntity(target, hero.getPlayer(), beamDamage, EntityDamageEvent.DamageCause.MAGIC, false);
				}
			}
		});

		effect.start();
		em.disposeOnTermination();

		player.getWorld().playSound(player.getEyeLocation(), Sound.ENDERMAN_SCREAM, 0.2f, 0.0001f);
		player.getWorld().playSound(player.getEyeLocation().add(beam.getTrajectory()), Sound.ENDERMAN_SCREAM, 0.2f, 0.0001f);
		player.getWorld().playSound(beam.midPoint().toLocation(player.getWorld()), Sound.ENDERMAN_SCREAM, 0.2f, 0.0001f);

		return SkillResult.NORMAL;
	}
}