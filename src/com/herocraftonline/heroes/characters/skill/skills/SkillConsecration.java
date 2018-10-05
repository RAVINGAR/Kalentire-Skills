package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.effects.common.SpeedEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SkillConsecration extends SkillBaseGroundEffect {

	public SkillConsecration(Heroes plugin) {
		super(plugin, "Consecration");
		setDescription("Marks the ground with holy power, dealing $1 damage to undead every $2 seconds for $3 seconds within $4 blocks blocks to the side and $5 blocks up and down (cylinder). " +
				"Allies within the area are granted movement speed. $6 $7");
		setUsage("/skill consecration");
		setIdentifiers("skill consecration");
		setArgumentRange(0, 0);
		setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.MULTI_GRESSIVE, SkillType.AREA_OF_EFFECT, SkillType.DAMAGING, SkillType.NO_SELF_TARGETTING, SkillType.SILENCEABLE, SkillType.MOVEMENT_INCREASING);
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

							Location originalLocation = getLocation();
							Color originalColor = color;
							color = Color.BLUE;

							int particles = (int) (2 * radius * particlesPerRadius);
							Vector crossXLine = new Vector(-radius * 2, 0, 0).multiply(1d / particles);
							Vector crossZLine = new Vector(0, 0, -radius * 2).multiply(1d / particles);

							setLocation(new Vector(radius, 0, radius / 10).toLocation(getLocation().getWorld()).add(originalLocation));
							for (int l = 0; l < particles; l++, getLocation().add(crossXLine)) {
								display(particle, getLocation());
							}

							setLocation(new Vector(radius / 10, 0, radius).toLocation(getLocation().getWorld()).add(originalLocation));
							for (int l = 0; l < particles; l++, getLocation().add(crossZLine)) {
								display(particle, getLocation());
							}

							setLocation(new Vector(radius, 0, radius / -10).toLocation(getLocation().getWorld()).add(originalLocation));
							for (int l = 0; l < particles; l++, getLocation().add(crossXLine)) {
								display(particle, getLocation());
							}

							setLocation(new Vector(radius / -10, 0, radius).toLocation(getLocation().getWorld()).add(originalLocation));
							for (int l = 0; l < particles; l++, getLocation().add(crossZLine)) {
								display(particle, getLocation());
							}

							setLocation(originalLocation);
							color = originalColor;
						}
					};

					e.setLocation(effect.getLocation().clone());
					e.asynchronous = true;
					e.iterations = 1;
					e.type = EffectType.INSTANT;
					e.color = Color.YELLOW;

					e.start();
					em.disposeOnTermination();

					player.getWorld().playSound(effect.getLocation(), Sound.ENTITY_GENERIC_BURN.value(), 0.25f, 0.0001f);
				}

				@Override
				public void groundEffectTargetAction(Hero hero, final LivingEntity target, final AreaGroundEffectEffect groundEffect) {
					Player player = hero.getPlayer();

					// Code from HolyWater to damage Undead mobs
					if (!(target instanceof Player)) {
						if (Util.isUndead(plugin, target) && damageCheck(player, target)) {
							damageEntity(target, player, damageTick, EntityDamageEvent.DamageCause.MAGIC, false);
						}
					}

					// Original Consecration code for allies
					else {
						Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
						if (targetHero == hero || (hero.hasParty() && hero.getParty().isPartyMember(targetHero))) {

							final CharacterTemplate targetCt = plugin.getCharacterManager().getCharacter(target);

							if (!targetCt.hasEffect("Speed")) {
								final SpeedEffect effect = new SpeedEffect(SkillConsecration.this,
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
				}
			});

			return SkillResult.NORMAL;
		}
	}
}
