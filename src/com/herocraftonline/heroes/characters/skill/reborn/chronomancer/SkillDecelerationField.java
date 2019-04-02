package com.herocraftonline.heroes.characters.skill.reborn.chronomancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicEffect;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.PeriodicManaDrainEffect;
import com.herocraftonline.heroes.characters.effects.common.WalkSpeedDecreaseEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.MathUtils;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.*;
import de.slikey.effectlib.util.DynamicLocation;
import fr.neatmonster.nocheatplus.checks.moving.Velocity;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.HashMap;

public class SkillDecelerationField extends TargettedLocationSkill {

    private final static String immunityEffectName = "TemporallyWarded";
    private final static String actualEffectName = "DeceleratedTime";
    private String applyText;
    private String expireText;

    public SkillDecelerationField(Heroes plugin) {
        super(plugin, "DecelerationField");
        setDescription("You tap into the web of time, accelerating it around a target location up to $1 blocks away. " +
                "All of those within a $2 block radius, enemy or ally, will be decelerated. The field lasts $3 second(s).");
        setUsage("/skill decelerationfield");
        setArgumentRange(0, 0);
        setIdentifiers("skill decelerationfield");
        setTypes(SkillType.MULTI_GRESSIVE, SkillType.MOVEMENT_SLOWING, SkillType.AREA_OF_EFFECT);

        setToggleableEffectName("DecelerationField");
        Bukkit.getServer().getPluginManager().registerEvents(new SkillListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double maxDistance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 20.0, false);
        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 16.0, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);

        return getDescription()
                .replace("$2", Util.decFormat.format(maxDistance))
                .replace("$2", Util.decFormat.format(radius))
                .replace("$3", Util.decFormat.format(duration / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 20);
        config.set(SkillSetting.RADIUS.node(), 16.0);
        config.set(SkillSetting.DURATION.node(), 10000);
        config.set("percent-speed-decrease", 0.35);
        config.set("projectile-velocity-multiplier", 0.5);
        config.set("pulse-period", 250);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is decelerating time!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is no longer decelerating time.");
        config.set(SkillSetting.DELAY.node(), 1500);
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "    " + ChatComponents.GENERIC_SKILL + "%hero% is decelerating time!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "    " + ChatComponents.GENERIC_SKILL + "%hero% is no longer decelerating time.").replace("%hero%", "$1");
        setUseText(null);
    }

    @Override
    public SkillResult use(Hero hero, Location location, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 16.0, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        long pulsePeriod = SkillConfigManager.getUseSetting(hero, this, "pulse-period", 250, false);
        double percentDecrease = SkillConfigManager.getUseSetting(hero, this, "percent-speed-decrease", 0.35, false);
        double projectileVMulti = SkillConfigManager.getUseSetting(hero, this, "projectile-velocity-multiplier", 0.5, false);

        DeceleratedFieldEmitterEffect emitterEffect = new DeceleratedFieldEmitterEffect(this, player, pulsePeriod, duration, location, radius, percentDecrease, projectileVMulti);
        hero.addEffect(emitterEffect);

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 2.0F, 0.533F);

        return SkillResult.NORMAL;
    }

    public class DeceleratedFieldEmitterEffect extends PeriodicExpirableEffect {

        private final EffectManager effectManager;
        private final Location location;
        private final double radius;
        private final double heightRadius;
        private final double offsetHeight;
        private final double flatDecrease;
        private final double projVMulti;

        DeceleratedFieldEmitterEffect(Skill skill, Player applier, long period, long duration, Location location, double radius, double percentDecrease, double projVMulti) {
            super(skill, "DecelerationField", applier, period, duration, applyText, expireText);
            this.location = location;

            this.effectManager = new EffectManager(plugin);
            this.radius = radius;
            this.heightRadius = radius;
            this.offsetHeight = radius / 2.0;
            this.flatDecrease = Util.convertPercentageToPlayerMovementSpeedValue(percentDecrease);
            this.projVMulti = projVMulti;

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.TEMPORAL);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            Player player = hero.getPlayer();
            int durationTicks = 20 * 60 * 60;    // An hour. We'll terminate it early and I don't imagine other people will ever bother to try and maintain it that long.

            CylinderEffect effect = new CylinderEffect(effectManager);
            effect.setLocation(location);
            effect.height = (float) heightRadius;
            effect.radius = (float) radius;
            effect.period = 1;
            effect.iterations = durationTicks;

            effect.particles = 150;
            effect.particle = Particle.SPELL_MOB;
            effect.color = Color.YELLOW;
            effect.solid = false;
            effect.enableRotation = false;

            effect.asynchronous = true;
            effectManager.start(effect);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            effectManager.dispose();
        }

        @Override
        public void tickMonster(Monster monster) { }

        @Override
        public void tickHero(Hero hero) {
            decelerateField(hero);
        }

        private void decelerateField(Hero hero) {
            Player player = hero.getPlayer();

            int tempDuration = (int) (getPeriod() + 250);   // Added 250 cuz this thing is buggy as hell without it BOI
            Collection<Entity> nearbyEnts = location.getWorld().getNearbyEntities(location, radius, heightRadius, radius);
            for (Entity ent : nearbyEnts) {
                if (ent instanceof Projectile) {
                    decelerateProjectile((Projectile) ent, projVMulti);
                } else if (ent instanceof LivingEntity) {
                    LivingEntity lEnt = (LivingEntity) ent;
                    CharacterTemplate ctTarget = plugin.getCharacterManager().getCharacter(lEnt);
                    if (ctTarget == null)
                        continue;
                    if (ctTarget.hasEffect(immunityEffectName))
                        continue;
                    if (hero.isAlliedTo(lEnt)) {
                        // Only skip our allies if they are invulnerable.
                        if (ctTarget.hasEffectType(EffectType.INVULNERABILITY))
                            continue;
                    } else {
                        // If they AREN'T our ally, we just need to make sure that they can actually be damaged.
                        if (!damageCheck(player, lEnt))
                            continue;
                    }

                    ctTarget.removeEffect(ctTarget.getEffect(actualEffectName));
                    ctTarget.addEffect(new DeceleratedTimeEffect(skill, player, tempDuration, flatDecrease, projVMulti));
                }
            }
        }
    }

    public class DeceleratedTimeEffect extends WalkSpeedDecreaseEffect {
        final double projVMulti;
        boolean slowedInAirAlready;

        DeceleratedTimeEffect(Skill skill, Player applier, int duration, double flatDecrease, double projVMulti) {
            super(skill, actualEffectName, applier, duration, flatDecrease, null, null);
            this.projVMulti = projVMulti;

            types.add(EffectType.HARMFUL);
            types.add(EffectType.TEMPORAL);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
        }

        @Override
        public void applyToMonster(Monster monster) {
            addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (getDuration() / 50), 3));
            super.applyToMonster(monster);
        }
    }

    private void decelerateProjectile(Projectile proj, double multi) {
        if (proj.hasMetadata(actualEffectName))
            return;
        Vector multipliedVel = proj.getVelocity().multiply(multi);
        proj.setVelocity(multipliedVel);
        proj.setMetadata(actualEffectName, new FixedMetadataValue(plugin, true));
    }

    public class SkillListener implements Listener {

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onProjLaunch(ProjectileLaunchEvent event) {
            if (event.getEntity().getShooter() == null)
                return;
            if (!(event.getEntity().getShooter() instanceof LivingEntity))
                return;
            if (event.getEntity().hasMetadata(actualEffectName))
                return;

            LivingEntity shooter = (LivingEntity) event.getEntity().getShooter();
            CharacterTemplate ctShooter = plugin.getCharacterManager().getCharacter(shooter);
            if (ctShooter == null || !ctShooter.hasEffect(actualEffectName))
                return;

            DeceleratedTimeEffect effect = (DeceleratedTimeEffect) ctShooter.getEffect(actualEffectName);
            if (effect == null)
                return;

            decelerateProjectile(event.getEntity(), effect.projVMulti);
        }
    }
}
