package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseMarkedTeleport;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillTimeWarp extends SkillBaseMarkedTeleport {

	public SkillTimeWarp(Heroes plugin) {
		super(plugin, "TimeWarp", false, new EffectType[] {
				EffectType.DAMAGING,
				EffectType.HARMFUL
		}, Particle.REDSTONE, new Color[] {
				Color.PURPLE,
				Color.BLACK
		});
		setDescription("Mark your target's current position in time for the next $1 seconds. At any point during that time you may re activate the skill to teleport" +
				" the target back to that location dealing an amount of damage starting at $2 and decaying towards 0 as the skills duration reaches end. If you do not" +
				" re activate the skill within the duration no damage is dealt and no teleport occurs. $4 $5");
		setUsage("/skill timewarp");
		setIdentifiers("skill timewarp");

		setTypes(SkillType.AGGRESSIVE, SkillType.DAMAGING, SkillType.NO_SELF_TARGETTING, SkillType.TELEPORTING);
	}

	@Override
	public String getDescription(Hero hero) {
		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 250d, false);
		double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1d, false);
		damage += hero.getAttributeValue(AttributeType.INTELLECT) * damageIncrease;

		double totalDuration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);

		int mana = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 0, false);
		long cooldown = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 0, false);

		return getDescription()
				.replace("$1", Util.decFormat.format(totalDuration / 1000))
				.replace("$2", Util.decFormat.format(damage))
				.replace("$4", mana > 0 ? "Mana: " + mana : "")
				.replace("$5", cooldown > 0 ? "C: " + Util.decFormat.format((double) cooldown / 1000) : "");
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.DURATION.node(), 10000);
		node.set(SkillSetting.DAMAGE.node(), 250d);
		node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1);

		node.set(PRESERVE_LOOK_DIRECTION_NODE, true);
		node.set(PRESERVE_VELOCITY_NODE, true);

		return node;
	}

	@Override
	protected void onMarkerActivate(Marker marker, long activateTime) {
		if (damageCheck(marker.getHero().getPlayer(), marker.getTarget().getEntity())) {
			double damage = SkillConfigManager.getUseSetting(marker.getHero(), this, SkillSetting.DAMAGE, 250d, false);
			double damageIncrease = SkillConfigManager.getUseSetting(marker.getHero(), this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1d, false);
			damage += marker.getHero().getAttributeValue(AttributeType.INTELLECT) * damageIncrease;
			long reCastDelay = SkillConfigManager.getUseSetting(marker.getHero(), this, RE_CAST_DELAY_NODE, 0, false);

			double totalDuration = SkillConfigManager.getUseSetting(marker.getHero(), this, SkillSetting.DURATION, 10000, false);
			double damageScale = 1 - ((activateTime - marker.getCreateTime() - reCastDelay) / (totalDuration - reCastDelay));
			if (damageScale < 0) {
				damageScale = 0;
			}

			damageEntity(marker.getTarget().getEntity(), marker.getHero().getPlayer(), damage * damageScale, EntityDamageEvent.DamageCause.MAGIC, false);
		}
	}
}
