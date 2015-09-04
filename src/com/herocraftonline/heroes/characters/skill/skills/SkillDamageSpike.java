package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

public class SkillDamageSpike extends SkillBaseSpike {

	private static final String KNOCKUP_STRENGTH = "knockup-strength";

	public SkillDamageSpike(Heroes plugin) {
		super(plugin, "DamageSpike");
		setDescription("Summon a fire spike that damages foes for $1 and knocks them up into the air.");
		setUsage("/skill damagespike");
		setIdentifiers("skill damagespike");
		setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_FIRE, SkillType.AGGRESSIVE, SkillType.NO_SELF_TARGETTING, SkillType.FORCE);
	}

	@Override
	public String getDescription(Hero hero) {
		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 200d, false);
		damage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 2d, false) * hero.getAttributeValue(AttributeType.INTELLECT);

		return getDescription().replace("$1", damage + "");
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.DAMAGE.node(), 200d);
		node.set(SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT.node(), 2d);
		node.set(KNOCKUP_STRENGTH, 0.6d);

		return node;
	}

	@Override
	public SkillResult use(Hero hero, LivingEntity livingEntity, String[] strings) {
		Player player = hero.getPlayer();

		if (damageCheck(player, livingEntity)) {

			broadcastExecuteText(hero, livingEntity);

			renderSpike(livingEntity.getLocation(), 3, 0.5, ParticleEffect.FLAME);

			double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 200d, false);
			damage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 2d, false) * hero.getAttributeValue(AttributeType.INTELLECT);

			double knockUp = SkillConfigManager.getUseSetting(hero, this, KNOCKUP_STRENGTH, 2d, false);

			damageEntity(livingEntity, player, damage, EntityDamageEvent.DamageCause.MAGIC);
			livingEntity.setVelocity(new Vector(0, knockUp, 0));

			player.getWorld().playSound(hero.getPlayer().getLocation(), Sound.GHAST_FIREBALL, 5, 0.00001f);

			return SkillResult.NORMAL;
		} else {
			return SkillResult.INVALID_TARGET;
		}
	}
}
