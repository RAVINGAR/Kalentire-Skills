package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.BurningEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.tools.Missile;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.libs.slikey.effectlib.effect.TornadoEffect;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class SkillFirenado extends ActiveSkill {

    private static final Color FIRE_ORANGE = Color.fromRGB(226, 88, 34);
    private static final Color FIRE_RED = Color.fromRGB(236, 60, 30);

    public SkillFirenado(final Heroes plugin) {
        super(plugin, "Firenado");
        setDescription("Conjure up a tornado of pure fire. The firenado roams around and seeks out nearby targets for the next $1 second(s). "
                + "Targets hit by the firenado are launched upwards, dealt $2 damage, and burned. "
                + "Burning targets take $3 burning damage over the next $4 second(s).");
        setUsage("/skill firenado");
        setArgumentRange(0, 0);
        setIdentifiers("skill firenado");
        setTypes(SkillType.ABILITY_PROPERTY_FIRE, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(final Hero hero) {
        final int tornadoDuration = SkillConfigManager.getUseSetting(hero, this, "tornado-duration", 8000, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 90, false);
        final double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        final int burnDuration = SkillConfigManager.getUseSetting(hero, this, "burn-duration", 3000, false);
        final double burnMultipliaer = SkillConfigManager.getUseSetting(hero, this, "burn-damage-multiplier", 2.0, false);
        final double totalBurnDamage = plugin.getDamageManager().calculateFireTickDamage((int) (burnDuration / 50), burnMultipliaer);

        final String formattedDuration = Util.decFormat.format(tornadoDuration / 1000);
        final String formattedDamage = Util.decFormat.format(damage);
        final String formattedBurnDamage = Util.decFormat.format(totalBurnDamage);
        final String formattedBurnDuration = Util.decFormat.format(burnDuration / 1000);
        return getDescription().replace("$1", formattedDuration).replace("$2", formattedDamage).replace("$3", formattedBurnDamage).replace("$4", formattedBurnDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 75);
        config.set("burn-duration", 4000);
        config.set("burn-damage-multiplier", 2.0);
        config.set("require-blaze-rod", false);
        config.set("hit-upwards-velocity", 0.8);
        config.set("tornado-velocity", 4.0);
        config.set("tornado-duration", 8000);
        config.set("tornado-max-heat-seeking-distance", 25);
        config.set("heat-seek-force-power", 0.2);
        config.set("tornado-visual-y-offset", 0.0);
        return config;
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        final boolean requireBlazeRod = SkillConfigManager.getUseSetting(hero, this, "require-blaze-rod", true);
        if (requireBlazeRod) {
            final PlayerInventory playerInv = player.getInventory();
            final ItemStack mainHand = NMSHandler.getInterface().getItemInMainHand(playerInv);
            final ItemStack offHand = NMSHandler.getInterface().getItemInOffHand(playerInv);
            if ((mainHand == null || mainHand.getType() != Material.BLAZE_ROD) && (offHand == null || offHand.getType() != Material.BLAZE_ROD)) {
                player.sendMessage("    " + ChatComponents.GENERIC_SKILL + "You are unable to cast this spell without holding a Blaze Rod as a catalyst!");
                return SkillResult.INVALID_TARGET_NO_MSG;
            }
        }

        broadcastExecuteText(hero);

        final FirenadoMissile missile = new FirenadoMissile(this, hero);
        missile.fireMissile();

        return SkillResult.NORMAL;
    }

    class FirenadoMissile extends Missile {

        final TornadoEffect vEffect = new TornadoEffect(effectLib);
        private final Skill skill;
        private final Hero hero;
        private final Player player;
        private final double defaultSpeed;
        private final int initialDurationTicks;
        private final double maxHeatSeekingDistance;
        private final double maxHeatSeekingDistanceSquared;
        private final int heatSeekingIntervalTicks;
        private final double heatSeekForcePower;
        private final double damage;
        private final int burnDuration;
        private final double burnMultipliaer;
        private final double hitUpwardsVelocity;
        private final Set<LivingEntity> hitTargets = new HashSet<>();
        private final Set<LivingEntity> tempIgnoreTargets = new HashSet<>();
        LivingEntity currentTarget = null;

        FirenadoMissile(final Skill skill, final Hero hero) {
            this.skill = skill;
            this.hero = hero;
            this.player = hero.getPlayer();

            this.damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 75.0, false);
            this.burnDuration = SkillConfigManager.getUseSetting(hero, skill, "burn-duration", 3000, false);
            this.burnMultipliaer = SkillConfigManager.getUseSetting(hero, skill, "burn-damage-multiplier", 2.0, false);
            this.hitUpwardsVelocity = SkillConfigManager.getUseSetting(hero, skill, "hit-upwards-velocity", 0.8, false);

            final Location playerLoc = player.getLocation();
            this.defaultSpeed = SkillConfigManager.getUseSetting(hero, skill, "tornado-velocity", 4.0, false);
            final Vector offset = playerLoc.getDirection().setY(0).normalize().multiply(3);
            final Location missileLoc = playerLoc.clone().add(offset);
            missileLoc.setPitch(0);
            setLocationAndSpeed(missileLoc, defaultSpeed);

            this.initialDurationTicks = SkillConfigManager.getUseSetting(hero, skill, "tornado-duration", 8000, false) / 50;
            this.maxHeatSeekingDistance = SkillConfigManager.getUseSetting(hero, skill, "tornado-max-heat-seeking-distance", 25.0, false);
            this.maxHeatSeekingDistanceSquared = maxHeatSeekingDistance * maxHeatSeekingDistance;
            this.heatSeekForcePower = SkillConfigManager.getUseSetting(hero, skill, "heat-seek-force-power", 0.5, false);
            this.heatSeekingIntervalTicks = (int) (this.initialDurationTicks * 0.15);

            final double radius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS, 4.0, false);

            setNoGravity();
            setEntityDetectRadius(radius);
            setRemainingLife(this.initialDurationTicks);

            vEffect.period = 5;
            vEffect.iterations = (this.initialDurationTicks) / vEffect.period;

            vEffect.yOffset = SkillConfigManager.getUseSetting(hero, skill, "tornado-visual-y-offset", 0.0, false);
            vEffect.showCloud = true;
            vEffect.showTornado = true;
            vEffect.tornadoColor = FIRE_RED;
            vEffect.tornadoParticle = Particle.SPELL_MOB;
            vEffect.distance = 0.375D * 3.0;
            vEffect.cloudParticle = Particle.CLOUD;
            vEffect.cloudColor = FIRE_ORANGE;
            vEffect.cloudSize = 0.25F;
            vEffect.tornadoHeight = (float) radius;
            vEffect.maxTornadoRadius = (float) radius * 0.5F;
            vEffect.asynchronous = true;
        }

        @Override
        protected void onStart() {
            vEffect.setLocation(getLocation());
            effectLib.start(vEffect);
        }

        @Override
        protected void onTick() {
            final Location location = getLocation();
            vEffect.setLocation(location);
            final Block block = location.getBlock();

            if (getTicksLived() % this.heatSeekingIntervalTicks != 0) {
                if (currentTarget != null) {
                    // Add force while heat seaking, but not every single tick.
                    addForce(getDirection().multiply(this.heatSeekForcePower));
                }
                return;
            }

            // We're at our desired tick interval.

            // Reach two blocks down for fire tick blocks
            Util.setBlockOnFireIfAble(block, 0.3, true);
            Util.setBlockOnFireIfAble(block.getRelative(BlockFace.DOWN), 0.3, true);

            flipColors();
            final LivingEntity target = getClosestEntity();
            if (target != null) {
                currentTarget = target;
                final Vector difference = target.getLocation().clone().subtract(location).toVector();
                setDirection(difference.normalize());
                setVelocity(difference.multiply(new Vector(0.5, 1, 0.5)));
            } else {
                currentTarget = null;
            }
        }

        private void flipColors() {
            if (vEffect.tornadoColor == FIRE_ORANGE) {
                vEffect.tornadoColor = FIRE_RED;
            } else {
                vEffect.tornadoColor = FIRE_ORANGE;
            }

            if (vEffect.cloudColor == FIRE_ORANGE) {
                vEffect.cloudColor = FIRE_RED;
            } else {
                vEffect.cloudColor = FIRE_ORANGE;
            }
        }

        private LivingEntity getClosestEntity() {
            double closestEntDistanceSquared = 99999999;
            LivingEntity closestEntity = null;

            final Collection<Entity> nearbyEnts = getWorld().getNearbyEntities(getLocation(), maxHeatSeekingDistance, maxHeatSeekingDistance * 0.5, maxHeatSeekingDistance);
            for (final Entity ent : nearbyEnts) {
                if (!(ent instanceof LivingEntity)) {
                    continue;
                }
                final LivingEntity lEnt = (LivingEntity) ent;
                if (hitTargets.contains(lEnt)) {
                    continue;
                }
                if (tempIgnoreTargets.contains(lEnt)) {
                    continue;
                }
                if (hero.isAlliedTo(lEnt)) {
                    hitTargets.add(lEnt);   // Fake a hit so we don't try this guy again.
                    continue;
                }
                if (!damageCheck(player, lEnt)) {
                    tempIgnoreTargets.add(lEnt);
                    continue;
                }

                final double distSquared = getLocation().distanceSquared(lEnt.getLocation());
                if (distSquared <= closestEntDistanceSquared) {
                    closestEntity = lEnt;
                    closestEntDistanceSquared = distSquared;
                }
            }
            return closestEntity;
        }

        @Override
        protected void onFinalTick() {
            vEffect.cancel();
        }

        @Override
        protected boolean onCollideWithBlock(final Block block, final Vector point, final BlockFace face) {
            final Location location = getLocation();

            // Make it "bounce" and go the other way.
            final Vector direction = getDirection();
            setLocation(location.clone().subtract(direction));
            setDirectionAndSpeed(direction.multiply(-1).setY(0.5), this.defaultSpeed);
//            if (currentTarget != null) {
//                tempIgnoreTargets.add(currentTarget);
//                currentTarget = null;
//            }
            return false;
        }

        // Don't ever "collide" with an entity
        @Override
        protected boolean onCollideWithEntity(final Entity entity) {
            return false;
        }

        // Hit around walls
        @Override
        protected boolean onBlockProtectsEntity(final Block block, final Entity entity, final Vector point, final BlockFace face) {
            return false;
        }

        // Don't do anything on block hit
        @Override
        protected void onBlockHit(final Block block, final Vector hitPoint, final BlockFace hitFace, final Vector hitForce) {
        }

        @Override
        protected void onEntityPassed(final Entity entity, final Vector passOrigin, final Vector passForce) {
            if (!(entity instanceof LivingEntity) || entity == player || hitTargets.contains(entity) || !damageCheck(player, (LivingEntity) entity)) {
                return;
            }

            final LivingEntity target = (LivingEntity) entity;
            hitTargets.add(target);

            addSpellTarget(target, hero);
            damageEntity(target, player, damage, EntityDamageEvent.DamageCause.MAGIC);
            target.setVelocity(target.getVelocity().add(new Vector(0, hitUpwardsVelocity, 0)));

            final CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
            targetCT.addEffect(new BurningEffect(this.skill, player, burnDuration, burnMultipliaer));

            currentTarget = null;
            tempIgnoreTargets.clear();
            setVelocity(getDirection().multiply(defaultSpeed));
        }
    }
}