package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.WalkSpeedDecreaseEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedLocationSkill;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.libs.slikey.effectlib.effect.CylinderEffect;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Collection;

public class SkillDecelerationField extends TargettedLocationSkill {

    private final static String immunityEffectName = "TemporallyWarded";
    private final static String actualEffectName = "DeceleratedTime";
    private String applyText;
    private String expireText;

    public SkillDecelerationField(final Heroes plugin) {
        super(plugin, "DecelerationField");
        setDescription("You tap into the web of time, decelerating everything around a target location. " +
                "All of those within a $1 block radius, enemy or ally, will be decelerated. The field lasts $2 second(s).");
        setUsage("/skill decelerationfield");
        setArgumentRange(0, 0);
        setIdentifiers("skill decelerationfield");
        setTypes(SkillType.MULTI_GRESSIVE, SkillType.MOVEMENT_SLOWING, SkillType.AREA_OF_EFFECT);

        setToggleableEffectName("DecelerationField");
        Bukkit.getServer().getPluginManager().registerEvents(new SkillListener(), plugin);
    }

    @Override
    public String getDescription(final Hero hero) {
        final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 16.0, false);
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(radius))
                .replace("$2", Util.decFormat.format((double) duration / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 18);
        config.set(ALLOW_TARGET_AIR_BLOCK_NODE, false);
        config.set(TRY_GET_SOLID_BELOW_BLOCK_NODE, true);
        config.set(MAXIMUM_FIND_SOLID_BELOW_BLOCK_HEIGHT_NODE, 7);
        config.set(SkillSetting.RADIUS.node(), 14.0);
        config.set(SkillSetting.DURATION.node(), 10000);
        config.set("percent-speed-decrease", 0.4);
        config.set("projectile-velocity-multiplier", 0.75);
        config.set("pulse-period", 250);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is decelerating time!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is no longer decelerating time.");
        config.set(SkillSetting.DELAY.node(), 1500);
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "    " + ChatComponents.GENERIC_SKILL + "%hero% is decelerating time!").replace("%hero%", "$1").replace("$hero$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "    " + ChatComponents.GENERIC_SKILL + "%hero% is no longer decelerating time.").replace("%hero%", "$1").replace("$hero$", "$1");
        setUseText(null);
    }

    @Override
    public SkillResult use(final Hero hero, final Location location, final String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 16.0, false);
        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        final long pulsePeriod = SkillConfigManager.getUseSetting(hero, this, "pulse-period", 250, false);
        final double percentDecrease = SkillConfigManager.getUseSetting(hero, this, "percent-speed-decrease", 0.35, false);
        final double projectileVMulti = SkillConfigManager.getUseSetting(hero, this, "projectile-velocity-multiplier", 0.5, false);

        final DeceleratedFieldEmitterEffect emitterEffect = new DeceleratedFieldEmitterEffect(this, player, pulsePeriod, duration, location, radius, percentDecrease, projectileVMulti);
        hero.addEffect(emitterEffect);

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 2.0F, 0.533F);

        return SkillResult.NORMAL;
    }

    private void decelerateProjectile(final Projectile proj, final double multi) {
        if (proj.hasMetadata(actualEffectName)) {
            return;
        }
        final Vector multipliedVel = proj.getVelocity().multiply(multi);
        proj.setVelocity(multipliedVel);
        proj.setMetadata(actualEffectName, new FixedMetadataValue(plugin, true));
    }

    public static class DeceleratedTimeEffect extends WalkSpeedDecreaseEffect {
        final double projVMulti;
        boolean slowedInAirAlready;

        DeceleratedTimeEffect(final Skill skill, final Player applier, final int duration, final double flatDecrease, final double projVMulti) {
            super(skill, actualEffectName, applier, duration, flatDecrease, null, null);
            this.projVMulti = projVMulti;

            types.add(EffectType.HARMFUL);
            types.add(EffectType.TEMPORAL);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
        }

        @Override
        public void applyToMonster(final Monster monster) {
            addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (getDuration() / 50), 3));
            super.applyToMonster(monster);
        }
    }

    public class DeceleratedFieldEmitterEffect extends PeriodicExpirableEffect {
        private final Location location;
        private final double radius;
        private final double heightRadius;
        private final double offsetHeight;
        private final double flatDecrease;
        private final double projVMulti;

        DeceleratedFieldEmitterEffect(final Skill skill, final Player applier, final long period, final long duration, final Location location, final double radius, final double percentDecrease, final double projVMulti) {
            super(skill, "DecelerationField", applier, period, duration, applyText, expireText);
            this.location = location;
            this.radius = radius;
            this.heightRadius = radius;
            this.offsetHeight = radius / 2.0;
            this.flatDecrease = Util.convertPercentageToPlayerMovementSpeedValue(percentDecrease);
            this.projVMulti = projVMulti;

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.TEMPORAL);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);

            final Player player = hero.getPlayer();
            final int durationTicks = 20 * 60 * 5;

            final CylinderEffect effect = new CylinderEffect(effectLib);
            effect.setLocation(location);
            effect.height = (float) heightRadius;
            effect.radius = (float) radius;
            effect.period = 1;
            effect.iterations = durationTicks;

            effect.particles = 35;
            effect.particle = Particle.SPELL_MOB;
            effect.color = Color.YELLOW;
            effect.solid = false;
            effect.enableRotation = false;

            effect.asynchronous = true;
            effectLib.start(effect);
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
        }

        @Override
        public void tickMonster(final Monster monster) {
        }

        @Override
        public void tickHero(final Hero hero) {
            decelerateField(hero);
        }

        private void decelerateField(final Hero hero) {
            final Player player = hero.getPlayer();

            final int tempDuration = (int) (getPeriod() + 250);   // Added 250 cuz this thing is buggy as hell without it BOI
            final Collection<Entity> nearbyEnts = location.getWorld().getNearbyEntities(location, radius, heightRadius, radius);
            for (final Entity ent : nearbyEnts) {
                if (ent instanceof Projectile) {
                    decelerateProjectile((Projectile) ent, projVMulti);
                } else if (ent instanceof LivingEntity) {
                    final LivingEntity lEnt = (LivingEntity) ent;
                    final CharacterTemplate ctTarget = plugin.getCharacterManager().getCharacter(lEnt);
                    if (ctTarget == null) {
                        continue;
                    }
                    if (ctTarget.hasEffect(immunityEffectName)) {
                        continue;
                    }
                    if (!hero.isAlliedTo(lEnt) && !damageCheck(player, lEnt)) {
                        continue;
                    }

                    ctTarget.removeEffect(ctTarget.getEffect(actualEffectName));
                    ctTarget.addEffect(new DeceleratedTimeEffect(skill, player, tempDuration, flatDecrease, projVMulti));
                }
            }
        }
    }

    public class SkillListener implements Listener {

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onProjLaunch(final ProjectileLaunchEvent event) {
            if (event.getEntity().getShooter() == null) {
                return;
            }
            if (!(event.getEntity().getShooter() instanceof LivingEntity)) {
                return;
            }
            if (event.getEntity().hasMetadata(actualEffectName)) {
                return;
            }

            final LivingEntity shooter = (LivingEntity) event.getEntity().getShooter();
            final CharacterTemplate ctShooter = plugin.getCharacterManager().getCharacter(shooter);
            if (ctShooter == null || !ctShooter.hasEffect(actualEffectName)) {
                return;
            }

            final DeceleratedTimeEffect effect = (DeceleratedTimeEffect) ctShooter.getEffect(actualEffectName);
            if (effect == null) {
                return;
            }

            decelerateProjectile(event.getEntity(), effect.projVMulti);
        }
    }
}
