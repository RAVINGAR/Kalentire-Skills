package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseGroundEffect;
import com.herocraftonline.heroes.libs.slikey.effectlib.Effect;
import com.herocraftonline.heroes.libs.slikey.effectlib.EffectType;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SkillDesecration extends SkillBaseGroundEffect {

    public SkillDesecration(final Heroes plugin) {
        super(plugin, "Desecration");
        setDescription("Marks the ground with unholy power, dealing $1 damage every $2 second(s) for $3 second(s) within $4 blocks to the side and $5 blocks up and down (cylinder). " +
                "Enemies within the area are slowed.");
        setUsage("/skill desecration");
        setIdentifiers("skill desecration");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.MULTI_GRESSIVE, SkillType.AREA_OF_EFFECT, SkillType.DAMAGING,
                SkillType.NO_SELF_TARGETTING, SkillType.SILENCEABLE, SkillType.MOVEMENT_SLOWING);
    }

    @Override
    public String getDescription(final Hero hero) {
        final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5d, false);
        final double height = SkillConfigManager.getUseSetting(hero, this, HEIGHT_NODE, 2d, false);
        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);
        final long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1000, false);

        final double damageTick = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 100d, false)
                + SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 2d, false) * hero.getAttributeValue(AttributeType.INTELLECT);

        return getDescription()
                .replace("$1", Util.decFormat.format(damageTick))
                .replace("$2", Util.decFormat.format((double) period / 1000))
                .replace("$3", Util.decFormat.format((double) duration / 1000))
                .replace("$4", Util.decFormat.format(radius))
                .replace("$5", Util.decFormat.format(height));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.RADIUS.node(), 5d);
        node.set(HEIGHT_NODE, 2d);
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set(SkillSetting.PERIOD.node(), 500);
        node.set(SkillSetting.DAMAGE_TICK.node(), 50d);
        node.set(SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT.node(), 1d);

        return node;
    }

    @Override
    public SkillResult use(final Hero hero, final String[] strings) {
        if (isAreaGroundEffectApplied(hero)) {
            return SkillResult.INVALID_TARGET_NO_MSG;
        } else {
            final Player player = hero.getPlayer();

            broadcastExecuteText(hero);

            final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5d, false);
            final double height = SkillConfigManager.getUseSetting(hero, this, HEIGHT_NODE, 2d, false);
            final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);
            final long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1000, false);

            final double damageTick = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 100d, false)
                    + SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 2d, false) * hero.getAttributeValue(AttributeType.INTELLECT);

            applyAreaGroundEffectEffect(hero, period, duration, player.getLocation(), radius, height, new GroundEffectActions() {

                @Override
                public void groundEffectTickAction(final Hero hero, final AreaGroundEffectEffect effect) {
                    final Effect e = new Effect(effectLib) {

                        final int particlesPerRadius = 3;
                        final Particle particle = Particle.REDSTONE;

                        @Override
                        public void onRun() {

                            final double inc = 1 / (particlesPerRadius * radius);

                            for (double angle = 0; angle <= 2 * Math.PI; angle += inc) {
                                final Vector v = new Vector(Math.cos(angle), 0, Math.sin(angle)).multiply(radius);
                                display(particle, getLocation().add(v));
                                getLocation().subtract(v);
                            }

                            final Location originalLocation = getLocation();
                            final Color originalColor = color;
                            color = Color.RED;

                            final int particles = (int) (2 * radius * particlesPerRadius);
                            final Vector crossXLine = new Vector(-radius * 2, 0, 0).multiply(1d / particles);
                            final Vector crossZLine = new Vector(0, 0, -radius * 2).multiply(1d / particles);

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
                    e.color = Color.BLACK;

                    e.start();
                    player.getWorld().playSound(effect.getLocation(), Sound.ENTITY_GENERIC_BURN, 0.25f, 0.0001f);
                }

                @Override
                public void groundEffectTargetAction(final Hero hero, final LivingEntity target, final AreaGroundEffectEffect groundEffect) {
                    final Player player = hero.getPlayer();
                    if (damageCheck(player, target)) {
                        damageEntity(target, player, damageTick, EntityDamageEvent.DamageCause.MAGIC, 0f);

                        final CharacterTemplate targetCt = plugin.getCharacterManager().getCharacter(target);

                        if (!targetCt.hasEffect("Slow")) {
                            final SlowEffect effect = new SlowEffect(SkillDesecration.this,
                                    player, groundEffect.getExpiry() - System.currentTimeMillis() + 200, 1, null, null);
                            targetCt.addEffect(effect);

                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    final Location targetLocation = target.getLocation();
                                    final double targetY = targetLocation.getY();
                                    targetLocation.setY(targetLocation.getY());
                                    final Location effectLocation = groundEffect.getLocation();
                                    final double groundEffectHeight = groundEffect.getHeight();

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
