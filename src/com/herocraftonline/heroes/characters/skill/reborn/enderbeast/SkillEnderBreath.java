package com.herocraftonline.heroes.characters.skill.reborn.enderbeast;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseGroundEffect;
import com.herocraftonline.heroes.characters.skill.tools.BasicMissile;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.Effect;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.EffectType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.*;

public class SkillEnderBreath extends SkillBaseGroundEffect {

    private static final Random random = new Random(System.currentTimeMillis());
    private NMSHandler nmsHandler = NMSHandler.getInterface();

    public SkillEnderBreath(Heroes plugin) {
        super(plugin, "EnderBreath");
        setDescription("Launch a ball of Ender Flame at your opponent. "
                + "The projectile explodes on hit, spreading dragon breath $4 blocks to the side and $5 blocks up and down (cylinder). "
                + "Enemies within the breath are dealt $1 damage every $2 second(s) for $3 second(s) and"
                + "if you are transformed, they suffer chaotic ender teleports.");
        setUsage("/skill enderbreath");
        setArgumentRange(0, 0);
        setIdentifiers("skill enderbreath");
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_ENDER, SkillType.ABILITY_PROPERTY_DARK, SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT);

        setToggleableEffectName(this.getName());
    }

    public String getDescription(Hero hero) {
        final int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 4, false);
        int height = SkillConfigManager.getUseSetting(hero, this, HEIGHT_NODE, 2, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);
        final long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 200, false);
        final double damageTick = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 50d, false);

        int warmup = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DELAY, 0, false);
        int stamina = SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA, 0, false);
        int mana = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 0, false);
        long cooldown = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 0, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(damageTick))
                .replace("$2", Util.decFormat.format((double) period / 1000))
                .replace("$3", Util.decFormat.format((double) duration / 1000))
                .replace("$4", Util.decFormat.format(radius))
                .replace("$5", Util.decFormat.format(height));
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.RADIUS.node(), 4);
        node.set(HEIGHT_NODE, 2);
        node.set(SkillSetting.DURATION.node(), 6000);
        node.set(SkillSetting.PERIOD.node(), 200);
        node.set(SkillSetting.DAMAGE_TICK.node(), 50d);
        node.set("projectile-velocity", 15.0);
        node.set("projectile-gravity", 14.7045);
        return node;
    }

    public SkillResult use(final Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        double projVelocity = SkillConfigManager.getUseSetting(hero, this, "projectile-velocity", 15.0, false);
        double projGravity = SkillConfigManager.getUseSetting(hero, this, "projectile-gravity", 14.7045, false);
        EnderBreathMissile missile = new EnderBreathMissile(plugin, this, hero, 0.4, projVelocity);
        missile.setGravity(projGravity);
        missile.fireMissile();

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    private class EnderBreathMissile extends BasicMissile {

        public EnderBreathMissile(Plugin plugin, Skill skill, Hero hero, double projectileSize, double projVelocity) {
            super(plugin, skill, hero, projectileSize, Particle.DRAGON_BREATH, projVelocity);
        }

        @Override
        protected void onBlockHit(Block block, Vector hitPoint, BlockFace hitFace, Vector hitForce) {
            explodeIntoGroundEffect(block.getRelative(hitFace).getLocation());
        }

        @Override
        protected void onEntityHit(Entity entity, Vector hitOrigin, Vector hitForce) {
            explodeIntoGroundEffect(entity.getLocation());
        }

        private void explodeIntoGroundEffect(Location location) {
            final int radius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS, 4, false);
            int height = SkillConfigManager.getUseSetting(hero, skill, HEIGHT_NODE, 2, false);
            long duration = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DURATION, 6000, false);
            final long period = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.PERIOD, 200, false);
            final double damageTick = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_TICK, 50d, false);

            int teleportRadius = (int) (radius * 0.75);
            List<Location> locationsInCircle = Util.getCircleLocationList(location, teleportRadius, 1, false, false, 1);

            EnderFlameAoEGroundActions groundEffect = new EnderFlameAoEGroundActions(damageTick, radius, height, locationsInCircle);
            applyAreaGroundEffectEffect(hero, period, duration, location, radius, height, groundEffect);
        }
    }

    private class EnderFlameAoEGroundActions implements GroundEffectActionsWithVisuals {

        private final double damageTick;
        private final int radius;
        private final int height;
        private final List<Location> locationsInRadius;

        EnderFlameAoEGroundActions(double damageTick, int radius, int height, List<Location> locationsInRadius) {
            this.damageTick = damageTick;
            this.radius = radius;
            this.height = height;
            this.locationsInRadius = locationsInRadius;
        }

        @Override
        public void groundEffectTargetAction(Hero hero, final LivingEntity target, final AreaGroundEffectEffect groundEffect, EffectManager effectManager) {
            Player player = hero.getPlayer();
            if (!damageCheck(player, target))
                return;

            addSpellTarget(target, hero);
            damageEntity(target, player, damageTick, DamageCause.MAGIC, false);

            if (!hero.hasEffect("EnderBeastTransformed"))
                return;

            teleportPlayer(player, target);
        }

        private void teleportPlayer(Player player, LivingEntity target) {
            int randomLocIndex = random.nextInt(locationsInRadius.size() - 1);

            Location desiredLocation = locationsInRadius.get(randomLocIndex).clone();
            World targetWorld = desiredLocation.getWorld();

            int distance = (int) target.getLocation().distance(desiredLocation);
//                Vector direction = distance.normalize();
//                RayCastHit hit = nmsHandler.getNMSPhysics().rayCast(
//                        targetWorld,
//                        target,
//                        target.getEyeLocation().toVector(),
//                        desiredLocation.toVector(),
//                        x -> x.getType().isSolid(),
//                        x -> false);
//
//                if (hit != null) {
//                    Heroes.log(Level.INFO, "Raycast Hit: " + hit.getPoint().toString());
//                } else {
//                    Heroes.log(Level.INFO, "Raycast Hit: null.");
//                }
//
//                if (hit == null || hit.getBlock(targetWorld) == null)
//                    return;
//                Location newLocation = hit.getBlock(targetWorld).getLocation();
            Block validFinalBlock = null;
            Block currentBlock;

            Vector dir = desiredLocation.clone().subtract(target.getEyeLocation()).toVector();
            Location iterLoc = target.getLocation().clone().setDirection(dir);

            BlockIterator iter = null;
            try {
                iter = new BlockIterator(iterLoc, distance);
            } catch (IllegalStateException e) {
                return;
            }

            while (iter.hasNext()) {
                currentBlock = iter.next();
                Material currentBlockType = currentBlock.getType();

                if (!Util.transparentBlocks.contains(currentBlockType))
                    break;

                if (Util.transparentBlocks.contains(currentBlock.getRelative(BlockFace.UP).getType()))
                    validFinalBlock = currentBlock;
            }

            if (validFinalBlock == null)
                return;

            Location newLocation = validFinalBlock.getLocation().clone().add(new Vector(.5, 0, .5));
            target.teleport(newLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
            targetWorld.playEffect(newLocation, org.bukkit.Effect.ENDER_SIGNAL, 3);
            targetWorld.playSound(newLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6F, 1.0F);
        }

        @Override
        public void groundEffectTickAction(Hero hero, AreaGroundEffectEffect effect, EffectManager effectManager) {
            final Player player = hero.getPlayer();

            Effect visualEffect = new Effect(effectManager) {
                Particle particle = Particle.DRAGON_BREATH;
                final double randomMin = -0.15;
                final double randomMax = 0.15;

                @Override
                public void onRun() {
                    for (double z = -radius; z <= radius; z += 0.33) {
                        for (double x = -radius; x <= radius; x += 0.33) {
                            if (x * x + z * z <= radius * radius) {
                                double randomX = x + getRandomInRange(randomMin, randomMax);
                                double randomZ = z + getRandomInRange(randomMin, randomMax);
                                display(particle, getLocation().clone().add(randomX, 0, randomZ));
                            }
                        }
                    }
                }
            };

            Location location = effect.getLocation().clone();
            visualEffect.asynchronous = true;
            visualEffect.iterations = 1;
            visualEffect.type = EffectType.INSTANT;
            visualEffect.setLocation(location);

            visualEffect.start();

            player.getWorld().playSound(location, Sound.ENTITY_GENERIC_BURN, 0.15f, 0.0001f);
        }
    }

    private double getRandomInRange(double minValue, double maxValue) {
        return minValue + random.nextDouble() * ((maxValue - minValue) + 1);
    }

    private float getRandomInRange(float minValue, float maxValue) {
        return minValue + random.nextFloat() * ((maxValue - minValue) + 1);
    }
}
