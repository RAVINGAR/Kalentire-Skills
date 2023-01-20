package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseGroundEffect;
import com.herocraftonline.heroes.characters.skill.tools.BasicMissile;
import com.herocraftonline.heroes.libs.slikey.effectlib.EffectManager;
import com.herocraftonline.heroes.libs.slikey.effectlib.EffectType;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.GeometryUtil;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Random;

public class SkillEnderBreath extends SkillBaseGroundEffect {

    private static final Random random = new Random(System.currentTimeMillis());
    private final NMSHandler nmsHandler = NMSHandler.getInterface();

    public SkillEnderBreath(final Heroes plugin) {
        super(plugin, "EnderBreath");
        setDescription("Launch a ball of Ender Flame towards your opponents. "
                + "The projectile disperses on contact, spreading dragon breath in a $4 block wide radius. "
                + "Enemies within the breath are dealt $1 damage every $2 second(s) for $3 second(s) and "
                + "if you are transformed, they suffer chaotic ender teleports.");
        setUsage("/skill enderbreath");
        setArgumentRange(0, 0);
        setIdentifiers("skill enderbreath");
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_ENDER, SkillType.ABILITY_PROPERTY_DARK, SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT);

        setToggleableEffectName(this.getName());
    }

    @Override
    public String getDescription(final Hero hero) {
        final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 4.0, false);
        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);
        final long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 200, false);
        final double damageTick = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 50d, false);

        final int warmup = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DELAY, 0, false);
        final int stamina = SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA, 0, false);
        final int mana = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 0, false);
        final long cooldown = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 0, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(damageTick))
                .replace("$2", Util.decFormat.format((double) period / 1000))
                .replace("$3", Util.decFormat.format((double) duration / 1000))
                .replace("$4", Util.decFormat.format(radius));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.RADIUS.node(), 4);
        node.set(HEIGHT_NODE, 2.0);
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set(SkillSetting.PERIOD.node(), 250);
        node.set(SkillSetting.DAMAGE_TICK.node(), 15.0);
        node.set("projectile-velocity", 15.0);
        node.set("projectile-gravity", 14.7045);
        return node;
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        final double projVelocity = SkillConfigManager.getUseSetting(hero, this, "projectile-velocity", 15.0, false);
        final double projGravity = SkillConfigManager.getUseSetting(hero, this, "projectile-gravity", 14.7045, false);
        final EnderBreathMissile missile = new EnderBreathMissile(plugin, this, hero);
        missile.setGravity(projGravity);
        missile.fireMissile();

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    private double getRandomInRange(final double minValue, final double maxValue) {
        return minValue + random.nextDouble() * ((maxValue - minValue) + 1);
    }

    private float getRandomInRange(final float minValue, final float maxValue) {
        return minValue + random.nextFloat() * ((maxValue - minValue) + 1);
    }

    private class EnderBreathMissile extends BasicMissile {

        public EnderBreathMissile(final Heroes plugin, final Skill skill, final Hero hero) {
            super(plugin, skill, hero, Particle.DRAGON_BREATH, Color.PURPLE, true);
        }

        @Override
        protected void onBlockHit(final Block block, final Vector hitPoint, final BlockFace hitFace, final Vector hitForce) {
            explodeIntoGroundEffect(block.getRelative(hitFace).getLocation());
        }

        @Override
        protected void onEntityHit(final Entity entity, final Vector hitOrigin, final Vector hitForce) {
            explodeIntoGroundEffect(entity.getLocation());
        }

        private void explodeIntoGroundEffect(final Location location) {
            final double radius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS, 4.0, false);
            final double height = SkillConfigManager.getUseSetting(hero, skill, HEIGHT_NODE, 2.0, false);
            final long duration = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DURATION, 6000, false);
            final long period = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.PERIOD, 200, false);
            final double damageTick = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_TICK, 50d, false);

            final double teleportRadius = radius * 0.75;
            final List<Location> locationsInCircle = GeometryUtil.getPerfectCircle(location, (int) teleportRadius, 1, false, false, 1);

            final EnderFlameAoEGroundActions groundEffect = new EnderFlameAoEGroundActions(damageTick, radius, height, locationsInCircle);
            applyAreaGroundEffectEffect(hero, period, duration, location, radius, height, groundEffect);
        }
    }

    private class EnderFlameAoEGroundActions implements GroundEffectActionsWithVisuals {

        private final double damageTick;
        private final double radius;
        private final double height;
        private final List<Location> locationsInRadius;

        EnderFlameAoEGroundActions(final double damageTick, final double radius, final double height, final List<Location> locationsInRadius) {
            this.damageTick = damageTick;
            this.radius = radius;
            this.height = height;
            this.locationsInRadius = locationsInRadius;
        }

        @Override
        public void groundEffectTargetAction(final Hero hero, final LivingEntity target, final AreaGroundEffectEffect groundEffect, final EffectManager effectManager) {
            final Player player = hero.getPlayer();
            if (!damageCheck(player, target)) {
                return;
            }

            addSpellTarget(target, hero);
            damageEntity(target, player, damageTick, DamageCause.MAGIC, 0.0f);

            if (!hero.hasEffect("EnderBeastTransformed")) {
                return;
            }

            teleportPlayer(player, target);
        }

        private void teleportPlayer(final Player player, final LivingEntity target) {
            final int randomLocIndex = random.nextInt(locationsInRadius.size() - 1);
            final Location desiredLocation = locationsInRadius.get(randomLocIndex).clone();
            final World targetWorld = desiredLocation.getWorld();

            // this buggy shit might lauch someone to the moon, but until that happens
            // let's prentend this code has no bugs
            final Location targetStartLoc = target.getLocation();
            final int distance = (int) targetStartLoc.distance(desiredLocation);
            final Vector dir = desiredLocation.clone().toVector().subtract(targetStartLoc.toVector());
            final Location iterLoc = targetStartLoc.clone().setDirection(dir);
            final BlockIterator iter;
            try {
                iter = new BlockIterator(iterLoc, 1, distance);
            } catch (final IllegalStateException e) {
                return;
            }
            Block validFinalBlock = null;
            Block currentBlock;
            while (iter.hasNext()) {
                currentBlock = iter.next();
                final Material currentBlockType = currentBlock.getType();
                if (!Util.transparentBlocks.contains(currentBlockType)) {
                    break;
                }

                if (Util.transparentBlocks.contains(currentBlock.getRelative(BlockFace.UP).getType())) {
                    validFinalBlock = currentBlock;
                }
            }

            if (validFinalBlock == null) {
                return;
            }

            final Location newLocation = validFinalBlock.getLocation();
            newLocation.setPitch(targetStartLoc.getPitch());
            newLocation.setYaw(targetStartLoc.getYaw());
            target.teleport(newLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);

            targetWorld.playEffect(newLocation, org.bukkit.Effect.ENDER_SIGNAL, 3);
            targetWorld.playSound(newLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6F, 1.0F);
        }

        @Override
        public void groundEffectTickAction(final Hero hero, final AreaGroundEffectEffect effect, final EffectManager effectManager) {
            final Player player = hero.getPlayer();

            final com.herocraftonline.heroes.libs.slikey.effectlib.Effect visualEffect = new com.herocraftonline.heroes.libs.slikey.effectlib.Effect(effectManager) {
                final Particle particle = Particle.DRAGON_BREATH;
                final double randomMin = -0.15;
                final double randomMax = 0.15;

                @Override
                public void onRun() {
                    for (double z = -radius; z <= radius; z += 0.33) {
                        for (double x = -radius; x <= radius; x += 0.33) {
                            if (x * x + z * z <= radius * radius) {
                                final double randomX = x + getRandomInRange(randomMin, randomMax);
                                final double randomZ = z + getRandomInRange(randomMin, randomMax);
                                display(particle, getLocation().clone().add(randomX, 0, randomZ));
                            }
                        }
                    }
                }
            };

            final Location location = effect.getLocation().clone();
            visualEffect.asynchronous = true;
            visualEffect.iterations = 1;
            visualEffect.type = EffectType.INSTANT;
            visualEffect.setLocation(location);

            visualEffect.start();

            player.getWorld().playSound(location, Sound.ENTITY_GENERIC_BURN, 0.15f, 0.0001f);
        }
    }
}
