package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import de.slikey.effectlib.Effect;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.EffectType;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SkillDesecration extends SkillBaseGroundEffect {

	private Set<UUID> effected = new HashSet<>();

	public SkillDesecration(Heroes plugin) {
		super(plugin, "Desecration");
		setDescription("");
		setUsage("/skill desecration");
		setIdentifiers("skill desecration");
		setArgumentRange(0, 0);
		setTypes(SkillType.MULTI_GRESSIVE, SkillType.AREA_OF_EFFECT, SkillType.DAMAGING, SkillType.NO_SELF_TARGETTING, SkillType.SILENCEABLE, SkillType.MOVEMENT_SLOWING);
	}

	@Override
	public String getDescription(Hero hero) {
		return getDescription();
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.RADIUS.node(), 5d);
		node.set(HEIGHT_NODE, 2d);
		node.set(SkillSetting.DURATION.node(), 5000);
		node.set(SkillSetting.PERIOD.node(), 500);
		node.set(SkillSetting.DAMAGE_TICK.node(), 50d);
		node.set(SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT.node(), 1d);

		return node;
	}

	@Override
	public SkillResult use(Hero hero, String[] strings) {
		if (isAreaGroundEffectApplied(hero)) {
			return SkillResult.INVALID_TARGET_NO_MSG;
		} else {
			final Player player = hero.getPlayer();

			broadcastExecuteText(hero);

			final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5d, false);
			double height = SkillConfigManager.getUseSetting(hero, this, HEIGHT_NODE, 2d, false);
			long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);
			final long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1000, false);

			final double damageTick = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 100d, false)
					+ SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 2d, false) * hero.getAttributeValue(AttributeType.INTELLECT);

			applyAreaGroundEffectEffect(hero, period, duration, player.getLocation(), radius, height, new GroundEffectActions() {

				@Override
				public void groundEffectTickAction(Hero hero, AreaGroundEffectEffect effect) {
					EffectManager em = new EffectManager(plugin);
					Effect e = new Effect(em) {

						int particlesPerRadius = 3;
						ParticleEffect particle = ParticleEffect.REDSTONE;

						@Override
						public void onRun() {

							double inc = 1 / (particlesPerRadius * radius);

							for (double angle = 0; angle <= 2 * Math.PI; angle += inc) {
								Vector v = new Vector(Math.cos(angle), 0, Math.sin(angle)).multiply(radius);
								display(particle, getLocation().add(v));
								getLocation().subtract(v);
							}

							Vector[] pentPoints = new Vector[5];

							double pentInc = (2 * Math.PI) / 5;

							for (int i = 0; i < pentPoints.length; i++) {
								pentPoints[i] = new Vector(Math.cos(pentInc * i), 0, Math.sin(pentInc * i)).multiply(radius);
							}

							Location originalLocation = getLocation();
							setLocation(originalLocation.clone());

							for (int i = 0; i < pentPoints.length; i++) {
								Vector line = pentPoints[(i + 2) % 5].clone().subtract(pentPoints[i]);
								double distance = line.length();
								int particles = (int) (distance * particlesPerRadius);
								line.multiply(1d / particles);
								setLocation(pentPoints[i].toLocation(getLocation().getWorld()).add(originalLocation));

								for (int l = 0; l < particles; l++, getLocation().add(line)) {
									display(particle, getLocation());
								}
							}

							setLocation(originalLocation);
						}
					};

					e.setLocation(effect.getLocation().clone());
					e.asynchronous = true;
					e.iterations = 1;
					e.type = EffectType.INSTANT;
					e.color = Color.RED;

					e.start();
					em.disposeOnTermination();
				}

				@Override
				public void groundEffectTargetAction(Hero hero, LivingEntity target) {
					Player player = hero.getPlayer();
					if (damageCheck(player, target)) {
						damageEntity(target, player, damageTick, EntityDamageEvent.DamageCause.MAGIC, false);

						CharacterTemplate targetCt = plugin.getCharacterManager().getCharacter(target);

						com.herocraftonline.heroes.characters.effects.Effect effect = targetCt.getEffect("Slow");
						if (effect instanceof RepeatingSlow) {

						} else {
							targetCt.addEffect(new SlowEffect(SkillDesecration.this, player, period + 500, 1));
						}
					}
				}
			});

			return SkillResult.NORMAL;
		}
	}

	private class RepeatingSlow extends SlowEffect {

		public RepeatingSlow(Skill skill, Player applier, long duration, int amplifier) {
			super(skill, applier, duration, amplifier);
		}

		@Override
		public void applyToHero(Hero hero) {
			effected.add(hero.getPlayer().getUniqueId());
		}

		@Override
		public void applyToMonster(Monster monster) {
			effected.add(monster.getEntity().getUniqueId());
		}

		@Override
		public void removeFromHero(Hero hero) {
			effected.remove(hero.getPlayer().getUniqueId());
		}

		@Override
		public void removeFromMonster(Monster monster) {
			effected.remove(monster.getEntity().getUniqueId());
		}

		//removeFrom(Hero h) { h.addEffect(new ExpirableEffect("mySkillFlag", 1000));}
		//applyToHero(Hero h) { if (!h.hasEffect("mySkillFlag")) {// Send message about being slowed here } // do stuff }
	}
}
