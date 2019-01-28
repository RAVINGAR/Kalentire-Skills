package com.herocraftonline.heroes.characters.skill.pack3;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseSphere;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;

public class SkillSerenity extends SkillBaseSphere {

	public SkillSerenity(Heroes plugin) {
		super(plugin, "Serenity");
		setDescription("Call upon the forces of nature to heal allies within $1 blocks for $2 every $3 seconds for $4 seconds. $5 $6");
		setUsage("/skill serenity");
		setIdentifiers("skill serenity");
		setArgumentRange(0, 0);
		//TODO edit types
		setTypes(SkillType.AREA_OF_EFFECT, SkillType.SILENCEABLE, SkillType.HEALING);
	}

	@Override
	public String getDescription(Hero hero) {
		final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5d, false);
		long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);
		long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1000, false);

		double healTick = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_TICK, 100d, false);
		healTick = getScaledHealing(hero, healTick);
		healTick += SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_TICK_INCREASE_PER_WISDOM, 2d, false) * hero.getAttributeValue(AttributeType.WISDOM);

		int mana = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 0, false);
		long cooldown = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 0, false);

		return getDescription()
				.replace("$1", Util.decFormat.format(radius))
				.replace("$2", Util.decFormat.format(healTick))
				.replace("$3", Util.decFormat.format((double) period / 1000))
				.replace("$4", Util.decFormat.format((double) duration / 1000))
				.replace("$5", mana > 0 ? "Mana: " + mana : "")
				.replace("$6", cooldown > 0 ? "C: " + Util.decFormat.format((double) cooldown / 1000) : "");
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.RADIUS.node(), 5d);
		node.set(SkillSetting.DURATION.node(), 6000);
		node.set(SkillSetting.PERIOD.node(), 1000);
		node.set(SkillSetting.HEALING_TICK.node(), 100d);
		node.set(SkillSetting.HEALING_TICK_INCREASE_PER_WISDOM.node(), 1d);

		return node;
	}

	@Override
	public SkillResult use(Hero hero, String[] strings) {
		if (isAreaSphereApplied(hero)) {
			return SkillResult.INVALID_TARGET_NO_MSG;
		} else {
			broadcastExecuteText(hero);

			final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5d, false);
			long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);
			long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1000, false);

			double healTick = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_TICK, 100d, false);
			healTick = getScaledHealing(hero, healTick);
			healTick += SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_TICK_INCREASE_PER_WISDOM, 2d, false) * hero.getAttributeValue(AttributeType.WISDOM);
			double finalHealTick = healTick;

			applyAreaSphereEffect(hero, period, duration, radius, new SphereActions() {

				@Override
				public void sphereTickAction(Hero hero, AreaSphereEffect effect) {
					renderSphere(hero.getPlayer().getEyeLocation(), radius, Particle.SPELL_MOB, Color.GREEN);
					hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 0.5f, 0.00001f);
				}

				@Override
				public void sphereTargetAction(Hero hero, Entity target) {
					if (target instanceof Player) {
						Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
						if (targetHero == hero || (hero.hasParty() && hero.getParty().isPartyMember(targetHero))) {
							targetHero.heal(finalHealTick);
						}
					}
				}
			}, EffectType.DISPELLABLE, EffectType.HEALING);

			return SkillResult.NORMAL;
		}
	}
}
