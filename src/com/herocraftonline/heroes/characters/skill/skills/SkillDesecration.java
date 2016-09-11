package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.Effect;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.EffectType;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SkillDesecration extends SkillBaseGroundEffect {

	public SkillDesecration(Heroes plugin) {
		super(plugin, "Desecration");
		setDescription("Marks the ground with unholy power, dealing $1 damage every $2 seconds for $3 seconds within $4 blocks to the side and $5 blocks up and down (cylinder). " +
				"Enemies within the area are slowed. $6 $7");
		setUsage("/skill desecration");
		setIdentifiers("skill desecration");
		setArgumentRange(0, 0);
		setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.MULTI_GRESSIVE, SkillType.AREA_OF_EFFECT, SkillType.DAMAGING, SkillType.NO_SELF_TARGETTING, SkillType.SILENCEABLE, SkillType.MOVEMENT_SLOWING);
	}

	@Override
	public String getDescription(Hero hero) {
		final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5d, false);
		double height = SkillConfigManager.getUseSetting(hero, this, HEIGHT_NODE, 2d, false);
		long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);
		final long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1000, false);

		final double damageTick = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 100d, false)
				+ SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 2d, false) * hero.getAttributeValue(AttributeType.INTELLECT);

		int mana = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 0, false);
		long cooldown = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 0, false);

		return getDescription()
				.replace("$1", Util.decFormat.format(damageTick))
				.replace("$2", Util.decFormat.format((double) period / 1000))
				.replace("$3", Util.decFormat.format((double) duration / 1000))
				.replace("$4", Util.decFormat.format(radius))
				.replace("$5", Util.decFormat.format(height))
				.replace("$6", mana > 0 ? "Mana: " + mana : "")
				.replace("$7", cooldown > 0 ? "C: " + Util.decFormat.format((double) cooldown / 1000) : "");
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
							Color originalColor = color;
							color = Color.RED;

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
							color = originalColor;
						}
					};

					e.setLocation(effect.getLocation().clone());
					e.asynchronous = true;
					e.iterations = 1;
					e.type = EffectType.INSTANT;
					e.color = Color.BLACK;

					e.start();
					em.disposeOnTermination();

					player.getWorld().playSound(effect.getLocation(), CompatSound.ENTITY_CAT_HISS.value(), 0.25f, 0.1f);
				}

				@Override
				public void groundEffectTargetAction(Hero hero, final LivingEntity target, final AreaGroundEffectEffect groundEffect) {
					final Player player = hero.getPlayer();
					if (damageCheck(player, target)) {
						damageEntity(target, player, damageTick, EntityDamageEvent.DamageCause.MAGIC, false);

						final CharacterTemplate targetCt = plugin.getCharacterManager().getCharacter(target);

						if (!targetCt.hasEffect("Slow")) {
							final SlowEffect effect = new SlowEffect(SkillDesecration.this,
									player, groundEffect.getExpiry() - System.currentTimeMillis() + 200, 1, null, null);
							targetCt.addEffect(effect);

							new BukkitRunnable() {
								@Override
								public void run() {
									Location targetLocation = target.getLocation();
									double targetY = targetLocation.getY();
									targetLocation.setY(targetLocation.getY());
									Location effectLocation = groundEffect.getLocation();
									double groundEffectHeight = groundEffect.getHeight();

									if (groundEffect.isExpired() || effectLocation.distanceSquared(targetLocation) > radius * radius ||
											targetY > effectLocation.getY() + groundEffectHeight || targetY < effectLocation.getY() - groundEffectHeight) {
										targetCt.removeEffect(effect);
										cancel();
									}
								}
							}.runTaskTimer(plugin, 4, 4);
						}
					}
				}
			});

			return SkillResult.NORMAL;
		}
	}
}
