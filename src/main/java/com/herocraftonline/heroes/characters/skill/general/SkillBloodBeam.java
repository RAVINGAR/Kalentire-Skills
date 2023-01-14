package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.BloodUnionEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseBeam;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;

import static com.herocraftonline.heroes.characters.skill.SkillType.*;

public class SkillBloodBeam extends SkillBaseBeam {

	private static final Particle BEAM_PARTICLE = Particle.REDSTONE;

	public SkillBloodBeam(Heroes plugin) {
		super(plugin, "BloodBeam");
		setDescription("Surging with blood, you fire off a beam that deals $1 damage to everything in its path.");
		setUsage("/skill bloodbeam");
		setArgumentRange(0, 0);
		setIdentifiers("skill bloodbeam");
		setTypes(ABILITY_PROPERTY_MAGICAL, DAMAGING, MULTI_GRESSIVE, AREA_OF_EFFECT, NO_SELF_TARGETTING, UNINTERRUPTIBLE,
				SILENCEABLE, ABILITY_PROPERTY_PROJECTILE, ABILITY_PROPERTY_BLEED);
	}

	@Override
	public String getDescription(Hero hero) {
		double beamDamage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
		return getDescription().replace("$1", Util.decFormat.format(beamDamage));
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection config = super.getDefaultConfig();
		config.set(BEAM_MAX_LENGTH_NODE, 15);
		config.set(BEAM_RADIUS_NODE, 2d);

		config.set(SkillSetting.DAMAGE.node(), 150d);
		config.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1d);
		config.set("blood-union-increase", 1);
		return config;
	}

	@Override
	public SkillResult use(Hero hero, String[] args) {
		final Player player = hero.getPlayer();
	//public SkillResult use(Hero hero, LivingEntity target, String[] args) {

		int beamMaxLength = SkillConfigManager.getUseSetting(hero, this, BEAM_MAX_LENGTH_NODE, 15, false);
		double beamRadius = SkillConfigManager.getUseSetting(hero, this, BEAM_RADIUS_NODE, 2d, false);
		final Beam beam = createObstructedBeam(player.getEyeLocation(), beamMaxLength, beamRadius);

		broadcastExecuteText(hero);
		final Skill skill = SkillBloodBeam.this;

		castBeam(hero, beam, new TargetHandler() {
			private boolean alreadyIncreasedBloodUnion = false;

			@Override
			public void handle(Hero hero, LivingEntity target, Beam.PointData pointData) {
				if (damageCheck(hero.getPlayer(), target)) {
					double beamDamage = SkillConfigManager.getScaledUseSettingDouble(hero, skill, SkillSetting.DAMAGE, false);
					damageEntity(target, hero.getPlayer(), beamDamage, EntityDamageEvent.DamageCause.MAGIC, 0.0f);

					// Just increase union only once (for the first target only)
					if (!alreadyIncreasedBloodUnion) {
						alreadyIncreasedBloodUnion = true;

						// Increase Blood Union
						if (hero.hasEffect("BloodUnionEffect")) {
							int bloodUnionIncrease = SkillConfigManager.getUseSetting(hero, skill, "blood-union-increase", 1, false);
							BloodUnionEffect buEffect = (BloodUnionEffect) hero.getEffect("BloodUnionEffect");
							assert buEffect != null;
							buEffect.addBloodUnion(bloodUnionIncrease, target instanceof Player);
						}
					}
				}
			}
		});

		renderEyeBeam(player, beam, BEAM_PARTICLE, Color.RED, 60, 10, 40, 0.125, 1);

		new BukkitRunnable() {
			private float volume = 0.25f;

			@Override
			public void run() {
				if ((volume -= 0.025f) <= 0) {
					cancel();
				} else {
					player.getWorld().playSound(player.getEyeLocation(), Sound.BLOCK_LAVA_POP, volume, 0.5f);
					player.getWorld().playSound(player.getEyeLocation().add(beam.getTrajectory()), Sound.BLOCK_LAVA_POP, volume, 0.5f);
					player.getWorld().playSound(beam.midPoint().toLocation(player.getWorld()), Sound.BLOCK_LAVA_POP, volume, 0.5f);
				}
			}
		}.runTaskTimer(plugin, 0, 1);

		return SkillResult.NORMAL;
	}
}
