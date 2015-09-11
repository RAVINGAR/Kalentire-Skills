package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.util.Util;
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
		setDescription("Surging with chaos, you fire off a beam that deals $1 damage to everything in its path. $2 $3");
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

	static boolean test;

	@Override
	public SkillResult use(Hero hero, String[] strings) {
		Player player = hero.getPlayer();

		int beamMaxLength = SkillConfigManager.getUseSetting(hero, this, BEAM_MAX_LENGTH_NODE, 15, false);
		double beamRadius = SkillConfigManager.getUseSetting(hero, this, BEAM_RADIUS_NODE, 2d, false);
		Beam beam = createObstructedBeam(player.getEyeLocation(), beamMaxLength, beamRadius);

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

		renderEyeBeam(player, beam, BEAM_PARTICLE, 60, 10, 40, 0.125, 1);

		player.getWorld().playSound(player.getEyeLocation(), Sound.ENDERMAN_SCREAM, 0.2f, 0.0001f);
		player.getWorld().playSound(player.getEyeLocation().add(beam.getTrajectory()), Sound.ENDERMAN_SCREAM, 0.2f, 0.0001f);
		player.getWorld().playSound(beam.midPoint().toLocation(player.getWorld()), Sound.ENDERMAN_SCREAM, 0.2f, 0.0001f);

		return SkillResult.NORMAL;
	}
}
