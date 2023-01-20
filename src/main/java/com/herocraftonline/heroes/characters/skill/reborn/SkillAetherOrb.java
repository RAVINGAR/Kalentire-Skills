package com.herocraftonline.heroes.characters.skill.reborn;

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
import com.herocraftonline.heroes.characters.skill.tools.BasicDamageMissile;
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
    private final NMSHandler nmsHandler = NMSHandler.getInterface();

    public SkillAetherOrb(final Heroes plugin) {
        super(plugin, "AetherOrb");
        setDescription("Call upon the forces of the aether and launch it forward in front of you. "
                + "Upon landing, an orb of Aether is formed at the location, dealing $1 damage every $2 second(s) for up to $3 second(s). "
                + "The orb can only hit up to $4 targets within $5 blocks every time it pulses.");
        setUsage("/skill aetherorb");
        setIdentifiers("skill aetherorb");
        setArgumentRange(0, 0);
        setTypes(SkillType.AREA_OF_EFFECT, SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_MAGICAL);

        setToggleableEffectName(this.getName());
    }

    @Override
    public String getDescription(final Hero hero) {
        final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 4.0, false);
        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);
        final long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 200, false);
        final double damageTick = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 50d, false);
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
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.RADIUS.node(), 5.0);
        node.set("height", 4.0);
        node.set(SkillSetting.PERIOD.node(), 500);
        node.set(SkillSetting.DURATION.node(), 6000);
        node.set(SkillSetting.DAMAGE_TICK.node(), 50d);
        node.set("max-targets-per-pulse", 4);
        node.set("projectile-velocity", 20.0);
        node.set("visual-orb-radius", 1.0);
        return node;
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero);
        final double projVelocity = SkillConfigManager.getUseSetting(hero, this, "projectile-velocity", 30.0, false);
        final AetherOrbMissile missile = new AetherOrbMissile(plugin, this, hero, 0.2, projVelocity);
        missile.fireMissile();

        return SkillResult.NORMAL;
    }

    private class AetherOrbMissile extends BasicDamageMissile {

        public AetherOrbMissile(final Heroes plugin, final Skill skill, final Hero hero, final double projectileSize, final double projVelocity) {
            super(plugin, skill, hero, Particle.SPELL_WITCH, Color.AQUA, EntityDamageEvent.DamageCause.MAGIC);
        }

        @Override
        protected boolean onCollideWithEntity(final Entity entity) {
            return false;
        }

        @Override
        protected void onBlockHit(final Block block, final Vector hitPoint, final BlockFace hitFace, final Vector hitForce) {
            explodeIntoGroundEffect(block.getRelative(hitFace).getLocation());
        }

        private void explodeIntoGroundEffect(final Location location) {
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
            super(skill, "ActiveAetherOrb", player, period, duration, null, null);

            this.height = SkillConfigManager.getUseSetting(hero, skill, "height", 4.0, false);
            this.orbLocation = location.add(new Vector(0, height / 2.0, 0));
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            this.radius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS, 6.0, false);
            this.damageTick = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_TICK, 50d, false);
            this.maxTargetsPerPulse = SkillConfigManager.getUseSetting(hero, skill, "max-targets-per-pulse", 4, false);
            this.orbVisualRadius = SkillConfigManager.getUseSetting(hero, skill, "visual-orb-radius", 1.0, false);

            final SphereEffect orbVisual = new SphereEffect(effectLib);
            orbVisual.setLocation(orbLocation);
            orbVisual.particle = Particle.SPELL_WITCH;
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
                damageEntity(target, applier, damageTick, EntityDamageEvent.DamageCause.MAGIC, false);

                final LineEffect lineVisual = new LineEffect(effectLib);
                lineVisual.particle = Particle.REDSTONE;
                lineVisual.color = blueViolet;
                lineVisual.setLocation(orbLocation);
                lineVisual.setTargetEntity(target);

                effectLib.start(lineVisual);
                orbLocation.getWorld().playSound(orbLocation, Sound.BLOCK_GLASS_BREAK, 0.3f, 2.0F);
            }
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
        }
    }
}
