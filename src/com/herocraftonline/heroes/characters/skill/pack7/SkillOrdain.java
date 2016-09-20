package com.herocraftonline.heroes.characters.skill.pack7;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseBeam;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import static com.herocraftonline.heroes.characters.skill.SkillType.*;

public class SkillOrdain extends SkillBaseBeam {

	private static final float PARTICLE_OFFSET_FROM_FACE = 1;
	private static final ParticleEffect BEAM_PARTICLE = ParticleEffect.VILLAGER_HAPPY;

	public SkillOrdain(Heroes plugin) {
		super(plugin, "Ordain");
		setDescription("You summon the calling of order to project a beam that heals $1 health in its path. $2 $3");
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

		int mana = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 0, false);
		long cooldown = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 0, false);

		return getDescription()
				.replace("$1", Util.decFormat.format(beamHeal))
				.replace("$2", mana > 0 ? "Mana: " + mana : "")
				.replace("$3", cooldown > 0 ? "C: " + Util.decFormat.format((double) cooldown / 1000) : "");
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(BEAM_MAX_LENGTH_NODE, 15);
		node.set(BEAM_RADIUS_NODE, 2d);

		node.set(SkillSetting.HEALING.node(), 200);
		node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 1d);

		return node;
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
				if (target instanceof Player) {
					Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
					if (targetHero == hero || (hero.hasParty() && hero.getParty().isPartyMember(targetHero))) {
						double beamHeal = SkillConfigManager.getUseSetting(hero, SkillOrdain.this, SkillSetting.HEALING, 150d, false);
						double beamHealIncrease = SkillConfigManager.getUseSetting(hero, SkillOrdain.this, SkillSetting.HEALING_INCREASE_PER_WISDOM, 1d, false);
						beamHeal += hero.getAttributeValue(AttributeType.WISDOM) * beamHealIncrease;

						targetHero.heal(beamHeal);
					}
				}
			}
		});

		renderEyeBeam(player, beam, BEAM_PARTICLE, 60, 10, 40, 0.125, 1);

		new BukkitRunnable() {

			private float volume = 0.25f;

			@Override
			public void run() {
				if ((volume -= 0.025f) <= 0) {
					cancel();
				}
				else {
					player.getWorld().playSound(player.getEyeLocation(), CompatSound.ENTITY_EXPERIENCE_ORB_PICKUP.value(), volume, 1);
					player.getWorld().playSound(player.getEyeLocation().add(beam.getTrajectory()), CompatSound.ENTITY_EXPERIENCE_ORB_PICKUP.value(), volume, 1);
					player.getWorld().playSound(beam.midPoint().toLocation(player.getWorld()), CompatSound.ENTITY_EXPERIENCE_ORB_PICKUP.value(), volume, 1);
				}
			}
		}.runTaskTimer(plugin, 0, 1);

		return SkillResult.NORMAL;
	}
}
