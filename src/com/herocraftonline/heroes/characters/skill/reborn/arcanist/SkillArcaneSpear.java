package com.herocraftonline.heroes.characters.skill.reborn.arcanist;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseBeam;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class SkillArcaneSpear extends SkillBaseBeam {

	public SkillArcaneSpear(Heroes plugin) {
		super(plugin, "ArcaneSpear");
		setDescription("Conjure a powerful spear of pure magic, dealing $1 damage to everything in its path.");
		setUsage("/skill arcanespear");
		setArgumentRange(0, 0);
		setIdentifiers("skill arcanespear");
		setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT, SkillType.NO_SELF_TARGETTING, SkillType.UNINTERRUPTIBLE, SkillType.SILENCEABLE);
	}

	@Override
	public String getDescription(Hero hero) {
		double damage = SkillConfigManager.getUseSetting(hero, SkillArcaneSpear.this, SkillSetting.DAMAGE, 150d, false);
		return getDescription().replace("$1", damage + "");
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection config = super.getDefaultConfig();
		config.set(BEAM_MAX_LENGTH_NODE, 8);
		config.set(BEAM_RADIUS_NODE, 1d);
		config.set(SkillSetting.DAMAGE.node(), 75.0);
		return config;
	}

	@Override
	public SkillResult use(Hero hero, String[] strings) {
		final Player player = hero.getPlayer();

		int beamMaxLength = SkillConfigManager.getUseSetting(hero, this, BEAM_MAX_LENGTH_NODE, 8, false);
		double beamRadius = SkillConfigManager.getUseSetting(hero, this, BEAM_RADIUS_NODE, 1d, false);
		final Beam beam = createObstructedBeam(player.getEyeLocation(), beamMaxLength, beamRadius);

		broadcastExecuteText(hero);

		castBeam(hero, beam, new TargetHandler() {
			@Override
			public void handle(Hero hero, LivingEntity target, Beam.PointData pointData) {
				if (damageCheck(hero.getPlayer(), target)) {
					double damage = SkillConfigManager.getUseSetting(hero, SkillArcaneSpear.this, SkillSetting.DAMAGE, 75.0, false);
					damageEntity(target, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.MAGIC, false);
				}
			}
		});

		renderEyeBeam(player, beam, Particle.REDSTONE, Color.PURPLE, 60, 10, 40, 0.125, 1);

		new BukkitRunnable() {
			private float volume = 0.25f;

			@Override
			public void run() {
				if ((volume -= 0.025f) <= 0) {
					cancel();
				}
				else {
					player.getWorld().playSound(player.getEyeLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, volume, 0.533f);
					player.getWorld().playSound(player.getEyeLocation().add(beam.getTrajectory()), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, volume, 0.533f);
					player.getWorld().playSound(beam.midPoint().toLocation(player.getWorld()), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, volume, 0.533f);
				}
			}
		}.runTaskTimer(plugin, 0, 1);

		return SkillResult.NORMAL;
	}
}
