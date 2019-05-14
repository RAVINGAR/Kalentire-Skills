package com.herocraftonline.heroes.characters.skill.reborn.chronomancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.WalkSpeedDecreaseEffect;
import com.herocraftonline.heroes.characters.effects.common.WalkSpeedIncreaseEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.CylinderEffect;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;

public class SkillDistortionField extends TargettedLocationSkill {
    private final String accelerateEffectName = "AcceleratedTime";
    private final String decelerateEffectName = "DeceleratedTime";
    private final String immunityEffectName = "TemporallyWarded";
    private String applyText;
    private String expireText;

    public SkillDistortionField(Heroes plugin) {
        super(plugin, "DistortionField");
        setDescription("You tap into the web of time and distort time within a $1 block radius."
                + "Any ally will be accelerated and any enemy will be decelerated. The field lasts $2 second(s).");
        setUsage("/skill distortionfield");
        setArgumentRange(0, 0);
        setIdentifiers("skill distortionfield");
        setTypes(SkillType.ABILITY_PROPERTY_TEMPORAL, SkillType.MULTI_GRESSIVE, SkillType.MOVEMENT_INCREASING,
                SkillType.MOVEMENT_SLOWING, SkillType.AREA_OF_EFFECT, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        double radius = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.RADIUS, false);
        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(radius))
                .replace("$2", Util.decFormat.format((double) duration / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 18.0);
        config.set(ALLOW_TARGET_AIR_BLOCK_NODE, false);
        config.set(TRY_GET_SOLID_BELOW_BLOCK_NODE, true);
        config.set(MAXIMUM_FIND_SOLID_BELOW_BLOCK_HEIGHT_NODE, 7);
        config.set(SkillSetting.RADIUS.node(), 14.0);
        config.set(SkillSetting.DURATION.node(), 10000);
        config.set("percent-speed-increase", 0.35);
        config.set("percent-speed-decrease", 0.4);
        config.set("pulse-period", 250);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is distorting time!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is no longer distorting time.");
        config.set(SkillSetting.DELAY.node(), 1500);
        return config;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "    " + ChatComponents.GENERIC_SKILL + "%hero% is accelerating time!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "    " + ChatComponents.GENERIC_SKILL + "%hero% is no longer accelerating time.").replace("%hero%", "$1");
        setUseText(null);
    }

    @Override
    public SkillResult use(Hero hero, Location targetLoc, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        double radius = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.RADIUS, false);
        long duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        int pulsePeriod = SkillConfigManager.getUseSetting(hero, this, "pulse-period", 250, false);
        double percentIncrease = SkillConfigManager.getUseSetting(hero, this, "percent-speed-increase", 0.35, false);
        double projectileVMultiIncrease = SkillConfigManager.getUseSetting(hero, this, "projectile-velocity-multiplier-accelerate", 1.35, false);
        double percentDecrease = SkillConfigManager.getUseSetting(hero, this, "percent-speed-decrease", 0.35, false);
        double projectileVMultiDecrease = SkillConfigManager.getUseSetting(hero, this, "projectile-velocity-multiplier-decelerate", 0.5, false);

        AcceleratedFieldEmitterEffect emitterEffect = new AcceleratedFieldEmitterEffect(this, player, pulsePeriod,
                duration, targetLoc, radius,
                percentIncrease, projectileVMultiIncrease,
                percentDecrease, projectileVMultiDecrease);
        hero.addEffect(emitterEffect);

//        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 2.0F, 2.0F);

        return SkillResult.NORMAL;
    }

    public class AcceleratedFieldEmitterEffect extends PeriodicExpirableEffect {

        private final EffectManager effectManager;
        private final Location location;
        private final double radius;
        private final double heightRadius;
        private final double offsetHeight;
        private final double flatSpeedIncrease;
        private final double projVMultiIncrease;
        private final double flatDecrease;
        private final double projVMultiDecrease;

        AcceleratedFieldEmitterEffect(Skill skill, Player applier, int period, long duration, Location location,
                                      double radius, double percentIncrease,
                                      double projVMultiIncrease,double percentDecrease, double projVMultiDecrease) {
            super(skill, "AccelerationField", applier, period, duration, applyText, expireText);
            this.location = location;

            this.effectManager = new EffectManager(plugin);
            this.radius = radius;
            this.heightRadius = radius;
            this.offsetHeight = radius / 2.0;
            this.flatSpeedIncrease = Util.convertPercentageToPlayerMovementSpeedValue(percentIncrease);
            this.projVMultiIncrease = projVMultiIncrease;
            this.flatDecrease = Util.convertPercentageToPlayerMovementSpeedValue(percentDecrease);
            this.projVMultiDecrease = projVMultiDecrease;

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.TEMPORAL);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            Player player = hero.getPlayer();
            int durationTicks = 20 * 60 * 60;    // An hour. We'll most likely terminate it early and I don't imagine other people will ever bother to try and maintain it that long.

            CylinderEffect effect = new CylinderEffect(effectManager);
            effect.setLocation(location);
            effect.height = (float) heightRadius;
            effect.radius = (float) radius;
            effect.period = 1;
            effect.iterations = durationTicks;

            effect.particles = 35;
            effect.particle = Particle.SPELL_MOB;
            effect.color = Color.TEAL;
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
            accelerateField(hero);
        }

        private void accelerateField(Hero hero) {
            Player player = hero.getPlayer();

            int tempDuration = (int) (getPeriod() + 250);   // Added 250 cuz this thing is buggy as hell without it BOI
            Collection<Entity> nearbyEnts = location.getWorld().getNearbyEntities(location, radius, heightRadius, radius);
            for (Entity ent : nearbyEnts) {
                if (ent instanceof LivingEntity) {
                    CharacterTemplate ctTarget = plugin.getCharacterManager().getCharacter((LivingEntity) ent);
                    if (ctTarget == null)
                        continue;
                    if (ctTarget.hasEffect(immunityEffectName))
                        continue;

                    LivingEntity target = (LivingEntity) ent;
                    if (hero.isAlliedTo(target)) {
                        ctTarget.removeEffect(ctTarget.getEffect(accelerateEffectName));
                        ctTarget.addEffect(new AcceleratedTimeEffect(skill, player, tempDuration, flatSpeedIncrease, projVMultiIncrease));
                    } else {
                        if (!hero.isAlliedTo(target)) {
                            if (!damageCheck(player, target))
                                continue;
                            ctTarget.removeEffect(ctTarget.getEffect(decelerateEffectName));
                            ctTarget.addEffect(new DeceleratedTimeEffect(skill, player, tempDuration, flatDecrease, projVMultiDecrease));
                        }

                    }
                }
            }
        }
    }

    public class AcceleratedTimeEffect extends WalkSpeedIncreaseEffect {
        final double projVMulti;

        AcceleratedTimeEffect(Skill skill, Player applier, int duration, double speedIncrease, double projVMulti) {
            super(skill, accelerateEffectName, applier, duration, speedIncrease, null, null);
            this.projVMulti = projVMulti;

            types.add(EffectType.BENEFICIAL);
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
            addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) (getDuration() / 50), 2));
            super.applyToMonster(monster);
        }
    }

    public class DeceleratedTimeEffect extends WalkSpeedDecreaseEffect {
        final double projVMulti;

        DeceleratedTimeEffect(Skill skill, Player applier, int duration, double flatDecrease, double projVMulti) {
            super(skill, decelerateEffectName, applier, duration, flatDecrease, null, null);
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
            addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (getDuration() / 50), 2));
            super.applyToMonster(monster);
        }
    }
}
