package com.herocraftonline.heroes.characters.skill.reborn.pyromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.BurningEffect;
import com.herocraftonline.heroes.characters.effects.common.CombustEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.tools.BasicDamageMissile;
import com.herocraftonline.heroes.characters.skill.tools.BasicMissile;
import com.herocraftonline.heroes.util.GeometryUtil;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.Effect;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.EffectType;
import de.slikey.effectlib.util.RandomUtils;
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

    public SkillGreatFireball(Heroes plugin) {
        super(plugin, "GreatFireball");
        setDescription("Conjure up a massive orb of pure fire. Direct hits deal $1 damage and will ignite the target, dealing $2 burning damage over the next $3 second(s). " +
                "The explosion will do an additional $4 damage to all enemies within $5 blocks and set the ground on fire.");
        setUsage("/skill greatfireball");
        setIdentifiers("skill greatfireball");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_FIRE, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);

        int burnDuration = SkillConfigManager.getUseSetting(hero, this, "burn-duration", 2000, false);
        double burnMultipliaer = SkillConfigManager.getUseSetting(hero, this, "burn-damage-multiplier", 2.0, false);
        double totalBurnDamage = plugin.getDamageManager().calculateFireTickDamage((int) (burnDuration / 50), burnMultipliaer);

        double explosionDamage = SkillConfigManager.getUseSetting(hero, this, "explosion-damage", 25.0, false);
        double explosionRadius = SkillConfigManager.getUseSetting(hero, this, "explosion-radius", 4.0, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(damage))
                .replace("$2", Util.decFormat.format(totalBurnDamage))
                .replace("$3", Util.decFormat.format(burnDuration / 1000.0))
                .replace("$4", Util.decFormat.format(explosionDamage))
                .replace("$5", Util.decFormat.format(explosionRadius));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 45.0);
        config.set("explosion-damage", 25.0);
        config.set("explosion-radius", 4.0);
        config.set("burn-duration", 3000);
        config.set("burn-damage-multiplier", 2.0);
        config.set("fire-tick-ground-radius", 2.5);
        config.set(BasicMissile.PROJECTILE_SIZE_NODE, 0.65);
        config.set(BasicMissile.PROJECTILE_PIERCES_ON_HIT_NODE, false);
        config.set(BasicMissile.PROJECTILE_VELOCITY_NODE, 35.0);
        config.set(BasicMissile.PROJECTILE_GRAVITY_NODE, 22.05675);
        config.set(BasicMissile.PROJECTILE_DURATION_TICKS_NODE, 30);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        GreatFireballMissile missile = new GreatFireballMissile(plugin, this, hero);
        missile.fireMissile();

        return SkillResult.NORMAL;
    }

    class GreatFireballMissile extends BasicDamageMissile {
        private final int burnDuration;
        private final double burnMultipliaer;
        private double explosionDamage;
        private double explosionRadius;
        private final double fireTickGroundRadius;
        private boolean explodedAlready = false;

        GreatFireballMissile(Heroes plugin, Skill skill, Hero hero) {
            super(plugin, skill, hero);

            this.burnDuration = SkillConfigManager.getUseSetting(hero, skill, "burn-duration", 3000, false);
            this.burnMultipliaer = SkillConfigManager.getUseSetting(hero, skill, "burn-damage-multiplier", 2.0, false);
            this.explosionDamage = SkillConfigManager.getUseSetting(hero, skill, "explosion-damage", 25.0, false);
            this.explosionRadius = SkillConfigManager.getUseSetting(hero, skill, "explosion-radius", 4.0, false);
            this.fireTickGroundRadius = SkillConfigManager.getUseSetting(hero, skill, "fire-tick-ground-radius", 2.5, false);
            this.visualEffect = new GreatFireballVisualEffect(this.effectManager, getEntityDetectRadius());
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
        protected void onBlockHit(Block block, Vector hitPoint, BlockFace hitFace, Vector hitForce) {
            performExplosion();
        }

        @Override
        protected void onValidTargetFound(LivingEntity target, Vector origin, Vector force) {
            performExplosion();

            if (!damageCheck(player, target))
                return;

            addSpellTarget(target, hero);
            damageEntity(target, player, this.damage, EntityDamageEvent.DamageCause.MAGIC, knockBackOnHit);

            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
            targetCT.addEffect(new BurningEffect(this.skill, player, burnDuration, burnMultipliaer));
        }

        private void performExplosion() {
            if (explodedAlready)
                return;

            explodedAlready = true;

            for (Location loc : GeometryUtil.getPerfectCircle(getLocation(), (int) this.fireTickGroundRadius, (int) this.fireTickGroundRadius, false, true, 0)) {
                plugin.getFireBlockManager().setBlockOnFireIfAble(loc.getBlock(), 0.7);
            }

            getWorld().playSound(getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 0.8F);

            Collection<Entity> nearbyEnts = getWorld().getNearbyEntities(getLocation(), this.explosionRadius, this.explosionRadius, this.explosionRadius);
            for (Entity ent : nearbyEnts) {
                if (!(ent instanceof LivingEntity))
                    continue;

                LivingEntity target = (LivingEntity) ent;
                if (!damageCheck(player, target))
                    continue;

                addSpellTarget(target, hero);
                damageEntity(target, player, this.explosionDamage, EntityDamageEvent.DamageCause.MAGIC);

                CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
                targetCT.addEffect(new CombustEffect(skill, player));
            }
        }
    }

    class GreatFireballVisualEffect extends Effect {
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

        GreatFireballVisualEffect(EffectManager effectManager, double radius) {
            this(effectManager, radius, 0);
        }

        GreatFireballVisualEffect(EffectManager effectManager, double radius, double decreasePerTick) {
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

            for (int i = 0; i < particleCount; ++i) {
                Vector vector = RandomUtils.getRandomVector().multiply(radiusToUse);
                location.add(vector);
                this.display(particle, location, color);
                location.subtract(vector);
            }
        }
    }
}