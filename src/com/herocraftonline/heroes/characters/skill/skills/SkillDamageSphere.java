package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicEffect;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.SphereEffect;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import static com.herocraftonline.heroes.characters.skill.SkillType.*;

public class SkillDamageSphere extends SkillBaseSphere {

	public SkillDamageSphere(Heroes plugin) {
		super(plugin, "DamageSphere");
		setDescription("Creates a sphere of flame around the caster that lasts $2 seconds, "
				+ "damaging enemies within a $1 block radius for %4 every $3 seconds.");
		setUsage("/skill damagesphere");
		setIdentifiers("skill damagesphere");
		setArgumentRange(0, 0);
		setTypes(ABILITY_PROPERTY_FIRE, AGGRESSIVE, AREA_OF_EFFECT, ARMOR_PIERCING, DAMAGING, FORCE, MULTI_GRESSIVE, NO_SELF_TARGETTING, SILENCEABLE, UNINTERRUPTIBLE);
	}

	@Override
	public String getDescription(Hero hero) {
		double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5d, false);
		long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);
		long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1000, false);

		double damageTick = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 100d, false);
		damageTick += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 2d, false) * hero.getAttributeValue(AttributeType.INTELLECT);

		return getDescription().replace("$1", radius + "").replace("$2", duration + "").replace("$3", period + "").replace("$4", damageTick + "");
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.RADIUS.node(), 5d);
		node.set(SkillSetting.DURATION.node(), 6000);
		node.set(SkillSetting.PERIOD.node(), 1000);
		node.set(SkillSetting.DAMAGE_TICK.node(), 100d);
		node.set(SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT.node(), 2d);

		return node;
	}

	@Override
	public SkillResult use(Hero hero, String[] strings) {
		if (hero.hasEffect("DamageSphereEffect")) {
			return SkillResult.FAIL;
		}
		else {
			broadcastExecuteText(hero);

			double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5d, false);
			long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);
			long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1000, false);

			double damageTick = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 100d, false);
			damageTick += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 2d, false) * hero.getAttributeValue(AttributeType.INTELLECT);

			hero.addEffect(new DamageSphereEffect(radius, duration, hero.getPlayer(), period, damageTick));

			return SkillResult.NORMAL;
		}
	}

	public class DamageSphereEffect extends PeriodicExpirableEffect {

		private final double radius;
		private final double damageTick;

		public DamageSphereEffect(double radius, long duration, Player player, long period, double damageTick) {
			super(SkillDamageSphere.this, "DamageSphereEffect", player, period, duration);
			this.radius = radius;
			this.damageTick = damageTick;

			types.add(EffectType.AREA_OF_EFFECT);
			types.add(EffectType.FIRE);
			types.add(EffectType.FORCE);
			types.add(EffectType.UNTARGETABLE);
			types.add(EffectType.UNBREAKABLE);
		}

		@Override
		public void tickMonster(Monster monster) {
			throw new RuntimeException("This should never be on a moster... atleast on the prototype");
		}

		@Override
		public void tickHero(Hero hero) {
			castSphere(hero, radius, new TargetHandler() {
				@Override
				public void handle(Hero hero, Entity target) {
					Player player = hero.getPlayer();
					if (target instanceof LivingEntity) {
						LivingEntity livingTarget = (LivingEntity) target;
						if (damageCheck(player, livingTarget)) {
							damageEntity(livingTarget, player, damageTick, EntityDamageEvent.DamageCause.MAGIC, true);
						}
					}
				}
			});

			EffectManager em = new EffectManager(plugin);
			SphereEffect effect = new SphereEffect(em);

			effect.setLocation(hero.getPlayer().getLocation());
			effect.radius = radius;
			effect.particle = ParticleEffect.FLAME;
			effect.particles = (int) radius * 100;
			effect.type = de.slikey.effectlib.EffectType.INSTANT;
			effect.iterations = 1;
			effect.asynchronous = true;

			effect.start();
			em.disposeOnTermination();

			hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.GHAST_FIREBALL, 5, 0.00001f);
		}
	}
}
