package com.herocraftonline.heroes.characters.skill.pack1;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SilenceEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseSpike;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

public class SkillInfernoBlade extends SkillBaseSpike {

	private static final Particle PARTICLE = Particle.FLAME;

	public SkillInfernoBlade(Heroes plugin) {
		super(plugin, "InfernoBlade");
		setDescription("Impales the target with a spike of fire silencing them for $1 seconds, dealing $2 damage.");
		setUsage("/skill infernoblade");
		setIdentifiers("skill infernoblade");
		setArgumentRange(0, 0);
		setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.NO_SELF_TARGETTING, SkillType.SILENCING, SkillType.ABILITY_PROPERTY_FIRE);
	}

	@Override
	public String getDescription(Hero hero) {
		int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false) / 1000;

		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 250d, false);
		damage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1d, false) * hero.getAttributeValue(AttributeType.INTELLECT);

		return getDescription()
				.replace("$1", Util.decFormat.format((double) duration / 1000))
				.replace("$2", Util.decFormat.format(damage));
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.MAX_DISTANCE.node(), 8d);
		node.set(SkillSetting.DAMAGE.node(), 250d);
		node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1d);

		node.set(SPIKE_HEIGHT_NODE, 3d);
		node.set(DOES_KNOCK_UP_NODE, true);
		node.set(KNOCK_UP_STRENGTH_NODE, 0.6);

		node.set(SkillSetting.DURATION.node(), 5000);

		return node;
	}

	@Override
	public SkillResult use(Hero hero, LivingEntity target, String[] strings) {
		Player player = hero.getPlayer();

		if (damageCheck(player, target)) {

			broadcastExecuteText(hero, target);

			double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 250d, false);
			damage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1d, false) * hero.getAttributeValue(AttributeType.INTELLECT);
			damageEntity(target, player, damage, EntityDamageEvent.DamageCause.MAGIC, false);

			CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
			int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
			SilenceEffect effect = new SilenceEffect(this, getName(), player, duration);
			targetCT.addEffect(effect);

			double spikeHeight = SkillConfigManager.getUseSetting(hero, this, SPIKE_HEIGHT_NODE, 3d, false);
			renderSpike(target.getLocation(), spikeHeight, BLOCK_SPIKE_RADIUS, PARTICLE);

			if (SkillConfigManager.getUseSetting(hero, this, DOES_KNOCK_UP_NODE, true)) {
				Vector knockUpVector = new Vector(0, SkillConfigManager.getUseSetting(hero, this, KNOCK_UP_STRENGTH_NODE, 0.6, false), 0);
				target.setVelocity(target.getVelocity().add(knockUpVector));
			}

			target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GHAST_SHOOT, 0.2f, 0.00001f);

			return SkillResult.NORMAL;
		} else {
			return SkillResult.INVALID_TARGET;
		}
	}
}
