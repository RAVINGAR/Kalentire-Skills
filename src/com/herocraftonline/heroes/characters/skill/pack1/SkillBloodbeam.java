package com.herocraftonline.heroes.characters.skill.pack1;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseBeam;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;

import static com.herocraftonline.heroes.characters.skill.SkillType.*;

public class SkillBloodbeam extends SkillBaseBeam {

	private static final Particle BEAM_PARTICLE = Particle.REDSTONE;

	public SkillBloodbeam(Heroes plugin) {
		super(plugin, "BloodBeam");
		setDescription("Surging with blood, you fire off a beam that deals $1 damage to everything in its path. $2 $3");
		setUsage("/skill bloodbeam");
		setArgumentRange(0, 0);
		setIdentifiers("skill bloodbeam");
		setTypes(ABILITY_PROPERTY_MAGICAL, DAMAGING, MULTI_GRESSIVE, AREA_OF_EFFECT, NO_SELF_TARGETTING, UNINTERRUPTIBLE, SILENCEABLE, ABILITY_PROPERTY_PROJECTILE, ABILITY_PROPERTY_BLEED);
	}

	@Override
	public String getDescription(Hero hero) {
		double beamDamage = SkillConfigManager.getUseSetting(hero, SkillBloodbeam.this, SkillSetting.DAMAGE, 150d, false);
		double beamDamageIncrease = SkillConfigManager.getUseSetting(hero, SkillBloodbeam.this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 5d, false);
		beamDamage += hero.getAttributeValue(AttributeType.INTELLECT) * beamDamageIncrease;

		int mana = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 0, false);
		long cooldown = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 0, false);

		return getDescription()
				.replace("$1", Util.decFormat.format(beamDamage))
				.replace("$2", mana > 0 ? "Mana: " + mana : "")
				.replace("$3", cooldown > 0 ? "C: " + Util.decFormat.format((double) cooldown / 1000) : "");
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(BEAM_MAX_LENGTH_NODE, 15);
		node.set(BEAM_RADIUS_NODE, 2d);

		node.set(SkillSetting.DAMAGE.node(), 150d);
		node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1d);

		return node;
	}

	@Override
	public SkillResult use(Hero hero, String[] strings) {
		final Player player = hero.getPlayer();
	//public SkillResult use(Hero hero, LivingEntity target, String[] args) {

		int beamMaxLength = SkillConfigManager.getUseSetting(hero, this, BEAM_MAX_LENGTH_NODE, 15, false);
		double beamRadius = SkillConfigManager.getUseSetting(hero, this, BEAM_RADIUS_NODE, 2d, false);
		final Beam beam = createObstructedBeam(player.getEyeLocation(), beamMaxLength, beamRadius);

		broadcastExecuteText(hero);

		castBeam(hero, beam, new TargetHandler() {
			@Override
			public void handle(Hero hero, LivingEntity target, Beam.PointData pointData) {
				if (damageCheck(hero.getPlayer(), target)) {
					double beamDamage = SkillConfigManager.getUseSetting(hero, SkillBloodbeam.this, SkillSetting.DAMAGE, 150d, false);
					double beamDamageIncrease = SkillConfigManager.getUseSetting(hero, SkillBloodbeam.this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1d, false);
					beamDamage += hero.getAttributeValue(AttributeType.INTELLECT) * beamDamageIncrease;

					damageEntity(target, hero.getPlayer(), beamDamage, EntityDamageEvent.DamageCause.MAGIC, false);
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

		/* Increase Blood Union
		if (hero.hasEffect("BloodUnionEffect")) {
			int bloodUnionIncrease = SkillConfigManager.getUseSetting(hero, this, "blood-union-increase", 1, false);
			BloodUnionEffect buEffect = (BloodUnionEffect) hero.getEffect("BloodUnionEffect");
			assert buEffect != null;
			buEffect.addBloodUnion(bloodUnionIncrease, true);
		}
 		*/
		return SkillResult.NORMAL;
	}
}
