package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.BurningEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.tools.BasicMissile;
import com.herocraftonline.heroes.libs.slikey.effectlib.Effect;
import com.herocraftonline.heroes.libs.slikey.effectlib.EffectManager;
import com.herocraftonline.heroes.libs.slikey.effectlib.EffectType;
import com.herocraftonline.heroes.libs.slikey.effectlib.util.RandomUtils;
import com.herocraftonline.heroes.util.GeometryUtil;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.Collection;

public class SkillGreatFireball extends ActiveSkill {

    private static final Color FIRE_ORANGE = Color.fromRGB(226, 88, 34);
    private static final Color FIRE_RED = Color.fromRGB(236, 60, 30);

    public SkillGreatFireball(final Heroes plugin) {
        super(plugin, "GreatFireball");
        setDescription("Conjure up a massive orb of pure fire. The orb deals $1 damage to any target hit and ");
        setUsage("/skill greatfireball");
        setIdentifiers("skill greatfireball");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_FIRE, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(final Hero hero) {
        final double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 80.0, false);
        return getDescription()
                .replace("$1", Util.decFormat.format(damage));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 45.0);
        config.set("explosion-damage", 25.0);
        config.set("explosion-radius", 4.0);
        config.set("burn-duration", 3000);
        config.set("burn-damage-multiplier", 2.0);
        config.set("fire-tick-ground-radius", 2.5);
        config.set("projectile-size", 0.65);
        config.set("projectile-velocity", 35.0);
        config.set("projectile-gravity", 22.05675);
        config.set("projectile-max-ticks-lived", 30);
        return config;
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        final double projSize = SkillConfigManager.getUseSetting(hero, this, "projectile-size", 0.5, false);
        final double projVelocity = SkillConfigManager.getUseSetting(hero, this, "projectile-velocity", 20, false);
        final GreatFireballMissile missile = new GreatFireballMissile(plugin, this, hero, projSize, projVelocity);
        missile.fireMissile();

        return SkillResult.NORMAL;
    }

    static class GreatFireballVisualEffect extends Effect {
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

        GreatFireballVisualEffect(final EffectManager effectManager, final double radius, final double decreasePerTick) {
            super(effectManager);

            this.period = 1;
            this.iterations = 500;
            this.type = EffectType.REPEATING;

            this.primaryParticle = Particle.REDSTONE;
            this.primaryColor = FIRE_ORANGE;
            this.primaryRadius = radius;
            this.primaryRadiusDecrease = decreasePerTick / this.period;
            this.primaryYOffset = 0.0D;
            this.primaryParticleCount = 10;

            this.secondaryParticle = Particle.SPELL_MOB;
            this.secondaryColor = FIRE_ORANGE;
            this.secondaryRadius = secondaryRadiusMultiplier(radius);
            this.secondaryRadiusDecrease = secondaryRadiusMultiplier(decreasePerTick) / this.period;
            this.secondaryYOffset = 0.0D;
            this.secondaryParticleCount = 20;
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

    class GreatFireballMissile extends BasicMissile {
        private final int burnDuration;
        private final double burnMultipliaer;
        private final double damage;
        private final double explosionDamage;
        private final double explosionRadius;
        private final double fireTickGroundRadius;

        GreatFireballMissile(final Plugin plugin, final Skill skill, final Hero hero, final double projectileSize, final double projVelocity) {
            super((Heroes) plugin, skill, hero, Particle.FLAME, Color.RED, true);

            setRemainingLife(SkillConfigManager.getUseSetting(hero, skill, "projectile-max-ticks-lived", 20, false));
            setGravity(SkillConfigManager.getUseSetting(hero, skill, "projectile-gravity", 5.0, false));

            this.damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 45.0, false);
            this.burnDuration = SkillConfigManager.getUseSetting(hero, skill, "burn-duration", 3000, false);
            this.burnMultipliaer = SkillConfigManager.getUseSetting(hero, skill, "burn-damage-multiplier", 2.0, false);
            this.explosionDamage = SkillConfigManager.getUseSetting(hero, skill, "explosion-damage", 25.0, false);
            this.explosionRadius = SkillConfigManager.getUseSetting(hero, skill, "explosion-radius", 4.0, false);
            this.fireTickGroundRadius = SkillConfigManager.getUseSetting(hero, skill, "fire-tick-ground-radius", 2.5, false);
            this.visualEffect = new GreatFireballVisualEffect(effectLib, projectileSize, 0);
        }

        @Override
        protected void onTick() {
            if (this.getTicksLived() % 2 == 0 && this.visualEffect != null) {
                this.visualEffect.setLocation(this.getLocation());
            }

            if (this.getTicksLived() % 4 == 0) {
                getWorld().playSound(getLocation(), Sound.ENTITY_GHAST_SHOOT, 0.5F, 0.5F);
            }
        }

        @Override
        protected void onFinalTick() {

        }

        @Override
        protected boolean onCollideWithEntity(final Entity entity) {
            return entity instanceof LivingEntity && !hero.isAlliedTo((LivingEntity) entity);
        }

        @Override
        protected void onBlockHit(final Block block, final Vector hitPoint, final BlockFace hitFace, final Vector hitForce) {
            performExplosion();
        }

        @Override
        protected void onEntityHit(final Entity entity, final Vector hitOrigin, final Vector hitForce) {
            performExplosion();

            final LivingEntity target = (LivingEntity) entity;
            if (!damageCheck(player, target)) {
                return;
            }

            addSpellTarget(target, hero);
            damageEntity(target, player, this.damage, EntityDamageEvent.DamageCause.MAGIC);

            final CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
            targetCT.addEffect(new BurningEffect(this.skill, player, burnDuration, burnMultipliaer));
        }

        private void performExplosion() {
            for (final Location loc : GeometryUtil.getPerfectCircle(getLocation(), (int) this.fireTickGroundRadius, (int) this.fireTickGroundRadius, false, true, 0)) {
                Util.setBlockOnFireIfAble(loc.getBlock(), 0.7);
            }

            getWorld().playSound(getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 0.8F);

            final Collection<Entity> nearbyEnts = getWorld().getNearbyEntities(getLocation(), this.explosionRadius, this.explosionRadius, this.explosionRadius);
            for (final Entity ent : nearbyEnts) {
                if (!(ent instanceof LivingEntity)) {
                    continue;
                }

                final LivingEntity target = (LivingEntity) ent;
                if (!damageCheck(player, target)) {
                    continue;
                }

                addSpellTarget(target, hero);
                damageEntity(target, player, this.explosionDamage, EntityDamageEvent.DamageCause.MAGIC);
            }
        }
    }
}