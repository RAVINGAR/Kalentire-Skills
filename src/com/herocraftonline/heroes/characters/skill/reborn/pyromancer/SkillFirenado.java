package com.herocraftonline.heroes.characters.skill.reborn.pyromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.BurningEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.tools.Missile;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.effect.TornadoEffect;
import de.slikey.effectlib.EffectManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class SkillFirenado extends ActiveSkill {

    private static final Color FIRE_ORANGE = Color.fromRGB(226, 88, 34);
    private static final Color FIRE_RED = Color.fromRGB(236, 60, 30);

    public SkillFirenado(Heroes plugin) {
        super(plugin, "Firenado");
        setDescription("Conjure up a tornado of pure fire. The firenado roams around and seeks out nearby targets for the next $1 second(s). "
                + "Targets hit by the firenado are launched upwards, dealt $2 damage, and burned. "
                + "Burning targets take $3 fire tick damage over the next $4 second(s).");
        setUsage("/skill firenado");
        setArgumentRange(0, 0);
        setIdentifiers("skill firenado");
        setTypes(SkillType.ABILITY_PROPERTY_FIRE, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int tornadoDuration = SkillConfigManager.getUseSetting(hero, this, "tornado-duration", 8000, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 90, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        int burnDuration = SkillConfigManager.getUseSetting(hero, this, "burn-duration", 3000, false);
        double burnMultipliaer = SkillConfigManager.getUseSetting(hero, this, "burn-damage-multiplier", 2.0, false);
        double totalBurnDamage = plugin.getDamageManager().calculateFireTickDamage((int) (burnDuration / 50), burnMultipliaer);

        String formattedDuration = Util.decFormat.format(tornadoDuration / 1000);
        String formattedDamage = Util.decFormat.format(damage);
        String formattedBurnDamage = Util.decFormat.format(totalBurnDamage);
        String formattedBurnDuration = Util.decFormat.format(burnDuration / 1000);
        return getDescription().replace("$1", formattedDuration).replace("$2", formattedDamage).replace("$3", formattedBurnDamage).replace("$4", formattedBurnDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 75);
        config.set("require-blaze-rod", true);
        config.set("hit-upwards-velocity", 0.8);
        config.set("tornado-velocity", 4.0);
        config.set("tornado-duration", 8000);
        config.set("tornado-max-heat-seeking-distance", 25);
        config.set("tornado-visual-y-offset", 0.0);
        config.set("burn-duration", 3000);
        config.set("burn-damage-multiplier", 2.0);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        boolean requireBlazeRod = SkillConfigManager.getUseSetting(hero, this, "require-blaze-rod", true);
        if (requireBlazeRod) {
            PlayerInventory playerInv = player.getInventory();
            ItemStack mainHand = NMSHandler.getInterface().getItemInMainHand(playerInv);
            ItemStack offHand = NMSHandler.getInterface().getItemInOffHand(playerInv);
            if ((mainHand == null || mainHand.getType() != Material.BLAZE_ROD) && (offHand == null || offHand.getType() != Material.BLAZE_ROD)) {
                player.sendMessage("    " + ChatComponents.GENERIC_SKILL + "You are unable to cast this spell without wielding a Blaze Rod as a catalyst!");
                return SkillResult.INVALID_TARGET_NO_MSG;
            }
        }

        Location playerLoc = player.getLocation();

        double velocity = SkillConfigManager.getUseSetting(hero, this, "tornado-velocity", 4.0, false);

        FirenadoMissile missile = new FirenadoMissile(this, hero);
        Vector offset = playerLoc.getDirection().normalize().multiply(3);
        Location missileLoc = playerLoc.clone().add(offset);
        missileLoc.setPitch(0);
        missile.setLocationAndSpeed(missileLoc, velocity);
        missile.fireMissile();

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    class FirenadoMissile extends Missile {

        private final Skill skill;
        private final Hero hero;
        private final Player player;
        private final int initialDurationTicks;
        private final int maxHeatSeekingDistance;
        private final int heatSeekingIntervalTicks;
        private final double damage;
        private final int burnDuration;
        private final double burnMultipliaer;
        private final double hitUpwardsVelocity;
        private final HashSet<LivingEntity> hitTargets = new HashSet<LivingEntity>();

        final EffectManager effectManager = new EffectManager(plugin);
        final TornadoEffect vEffect = new TornadoEffect(effectManager);

        private double defaultSpeed;
        LivingEntity currentTarget = null;

        FirenadoMissile(Skill skill, Hero hero) {
            this.skill = skill;
            this.hero = hero;
            this.player = hero.getPlayer();

            this.damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 75.0, false);
            this.burnDuration = SkillConfigManager.getUseSetting(hero, skill, "burn-duration", 3000, false);
            this.burnMultipliaer = SkillConfigManager.getUseSetting(hero, skill, "burn-damage-multiplier", 2.0, false);

            this.hitUpwardsVelocity = SkillConfigManager.getUseSetting(hero, skill, "hit-upwards-velocity", 0.8, false);
            this.initialDurationTicks = SkillConfigManager.getUseSetting(hero, skill, "tornado-duration", 8000, false) / 50;
            this.maxHeatSeekingDistance = SkillConfigManager.getUseSetting(hero, skill, "tornado-max-heat-seeking-distance", 25, false);
            this.heatSeekingIntervalTicks = (int) (this.initialDurationTicks * 0.15);

            double radius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS, 4.0, false);

            setNoGravity();
            setDrag(0);
            setMass(1);
            setEntityDetectRadius(radius);
            setRemainingLife(this.initialDurationTicks);

            vEffect.period = 5;
            vEffect.iterations = (this.initialDurationTicks) / vEffect.period;

            vEffect.yOffset = SkillConfigManager.getUseSetting(hero, skill, "tornado-visual-y-offset", 0.0, false);
            vEffect.showCloud = true;
            vEffect.showTornado = true;
            vEffect.tornadoColor = FIRE_RED;
            vEffect.tornadoParticle = Particle.SPELL_MOB;
            vEffect.cloudParticle = Particle.CLOUD;
            vEffect.cloudColor = FIRE_ORANGE;
            vEffect.cloudSize = 1F;
            vEffect.tornadoHeight = (float) radius;
            vEffect.maxTornadoRadius = (float) radius / 2F;
            vEffect.asynchronous = true;
        }

        protected void onStart() {
            vEffect.setLocation(getLocation());
            effectManager.start(vEffect);
            this.defaultSpeed = getVelocity().length();
        }

        protected void onTick() {
            Location location = getLocation();
            vEffect.setLocation(location);
            Block block = location.getBlock();

            Util.setBlockOnFireIfAble(block, 0.3, true);
            Util.setBlockOnFireIfAble(block.getRelative(BlockFace.DOWN), 0.3, true);

            if (getTicksLived() % this.heatSeekingIntervalTicks != 0) {
                if (currentTarget != null)
                    addForce(getDirection());
                return;
            }

            flipColors();
            LivingEntity target = getClosestEntity();
            if (target != null) {
                currentTarget = target;
                Vector difference = target.getLocation().clone().subtract(location).toVector();
                setDirection(difference.normalize());
                setVelocity(difference.multiply(new Vector(0.5, 1, 0.5)));
            } else {
                currentTarget = null;
            }
        }

        private void flipColors() {
            if (vEffect.tornadoColor == FIRE_ORANGE)
                vEffect.tornadoColor = FIRE_RED;
            else
                vEffect.tornadoColor = FIRE_ORANGE;

            if (vEffect.cloudColor == FIRE_ORANGE)
                vEffect.cloudColor = FIRE_RED;
            else
                vEffect.cloudColor = FIRE_ORANGE;
        }

        private LivingEntity getClosestEntity() {
            double closestEntDistance = 9999;
            LivingEntity closestEntity = null;

            Collection<Entity> nearbyEnts = getWorld().getNearbyEntities(getLocation(), maxHeatSeekingDistance, maxHeatSeekingDistance * 0.5, maxHeatSeekingDistance);
            for (Entity ent : nearbyEnts) {
                if (!(ent instanceof LivingEntity))
                    continue;
                LivingEntity lEnt = (LivingEntity) ent;
                if (hitTargets.contains(lEnt))
                    continue;
                if (!damageCheck(player, lEnt))
                    continue;

                double distance = getLocation().distance(lEnt.getLocation());
                if (distance < closestEntDistance) {
                    closestEntity = lEnt;
                    closestEntDistance = distance;
                }
            }
            return closestEntity;
        }

        protected void onFinalTick() {
            effectManager.dispose();
        }

        // This didn't really work so great.
        protected boolean onCollideWithBlock(Block block, Vector point, BlockFace face) {
            // Make it "bounce" and go the other way.
//            if (currentTarget == null) {
//                Vector direction = getDirection();
//                setLocation(getLocation().clone().subtract(direction));
//                setDirectionAndSpeed(direction.multiply(-1), defaultSpeed);
//            }
            return false;
        }

        // Don't ever "collide" with an entity
        protected boolean onCollideWithEntity(Entity entity) {
            return false;
        }

        // Hit around walls
        protected boolean onBlockProtectsEntity(Block block, Entity entity, Vector point, BlockFace face) {
            return false;
        }

        @Override
        protected void onEntityPassed(Entity entity, Vector passOrigin, Vector passForce) {
            if (!(entity instanceof LivingEntity) || entity == player || hitTargets.contains(entity) || !damageCheck(player, (LivingEntity) entity))
                return;

            LivingEntity target = (LivingEntity) entity;
            hitTargets.add(target);

            addSpellTarget(target, hero);
            damageEntity(target, player, damage, EntityDamageEvent.DamageCause.MAGIC);
            target.setVelocity(target.getVelocity().add(new Vector(0, hitUpwardsVelocity, 0)));

            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
            targetCT.addEffect(new BurningEffect(this.skill, player, burnDuration, burnMultipliaer));

            currentTarget = null;
            setVelocity(getDirection().setY(0).multiply(defaultSpeed));
        }

        @Override
        protected void onBlockHit(Block block, Vector hitPoint, BlockFace hitFace, Vector hitForce) {
            effectManager.dispose();
        }
    }
}