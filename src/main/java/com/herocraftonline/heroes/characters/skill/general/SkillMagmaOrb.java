package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.tools.Missile;
import com.herocraftonline.heroes.libs.slikey.effectlib.Effect;
import com.herocraftonline.heroes.libs.slikey.effectlib.EffectManager;
import com.herocraftonline.heroes.libs.slikey.effectlib.EffectType;
import com.herocraftonline.heroes.libs.slikey.effectlib.util.RandomUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.Collection;

public class SkillMagmaOrb extends ActiveSkill {

    private static final Color FIRE_ORANGE = Color.fromRGB(226, 88, 34);
    private static final Color FIRE_RED = Color.fromRGB(236, 60, 30);

    public SkillMagmaOrb(final Heroes plugin) {
        super(plugin, "MagmaOrb");
        setDescription("Conjure up a projectile of pure fire. The orb seeks out nearby entities and lasts for $1 second(s). "
                + "Targets hit by the magmaOrb are launched upwards and dealt $2 damage");
        setUsage("/skill magmaorb");
        setArgumentRange(0, 0);
        setIdentifiers("skill magmaorb");
        setTypes(SkillType.ABILITY_PROPERTY_FIRE, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(final Hero hero) {
        final int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 80, false);
        return getDescription().replace("$1", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 75);
        config.set(SkillSetting.RADIUS.node(), 4.0);
        config.set(SkillSetting.DELAY.node(), 1000);
        config.set("projectile-radius", 2.0);
        config.set("projectile-velocity", 12.0);
        config.set("projectile-velocity", 12.0);
        return config;
    }

    @Override
    public void onWarmup(final Hero hero) {
        super.onWarmup(hero);

        final int warmupTime = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DELAY, 1, false);
        final int warmupTicks = warmupTime / 50;

        final double radius = SkillConfigManager.getUseSetting(hero, this, "projectile-radius", 4.0, false) * 2;  // Double it for the warmup visual
        final double decreasePerTick = radius / warmupTicks;

        final MagmaOrbVisualEffect vEffect = new MagmaOrbVisualEffect(effectLib, radius, decreasePerTick);
        vEffect.setLocation(hero.getPlayer().getLocation());
        effectLib.start(vEffect);
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        final MagmaOrbMissile missile = new MagmaOrbMissile(hero, this);
        missile.fireMissile();

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    static class MagmaOrbVisualEffect extends Effect {
        private final Particle primaryParticle;
        private final Color primaryColor;
        private final double primaryYOffset;
        private final int primaryParticleCount;
        private final double primaryRadiusDecrease;
        private final Particle secondaryParticle;
        private final Color secondaryColor;
        private final double secondaryYOffset;
        private final int secondaryParticleCount;
        private final double secondaryRadiusDecrease;
        private double primaryRadius;
        private double secondaryRadius;

        MagmaOrbVisualEffect(final EffectManager effectManager, final double radius, final double decreasePerTick) {
            super(effectManager);

            this.period = 1;
            this.iterations = 500;
            this.type = EffectType.REPEATING;

            this.primaryParticle = Particle.REDSTONE;
            this.primaryColor = FIRE_ORANGE;
            this.primaryRadius = radius;
            this.primaryRadiusDecrease = decreasePerTick / this.period;
            this.primaryYOffset = 0.0D;
            this.primaryParticleCount = 25;

            this.secondaryParticle = Particle.SPELL_MOB;
            this.secondaryColor = FIRE_ORANGE;
            this.secondaryRadius = secondaryRadiusMultiplier(radius);
            this.secondaryRadiusDecrease = secondaryRadiusMultiplier(decreasePerTick) / this.period;
            this.secondaryYOffset = 0.0D;
            this.secondaryParticleCount = 75;
        }

        @Override
        public void onRun() {
            if (primaryRadiusDecrease != 0.0D) {
                this.primaryRadius -= primaryRadiusDecrease;
            }
            if (primaryRadius > 0) {
                displaySphere(this.primaryRadius, this.primaryYOffset, this.primaryParticle, this.primaryColor, this.primaryParticleCount);
            }

            displayCenter();

            if (secondaryRadiusDecrease != 0.0D) {
                this.secondaryRadius -= secondaryRadiusDecrease;
            }
            if (secondaryRadius > 0) {
                displaySphere(this.secondaryRadius, this.secondaryYOffset, this.secondaryParticle, this.secondaryColor, this.secondaryParticleCount);
            }
        }

        private double secondaryRadiusMultiplier(final double radiusValue) {
            return radiusValue * 1.2;
        }

        private void displayCenter() {
            final Location location = this.getLocation();
            final Vector vector = new Vector(0.0D, primaryYOffset, 0.0D);
            location.add(vector);
            this.display(Particle.LAVA, location);
            location.subtract(vector);
        }

        private void displaySphere(final double radiusToUse, final double yOffset, final Particle particle, final Color color, final int particleCount) {
            final Location location = this.getLocation();
            location.add(0.0D, yOffset, 0.0D);

            for (int i = 0; i < particleCount; ++i) {
                final Vector vector = RandomUtils.getRandomVector().multiply(radiusToUse);
                location.add(vector);
                this.display(particle, location, color);
                location.subtract(vector);
            }
        }
    }

    class MagmaOrbMissile extends Missile {

        final MagmaOrbVisualEffect vEffect;
        private final Hero hero;
        private final Player player;
        private final double damage;
        private final double damageRadius;

        MagmaOrbMissile(final Hero hero, final Skill skill) {
            this.hero = hero;
            this.player = hero.getPlayer();
            this.damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 75.0, false);
            this.damageRadius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS, 6.0, false);

            final double projRadius = SkillConfigManager.getUseSetting(hero, skill, "projectile-radius", 2.0, false);

            setGravity(SkillConfigManager.getUseSetting(hero, skill, "projectile-gravity", 14.7045, false));
            setDrag(SkillConfigManager.getUseSetting(hero, skill, "projectile-drag", 0, false));
            setMass(SkillConfigManager.getUseSetting(hero, skill, "projectile-mass", 5, false));
            setEntityDetectRadius(projRadius);

            final Location playerLoc = player.getEyeLocation();
            final Vector direction = playerLoc.getDirection().normalize();
            final Vector offset = direction.multiply(1.5);
            final Location missileLoc = playerLoc.add(offset);
            setLocationAndSpeed(missileLoc, SkillConfigManager.getUseSetting(hero, skill, "projectile-velocity", 12.0, false));

            vEffect = new MagmaOrbVisualEffect(effectLib, projRadius, 0);
        }

        @Override
        protected void onStart() {
            vEffect.setLocation(getLocation());
            effectLib.start(vEffect);
        }

        @Override
        protected void onTick() {
            vEffect.setLocation(getLocation());
        }

        @Override
        protected void onFinalTick() {
            vEffect.cancel();
        }

        @Override
        protected boolean onCollideWithEntity(final Entity entity) {
            return entity instanceof LivingEntity && !entity.equals(player) && damageCheck(player, (LivingEntity) entity);
        }

        @Override
        protected void onBlockHit(final Block block, final Vector hitPoint, final BlockFace hitFace, final Vector hitForce) {
            applyDamageAoE();
        }

        @Override
        protected void onEntityHit(final Entity entity, final Vector hitOrigin, final Vector hitForce) {
            applyDamageAoE();
        }

        private void applyDamageAoE() {
            final Collection<Entity> nearbyEnts = getWorld().getNearbyEntities(getLocation(), this.damageRadius, this.damageRadius, this.damageRadius);
            for (final Entity ent : nearbyEnts) {
                if (!(ent instanceof LivingEntity)) {
                    continue;
                }

                final LivingEntity target = (LivingEntity) ent;
                if (!damageCheck(player, target)) {
                    continue;
                }

                addSpellTarget(target, hero);
                damageEntity(target, player, this.damage, EntityDamageEvent.DamageCause.MAGIC);
            }
        }
    }
}