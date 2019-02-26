package com.herocraftonline.heroes.characters.skill.reborn.pyromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.tools.Missile;
import de.slikey.effectlib.Effect;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.EffectType;
import de.slikey.effectlib.util.RandomUtils;
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

    public SkillMagmaOrb(Heroes plugin) {
        super(plugin, "MagmaOrb");
        setDescription("Conjure up a projectile of pure fire. The orb seeks out nearby entities and lasts for $1 seconds. "
                + "Targets hit by the magmaOrb are launched upwards and dealt $2 damage");
        setUsage("/skill magmaorb");
        setArgumentRange(0, 0);
        setIdentifiers("skill magmaorb");
        setTypes(SkillType.ABILITY_PROPERTY_FIRE, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 80, false);
        return getDescription().replace("$1", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 75);
        config.set(SkillSetting.RADIUS.node(), 4.0);
        config.set(SkillSetting.DELAY.node(), 1000);
        config.set("projectile-radius", 2.0);
        config.set("projectile-velocity", 12.0);
        config.set("projectile-velocity", 12.0);
        return config;
    }

    @Override
    public void onWarmup(Hero hero) {
        super.onWarmup(hero);

        int warmupTime = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DELAY, 1, false);
        int warmupTicks = warmupTime / 50;

        double radius = SkillConfigManager.getUseSetting(hero, this, "projectile-radius", 4.0, false) * 2;  // Double it for the warmup visual
        double decreasePerTick = radius / warmupTicks;

        final EffectManager effectManager = new EffectManager(plugin);
        MagmaOrbVisualEffect vEffect = new MagmaOrbVisualEffect(effectManager, radius, decreasePerTick);
        vEffect.setLocation(hero.getPlayer().getLocation());
        effectManager.start(vEffect);
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        MagmaOrbMissile missile = new MagmaOrbMissile(hero, this);
        missile.fireMissile();

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    class MagmaOrbMissile extends Missile {

        private final Hero hero;
        private final Player player;
        private final double damage;
        private final double damageRadius;

        final EffectManager effectManager = new EffectManager(plugin);
        final MagmaOrbVisualEffect vEffect;

        MagmaOrbMissile(Hero hero, Skill skill) {
            this.hero = hero;
            this.player = hero.getPlayer();
            this.damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 75.0, false);
            this.damageRadius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS, 6.0, false);

            double projRadius = SkillConfigManager.getUseSetting(hero, skill, "projectile-radius", 2.0, false);

            setGravity(SkillConfigManager.getUseSetting(hero, skill, "projectile-gravity", 14.7045, false));
            setDrag(SkillConfigManager.getUseSetting(hero, skill, "projectile-drag", 0, false));
            setMass(SkillConfigManager.getUseSetting(hero, skill, "projectile-mass", 5, false));
            setEntityDetectRadius(projRadius);

            Location playerLoc = player.getEyeLocation();
            Vector direction = playerLoc.getDirection().normalize();
            Vector offset = direction.multiply(1.5);
            Location missileLoc = playerLoc.add(offset);
            setLocationAndSpeed(missileLoc, SkillConfigManager.getUseSetting(hero, skill, "projectile-velocity", 12.0, false));

            vEffect = new MagmaOrbVisualEffect(effectManager, projRadius, 0);
        }

        protected void onStart() {
            vEffect.setLocation(getLocation());
            effectManager.start(vEffect);
        }

        protected void onTick() {
            vEffect.setLocation(getLocation());
        }

        protected void onFinalTick() {
            effectManager.dispose();
        }

        protected boolean onCollideWithEntity(Entity entity) {
            if (!(entity instanceof LivingEntity) || entity.equals(player) || !damageCheck(player, (LivingEntity) entity))
                return false;
            return true;
        }

        @Override
        protected void onBlockHit(Block block, Vector hitPoint, BlockFace hitFace, Vector hitForce) {
            applyDamageAoE();
        }

        @Override
        protected void onEntityHit(Entity entity, Vector hitOrigin, Vector hitForce) {
            applyDamageAoE();
        }

        private void applyDamageAoE() {
            Collection<Entity> nearbyEnts = getWorld().getNearbyEntities(getLocation(), this.damageRadius, this.damageRadius, this.damageRadius);
            for (Entity ent : nearbyEnts) {
                if (!(ent instanceof LivingEntity))
                    continue;
                LivingEntity target = (LivingEntity) ent;
                if (!damageCheck(player, target))
                    continue;

                addSpellTarget(target, hero);
                damageEntity(target, player, this.damage, EntityDamageEvent.DamageCause.FIRE);
            }
        }
    }

    class MagmaOrbVisualEffect extends Effect {
        private Particle primaryParticle;
        private Color primaryColor;
        private double primaryRadius;
        private double primaryYOffset;
        private int primaryParticleCount;
        private double primaryRadiusDecrease;

        private Particle secondaryParticle;
        private Color secondaryColor;
        private double secondaryRadius;
        private double secondaryYOffset;
        private int secondaryParticleCount;
        private double secondaryRadiusDecrease;

        MagmaOrbVisualEffect(EffectManager effectManager, double radius, double decreasePerTick) {
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

            this.secondaryParticle = Particle.DRIP_LAVA;
            this.secondaryColor = FIRE_ORANGE;
            this.secondaryRadius = secondaryRadiusMultiplier(radius);
            this.secondaryRadiusDecrease = secondaryRadiusMultiplier(decreasePerTick) / this.period;
            this.secondaryYOffset = 0.0D;
            this.secondaryParticleCount = 75;
        }

        public void onRun() {
            if (primaryRadiusDecrease != 0.0D)
                this.primaryRadius -= primaryRadiusDecrease;
            if (primaryRadius > 0)
                displaySphere(this.primaryRadius, this.primaryYOffset, this.primaryParticle, this.primaryColor, this.primaryParticleCount);

            displayCenter();

            if (secondaryRadiusDecrease != 0.0D)
                this.secondaryRadius -= secondaryRadiusDecrease;
            if (secondaryRadius > 0)
                displaySphere(this.secondaryRadius, this.secondaryYOffset, this.secondaryParticle, this.secondaryColor, this.secondaryParticleCount);
        }

        private double secondaryRadiusMultiplier(double radiusValue) {
            return radiusValue * 1.2;
        }

        private void displayCenter() {
            Location location = this.getLocation();
            Vector vector = new Vector(0.0D, primaryYOffset, 0.0D);
            location.add(vector);
            this.display(Particle.LAVA, location);
            location.subtract(vector);
        }

        private void displaySphere(double radiusToUse, double yOffset, Particle particle, Color color, int particleCount) {
            Location location = this.getLocation();
            location.add(0.0D, yOffset, 0.0D);

            for(int i = 0; i < particleCount; ++i) {
                Vector vector = RandomUtils.getRandomVector().multiply(radiusToUse);
                location.add(vector);
                this.display(particle, location, color);
                location.subtract(vector);
            }
        }
    }
}