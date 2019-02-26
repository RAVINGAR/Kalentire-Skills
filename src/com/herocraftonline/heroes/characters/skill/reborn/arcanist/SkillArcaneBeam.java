package com.herocraftonline.heroes.characters.skill.reborn.arcanist;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseBeam;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;

import static com.herocraftonline.heroes.characters.skill.SkillType.*;

public class SkillArcaneBeam extends SkillBaseBeam {

	private static final Particle BEAM_PARTICLE = Particle.REDSTONE;

	public SkillArcaneBeam(Heroes plugin) {
		super(plugin, "ArcaneBeam");
		setDescription("Channel a powerful beam of pure magic, dealing $1 damage to everything in its path while active.");
		setUsage("/skill arcanebeam");
		setArgumentRange(0, 0);
		setIdentifiers("skill arcanebeam");
		setTypes(ABILITY_PROPERTY_MAGICAL, DAMAGING, AGGRESSIVE, AREA_OF_EFFECT, NO_SELF_TARGETTING, UNINTERRUPTIBLE, SILENCEABLE);
	}

	@Override
	public String getDescription(Hero hero) {
		double damage = SkillConfigManager.getUseSetting(hero, SkillArcaneBeam.this, SkillSetting.DAMAGE, 150d, false);
		return getDescription().replace("$1", damage + "");
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection config = super.getDefaultConfig();
		config.set(BEAM_MAX_LENGTH_NODE, 10);
		config.set(BEAM_RADIUS_NODE, 2d);
		config.set(SkillSetting.DAMAGE.node(), 150d);
		return config;
	}

	@Override
	public SkillResult use(Hero hero, String[] strings) {
		final Player player = hero.getPlayer();

		int beamMaxLength = SkillConfigManager.getUseSetting(hero, this, BEAM_MAX_LENGTH_NODE, 15, false);
		double beamRadius = SkillConfigManager.getUseSetting(hero, this, BEAM_RADIUS_NODE, 2d, false);
		final Beam beam = createObstructedBeam(player.getEyeLocation(), beamMaxLength, beamRadius);

		broadcastExecuteText(hero);

		castBeam(hero, beam, new TargetHandler() {
			@Override
			public void handle(Hero hero, LivingEntity target, Beam.PointData pointData) {
				if (damageCheck(hero.getPlayer(), target)) {
					double damage = SkillConfigManager.getUseSetting(hero, SkillArcaneBeam.this, SkillSetting.DAMAGE, 150d, false);
					damageEntity(target, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.MAGIC, false);
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
				}
				else {
					player.getWorld().playSound(player.getEyeLocation(), Sound.BLOCK_LAVA_POP, volume, 0.5f);
					player.getWorld().playSound(player.getEyeLocation().add(beam.getTrajectory()), Sound.BLOCK_LAVA_POP, volume, 0.5f);
					player.getWorld().playSound(beam.midPoint().toLocation(player.getWorld()), Sound.BLOCK_LAVA_POP, volume, 0.5f);
				}
			}
		}.runTaskTimer(plugin, 0, 1);

		return SkillResult.NORMAL;
	}
}
