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

import static com.herocraftonline.heroes.characters.skill.SkillType.*;

public class SkillOrdain extends SkillBaseBeam {

	private static final float PARTICLE_OFFSET_FROM_FACE = 1;
	private static final ParticleEffect BEAM_PARTICLE = ParticleEffect.VILLAGER_HAPPY;

	public SkillOrdain(Heroes plugin) {
		super(plugin, "Ordain");
		setDescription("You summon the calling of order to project a beam that heals $1 health in its path.");
		setUsage("/skill ordain");
		setArgumentRange(0, 0);
		setIdentifiers("skill ordain");
		setTypes(HEALING, AREA_OF_EFFECT, NO_SELF_TARGETTING, UNINTERRUPTIBLE, SILENCEABLE);
	}

	@Override
	public String getDescription(Hero hero) {
		double beamHeal = SkillConfigManager.getUseSetting(hero, SkillOrdain.this, SkillSetting.HEALING, 150d, false);
		double beamHealIncrease = SkillConfigManager.getUseSetting(hero, SkillOrdain.this, SkillSetting.HEALING_INCREASE_PER_WISDOM, 1d, false);
		beamHeal += hero.getAttributeValue(AttributeType.WISDOM) * beamHealIncrease;

		return getDescription().replace("$1", beamHeal + "");
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SETTING_BEAM_MAX_LENGTH, 15);
		node.set(SETTING_BEAM_RADIUS, 2d);

		node.set(SkillSetting.HEALING.node(), 200);
		node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 1d);

		return node;
	}

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
				if (target instanceof Player) {
					Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
					if (hero.hasParty() && hero.getParty().isPartyMember(targetHero)) {
						double beamHeal = SkillConfigManager.getUseSetting(hero, SkillOrdain.this, SkillSetting.HEALING, 150d, false);
						double beamHealIncrease = SkillConfigManager.getUseSetting(hero, SkillOrdain.this, SkillSetting.HEALING_INCREASE_PER_WISDOM, 1d, false);
						beamHeal += hero.getAttributeValue(AttributeType.WISDOM) * beamHealIncrease;

						targetHero.heal(beamHeal);
					}
				}
			}
		});

		effect.start();
		em.disposeOnTermination();

		player.getWorld().playSound(player.getEyeLocation(), Sound.AMBIENCE_RAIN, 0.2f, 5f);
		player.getWorld().playSound(player.getEyeLocation().add(beam.getTrajectory()), Sound.AMBIENCE_RAIN, 0.2f, 5f);
		player.getWorld().playSound(beam.midPoint().toLocation(player.getWorld()), Sound.AMBIENCE_RAIN, 0.2f, 5f);

		return SkillResult.NORMAL;
	}
}