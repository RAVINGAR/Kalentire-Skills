package com.herocraftonline.heroes.characters.skill.reborn.enderbeast;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseGroundEffect;
import com.herocraftonline.heroes.characters.skill.tools.BasicMissile;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.GeometryUtil;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.Effect;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.EffectType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
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
    private NMSHandler nmsHandler = NMSHandler.getInterface();

    public SkillEnderBreath(Heroes plugin) {
        super(plugin, "EnderBreath");
        setDescription("Launch a ball of Ender Flame towards your opponents. "
                + "The projectile disperses on contact, spreading dragon breath in a $4 block wide radius. "
                + "Enemies within the breath are dealt $1 damage every $2 second(s) for $3 second(s) and "
                + "if you are transformed, they suffer chaotic ender teleports.");
        setUsage("/skill enderbreath");
        setIdentifiers("skill enderbreath");
        setArgumentRange(0, 0);
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_ENDER, SkillType.ABILITY_PROPERTY_DARK, SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT);

        setToggleableEffectName(this.getName());
    }

    public String getDescription(Hero hero) {
        final double radius = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.RADIUS, false);
        long duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        final long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 200, false);
        final double damageTick = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE_TICK, false);

        int warmup = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DELAY, 0, false);
        int stamina = SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA, 0, false);
        int mana = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 0, false);
        long cooldown = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 0, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(damageTick))
                .replace("$2", Util.decFormat.format((double) period / 1000))
                .replace("$3", Util.decFormat.format((double) duration / 1000))
                .replace("$4", Util.decFormat.format(radius));
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.RADIUS.node(), 4.0);
        config.set(HEIGHT_NODE, 2.0);
        config.set(SkillSetting.DURATION.node(), 5000);
        config.set(SkillSetting.PERIOD.node(), 250);
        config.set(SkillSetting.DAMAGE_TICK.node(), 15.0);
        config.set(BasicMissile.PROJECTILE_SIZE_NODE, 0.4);
        config.set(BasicMissile.PROJECTILE_VELOCITY_NODE, 15.0);
        config.set(BasicMissile.PROJECTILE_GRAVITY_NODE, 14.7045);
        return config;
    }

    public SkillResult use(final Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        EnderBreathMissile missile = new EnderBreathMissile(plugin, this, hero);
        missile.fireMissile();

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    private class EnderBreathMissile extends BasicMissile {

        public EnderBreathMissile(Heroes plugin, Skill skill, Hero hero) {
            super(plugin, skill, hero, Particle.DRAGON_BREATH);
        }

        @Override
        protected void onBlockHit(Block block, Vector hitPoint, BlockFace hitFace, Vector hitForce) {
            explodeIntoGroundEffect(block.getRelative(hitFace).getLocation());
        }

        @Override
        protected void onValidTargetFound(LivingEntity target, Vector hitOrigin, Vector hitForce) {
            explodeIntoGroundEffect(target.getLocation());
        }

        private void explodeIntoGroundEffect(Location location) {
            final double radius = SkillConfigManager.getScaledUseSettingDouble(hero, skill, SkillSetting.RADIUS, false);
            double height = SkillConfigManager.getUseSetting(hero, skill, HEIGHT_NODE, 2.0, false);
            long duration = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DURATION, 6000, false);
            final long period = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.PERIOD, 200, false);
            final double damageTick = SkillConfigManager.getScaledUseSettingDouble(hero, skill, SkillSetting.DAMAGE_TICK, false);

            double teleportRadius = radius * 0.75;
            List<Location> locationsInCircle = GeometryUtil.getPerfectCircle(location, (int) teleportRadius, 1, false, false, 1);

            EnderFlameAoEGroundActions groundEffect = new EnderFlameAoEGroundActions(damageTick, radius, height, locationsInCircle);
            applyAreaGroundEffectEffect(hero, period, duration, location, radius, height, groundEffect);
        }
    }

    private class EnderFlameAoEGroundActions implements GroundEffectActionsWithVisuals {
        private final double damageTick;
        private final double radius;
        private final double height;
        private final List<Location> locationsInRadius;

        EnderFlameAoEGroundActions(double damageTick, double radius, double height, List<Location> locationsInRadius) {
            this.damageTick = damageTick;
            this.radius = radius;
            this.height = height;
            this.locationsInRadius = locationsInRadius;
        }

        @Override
        public void groundEffectTargetAction(Hero hero, final LivingEntity target, final AreaGroundEffectEffect groundEffect, EffectManager effectManager) {
            Player player = hero.getPlayer();
            if (!target.equals(player) && !damageCheck(player, target))
                return;

            if (target.equals(player)) {
                addSpellTarget(target, hero);
                damageEntity(target, player, damageTick, DamageCause.MAGIC, false);
            }

            if (!hero.hasEffect("EnderBeastTransformed"))
                return;

            teleportPlayer(player, target);
        }

        private void teleportPlayer(Player player, LivingEntity target) {
            int randomLocIndex = random.nextInt(locationsInRadius.size() - 1);
            Location desiredLocation = locationsInRadius.get(randomLocIndex).clone();
            World targetWorld = desiredLocation.getWorld();

            // this buggy shit might lauch someone to the moon, but until that happens
            // let's prentend this code has no bugs
            Location targetStartLoc = target.getLocation();
            int distance = (int) targetStartLoc.distance(desiredLocation);
            Vector dir = desiredLocation.clone().toVector().subtract(targetStartLoc.toVector());
            Location iterLoc = targetStartLoc.clone().setDirection(dir);
            BlockIterator iter;
            try {
                iter = new BlockIterator(iterLoc, 1, distance);
            } catch (IllegalStateException e) {
                return;
            }
            Block validFinalBlock = null;
            Block currentBlock;
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

            Location newLocation = validFinalBlock.getLocation();
            newLocation.setPitch(targetStartLoc.getPitch());
            newLocation.setYaw(targetStartLoc.getYaw());
            target.teleport(newLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);

            targetWorld.playEffect(newLocation, org.bukkit.Effect.ENDER_SIGNAL, 3);
            targetWorld.playSound(newLocation, Sound.ENTITY_ENDERMEN_TELEPORT, 0.6F, 1.0F);
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

            effectManager.start(visualEffect);

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
