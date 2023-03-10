package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.tools.BasicMissile;
import com.herocraftonline.heroes.libs.slikey.effectlib.EffectType;
import com.herocraftonline.heroes.libs.slikey.effectlib.effect.LineEffect;
import com.herocraftonline.heroes.libs.slikey.effectlib.effect.SphereEffect;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

public class SkillAetherOrb extends ActiveSkill {

    private static final Color blueViolet = Color.fromRGB(138, 43, 226);
    private static final String effectName = "ActiveAetherOrb";

    private final NMSHandler nmsHandler = NMSHandler.getInterface();

    public SkillAetherOrb(final Heroes plugin) {
        super(plugin, "AetherOrb");
        setDescription("Call upon the Aether to launch it in front of you. " +
                "Upon landing an Orb is spawned at the location pulsing $1 damage on up to $4 targets in a $5 block radius every $2 second(s) for $3 seconds.");
        setUsage("/skill aetherorb");
        setIdentifiers("skill aetherorb");
        setArgumentRange(0, 0);
        setTypes(SkillType.AREA_OF_EFFECT, SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_MAGICAL);
    }

    @Override
    public String getDescription(final Hero hero) {
        final double radius = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.RADIUS, false);
        final long duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        final long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 200, false);
        final double damageTick = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE_TICK, false);
        final int maxTargets = SkillConfigManager.getUseSetting(hero, this, "max-targets-per-pulse", 4, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(damageTick))
                .replace("$2", Util.decFormat.format((double) period / 1000))
                .replace("$3", Util.decFormat.format((double) duration / 1000))
                .replace("$4", maxTargets + "")
                .replace("$5", Util.decFormat.format(radius));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.RADIUS.node(), 5.0);
        config.set("height", 4.0);
        config.set(SkillSetting.PERIOD.node(), 500);
        config.set(SkillSetting.DURATION.node(), 6000);
        config.set(SkillSetting.DAMAGE_TICK.node(), 50d);
        config.set(BasicMissile.PROJECTILE_VELOCITY_NODE, 20.0);
        config.set(BasicMissile.PROJECTILE_PIERCES_ON_HIT_NODE, true);
        config.set(BasicMissile.PROJECTILE_SIZE_NODE, 0.2);
        config.set(BasicMissile.PROJECTILE_GRAVITY_NODE, 12.25375);
        config.set(BasicMissile.PROJECTILE_DURATION_TICKS_NODE, 999999);
        config.set("max-targets-per-pulse", 4);
        config.set("visual-orb-radius", 1.0);
        return config;
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        if (hero.hasEffect(effectName)) {
            hero.removeEffect(hero.getEffect(effectName));
        }

        broadcastExecuteText(hero);
        final AetherOrbMissile missile = new AetherOrbMissile(plugin, this, hero);
        missile.fireMissile();

        return SkillResult.NORMAL;
    }

    private class AetherOrbMissile extends BasicMissile {
        public AetherOrbMissile(final Heroes plugin, final Skill skill, final Hero hero) {
            super(plugin, skill, hero, Particle.SPELL_WITCH);
        }

        @Override
        protected void onBlockHit(final Block block, final Vector hitPoint, final BlockFace hitFace, final Vector hitForce) {
            explodeIntoGroundEffect(block.getRelative(hitFace).getLocation());
        }

        @Override
        protected void onValidTargetFound(final LivingEntity target, final Vector origin, final Vector passForce) {
            explodeIntoGroundEffect(target.getLocation());
        }

        private void explodeIntoGroundEffect(final Location location) {
            if (hero.hasEffect(effectName)) {
                return;
            }

            final long period = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.PERIOD, 500, false);
            final long duration = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DURATION, 6000, false);

            final AetherOrbEffect orbEffect = new AetherOrbEffect(skill, player, hero, period, duration, location.clone());
            hero.addEffect(orbEffect);
        }
    }

    private class AetherOrbEffect extends PeriodicExpirableEffect {
        private final Location orbLocation;
        private final double height;
        private double orbVisualRadius;
        private double damageTick;
        private int maxTargetsPerPulse;
        private double radius;

        AetherOrbEffect(final Skill skill, final Player player, final Hero hero, final long period, final long duration, final Location location) {
            super(skill, effectName, player, period, duration, null, null);

            this.height = SkillConfigManager.getUseSetting(hero, skill, "height", 4.0, false);
            this.orbLocation = location.add(new Vector(0, height / 2.0, 0));
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);

            this.radius = SkillConfigManager.getScaledUseSettingDouble(hero, skill, SkillSetting.RADIUS, false);
            this.damageTick = SkillConfigManager.getScaledUseSettingDouble(hero, skill, SkillSetting.DAMAGE_TICK, false);
            this.maxTargetsPerPulse = SkillConfigManager.getUseSetting(hero, skill, "max-targets-per-pulse", 4, false);
            this.orbVisualRadius = SkillConfigManager.getUseSetting(hero, skill, "visual-orb-radius", 1.0, false);

            final SphereEffect orbVisual = new SphereEffect(effectLib);
            orbVisual.setLocation(orbLocation);
            orbVisual.particle = Particle.SPELL_WITCH;
            orbVisual.particles = 25;
            orbVisual.radius = orbVisualRadius;
            orbVisual.type = EffectType.REPEATING;
            orbVisual.period = 1;
            orbVisual.iterations = (int) (getDuration() / 50);
            orbVisual.asynchronous = true;

            effectLib.start(orbVisual);
            orbLocation.getWorld().playSound(orbLocation, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.15f, 0.0001f);
        }

        @Override
        public void tickMonster(final Monster monster) {
        }

        @Override
        public void tickHero(final Hero hero) {
            int currentHitCount = 0;
            for (final Entity entity : orbLocation.getWorld().getNearbyEntities(orbLocation, radius, height, radius)) {
                if (currentHitCount >= maxTargetsPerPulse) {
                    break;
                }
                if (!(entity instanceof LivingEntity)) {
                    continue;
                }
                final LivingEntity target = (LivingEntity) entity;
                if (!damageCheck(applier, target)) {
                    continue;
                }

                currentHitCount++;

                addSpellTarget(target, hero);
                damageEntity(target, applier, damageTick, EntityDamageEvent.DamageCause.MAGIC, 0.0f);

                final LineEffect lineVisual = new LineEffect(effectLib);
                lineVisual.particle = Particle.REDSTONE;
                lineVisual.color = blueViolet;
                lineVisual.setLocation(orbLocation);
                lineVisual.setTargetEntity(target);

                effectLib.start(lineVisual);
                orbLocation.getWorld().playSound(orbLocation, Sound.BLOCK_GLASS_BREAK, 0.3f, 2.0F);
            }
        }
    }
}
