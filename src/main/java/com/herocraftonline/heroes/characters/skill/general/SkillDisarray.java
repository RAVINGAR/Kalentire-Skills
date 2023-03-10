package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseBeam;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDamageEvent;

import static com.herocraftonline.heroes.characters.skill.SkillType.*;

public class SkillDisarray extends SkillBaseBeam {

	private static final Particle BEAM_PARTICLE = Particle.SPELL;
	
	public SkillDisarray(Heroes plugin) {
		super(plugin, "Disarray");
		setDescription("Surging with chaos, you fire off a beam that deals $1 damage to everything in its path.");
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
				.replace("$1", Util.decFormat.format(beamDamage));
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
		Player player = hero.getPlayer();

		int beamMaxLength = SkillConfigManager.getUseSetting(hero, this, BEAM_MAX_LENGTH_NODE, 15, false);
		double beamRadius = SkillConfigManager.getUseSetting(hero, this, BEAM_RADIUS_NODE, 2d, false);
		Beam beam = createObstructedBeam(player.getEyeLocation(), beamMaxLength, beamRadius);

		broadcastExecuteText(hero);

		castBeam(hero, beam, (hero1, target, pointData) -> {
			if (damageCheck(hero1.getPlayer(), target)) {
				double beamDamage = SkillConfigManager.getUseSetting(hero1, SkillDisarray.this, SkillSetting.DAMAGE, 150d, false);
				double beamDamageIncrease = SkillConfigManager.getUseSetting(hero1, SkillDisarray.this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1d, false);
				beamDamage += hero1.getAttributeValue(AttributeType.INTELLECT) * beamDamageIncrease;

				damageEntity(target, hero1.getPlayer(), beamDamage, EntityDamageEvent.DamageCause.MAGIC, false);
			}
		});

		renderEyeBeam(player, beam, BEAM_PARTICLE, Color.RED, 60, 10, 40, 0.125, 1);

		player.getWorld().playSound(player.getEyeLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 0.2f, 0.0001f);
		player.getWorld().playSound(player.getEyeLocation().add(beam.getTrajectory()), Sound.ENTITY_ENDERMAN_SCREAM, 0.2f, 0.0001f);
		player.getWorld().playSound(beam.midPoint().toLocation(player.getWorld()), Sound.ENTITY_ENDERMAN_SCREAM, 0.2f, 0.0001f);

		return SkillResult.NORMAL;
	}
}
