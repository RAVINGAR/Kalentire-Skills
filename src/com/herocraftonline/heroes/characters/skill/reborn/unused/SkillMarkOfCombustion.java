package com.herocraftonline.heroes.characters.skill.reborn.unused;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.StackingEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.tools.Missile;
import com.herocraftonline.heroes.chat.ChatComponents;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.BigBangEffect;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.Collection;

public class SkillMarkOfCombustion extends ActiveSkill {

    private static final Color FIRE_ORANGE = Color.fromRGB(226, 88, 34);
    private static final Color FIRE_RED = Color.fromRGB(236, 60, 30);
    private String applyText;
    private String failureExpireText;
    private String expireText;

    public SkillMarkOfCombustion(Heroes plugin) {
        super(plugin, "MarkOfCombustion");
        setDescription("TBD");
        setUsage("/skill markofcombustion");
        setArgumentRange(0, 0);
        setIdentifiers("skill markofcombustion");
        setTypes(SkillType.ABILITY_PROPERTY_FIRE, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 80, false);
        return getDescription().replace("%1", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("projectile-damage", 25.0);
        config.set("projectile-max-duration", 750);
        config.set("projectile-heat-seeking-distance", 5);
        config.set("projectile-heat-seeking-force-multiplier", 1.5);

        config.set("combust-duration", 10000);
        config.set("combust-damage-per-stack", 50);
        config.set("combust-duration", 10000);

        config.set("projectile-radius", 0.5);
        config.set("num-projectiles", 3);
        config.set("projectile-launch-delay-ticks", 2);

        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% been Marked for Combustion by %hero%!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero%'s Combustion Mark explodes on %target%!");
        config.set("failure-expire-text", ChatComponents.GENERIC_SKILL + "%hero%'s Combustion Mark fades from %target% harmlessly.");

        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                ChatComponents.GENERIC_SKILL + "%target% been Marked for Combustion by %hero%!")
                .replace("%hero%", "$2")
                .replace("%target%", "$1");

        failureExpireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero%'s Combustion Mark fades from %target% harmlessly.")
                .replace("%hero%", "$2")
                .replace("%target%", "$1");

        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero%'s Combustion Mark explodes on %target%!")
                .replace("%hero%", "$2")
                .replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        double velocity = SkillConfigManager.getUseSetting(hero, this, "projectile-velocity", 20.0, false);
        int numProjectiles = SkillConfigManager.getUseSetting(hero, this, "num-projectiles", 3, false);
        int projectileLaunchDelay = SkillConfigManager.getUseSetting(hero, this, "projectile-launch-delay-ticks", 2, false);

        Skill skill = this;
        for(int i = 0; i < numProjectiles; i++) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                public void run() {
                    Location eyeLoc = player.getEyeLocation();
                    CombustionMissile missile = new CombustionMissile(hero, skill);
                    Vector offset = eyeLoc.getDirection().clone().normalize().multiply(2).add(new Vector(0, 1, 0));
                    Location missileLoc = eyeLoc.clone().add(offset);
                    missile.setLocationAndSpeed(missileLoc, velocity);
                    missile.fireMissile();
                }
            }, projectileLaunchDelay * i);
        }

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    class CombustionStacks extends StackingEffect {

        private final double damagePerStack;
        private final int minmumStacksToExplode;

        CombustionStacks(Skill skill, String playerSpecificEffectName, Player applier, double damagePerStack, int minmumStacksToExplode, int maxStacks) {
            super(skill, playerSpecificEffectName, applier, maxStacks, true, applyText, expireText);
            this.damagePerStack = damagePerStack;
            this.minmumStacksToExplode = minmumStacksToExplode;

            types.add(EffectType.FIRE);
            types.add(EffectType.HARMFUL);
            types.add(EffectType.DISPELLABLE);
        }

        @Override
        public void removeFromHero(Hero targetHero) {
            if (this.getStackCount() < this.minmumStacksToExplode) {
                targetHero.getPlayer().sendMessage("You should not see an explosion message.");
                this.setRemoveText(failureExpireText);
                super.removeFromHero(targetHero);
                return;
            }

            Player targetPlayer = targetHero.getPlayer();
            if (damageCheck(applier, (LivingEntity) targetPlayer)) {
                addSpellTarget(targetPlayer, targetHero);
                damageEntity(targetPlayer, applier, this.damagePerStack * this.getStackCount(), EntityDamageEvent.DamageCause.MAGIC);
            }
        }
    }

    class CombustionMissile extends Missile {

        private final Hero hero;
        private final Player player;
        private final Skill skill;

        private final int initialDurationTicks;
        private final int maxHeatSeekingDistance;
        private final int heatSeekingIntervalTicks;
        private final double heatSeekForce;
        private final double projectileDamage;
        private final double radius;

        private final int combustDuration;
        private final int combustDamage;
        private final int combustMaxStacks;
        private final int combustMinStacksToExplode;

        final EffectManager vEffectManager = new EffectManager(plugin);
        final BigBangEffect vFireEffect = new BigBangEffect(vEffectManager);
//        final FireworkEffect fireworkEffect;

        private double defaultSpeed;
        LivingEntity currentTarget = null;

        CombustionMissile(Hero hero, Skill skill) {
            this.hero = hero;
            this.skill = skill;
            this.player = hero.getPlayer();

            this.projectileDamage = SkillConfigManager.getUseSetting(hero, skill, "projectile-damage", 25, false);
            this.initialDurationTicks = SkillConfigManager.getUseSetting(hero, skill, "projectile-max-duration", 750, false) / 50;
            this.maxHeatSeekingDistance = SkillConfigManager.getUseSetting(hero, skill, "projectile-heat-seeking-distance", 5, false);
            this.heatSeekForce = SkillConfigManager.getUseSetting(hero, skill, "projectile-heat-seeking-force-multiplier", 1.5, false);
            this.heatSeekingIntervalTicks = (int) (this.initialDurationTicks * 0.15);

            this.combustDuration = SkillConfigManager.getUseSetting(hero, skill, "combust-duration", 10000, false);
            this.combustDamage = SkillConfigManager.getUseSetting(hero, skill, "combust-damage-per-stack", 100, false);
            this.combustMinStacksToExplode = SkillConfigManager.getUseSetting(hero, skill, "combust-minimum-stacks-to-explode", 5, false);
            this.combustMaxStacks = SkillConfigManager.getUseSetting(hero, skill, "combust-max-stacks", 20, false);

            this.radius = SkillConfigManager.getUseSetting(hero, skill, "projectile-radius", 0.5, false);

            setNoGravity();
            setDrag(0);
            setEntityDetectRadius(radius);
            setRemainingLife(this.initialDurationTicks);

//            fireworkEffect = FireworkEffect.builder()
//                    .with(FireworkEffect.Type.BURST)
//                    .withColor(FIRE_ORANGE)
//                    .withColor(FIRE_RED)
//                    .withFade(Color.BLACK)
//                    .flicker(true)
//                    .trail(true)
//                    .build();

//            int innerVisualRadius = (int) (radius * 0.5);
//            vFireEffect.color = FIRE_ORANGE;
//            vFireEffect.color2 = FIRE_RED;
//            vFireEffect.color3 = FIRE_ORANGE;
//            vFireEffect.explosions = 1;
//            vFireEffect.intensity = 2;
//            vFireEffect.soundInterval = 0;
//            vFireEffect.explosions = 2;
////            vFireEffect.radius = (int) (radius * 2);
//            vFireEffect.iterations = (this.initialDurationTicks) / vFireEffect.period;
//            vFireEffect.asynchronous = true;

//            int innerVisualRadius = (int) (radius * 0.5);
//            vFireEffect.particle = Particle.FLAME;
//            vFireEffect.radius = innerVisualRadius >= 0.1 ? innerVisualRadius : radius;   // Make the inner sphere smaller but only if we can
//            vFireEffect.iterations = (this.initialDurationTicks) / vFireEffect.period;
//            vFireEffect.asynchronous = true;
        }

        private void updateVisualLocation() {
//            spawnFirework(getLocation());
//            vFireEffect.setLocation(getLocation());
            Location location = getLocation();
            World world = location.getWorld();
            world.spawnParticle(Particle.REDSTONE, location, 5, 0, 0, 0, 1, new Particle.DustOptions(FIRE_RED, 1));
//            for (int i = 0; i < this.radius; i += radius * .1) {
                world.spawnParticle(Particle.REDSTONE, location, 1, -0.25, 0, 0, 0, new Particle.DustOptions(FIRE_ORANGE, 1));
                world.spawnParticle(Particle.REDSTONE, location, 1, 0, -0.25, 0, 0, new Particle.DustOptions(FIRE_ORANGE, 1));
                world.spawnParticle(Particle.REDSTONE, location, 1, 0, 0, -0.25, 0, new Particle.DustOptions(FIRE_ORANGE, 1));
                world.spawnParticle(Particle.REDSTONE, location, 1, 0.25, 0, 0, 0, new Particle.DustOptions(FIRE_ORANGE, 1));
                world.spawnParticle(Particle.REDSTONE, location, 1, 0, 0.25, 0, 0, new Particle.DustOptions(FIRE_ORANGE, 1));
                world.spawnParticle(Particle.REDSTONE, location, 1, 0, 0, 0.25, 0, new Particle.DustOptions(FIRE_ORANGE, 1));
//            }
            world.playSound(location, Sound.BLOCK_FIRE_EXTINGUISH, 0.5F, 0.7F);
            world.playSound(location, Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 0.7F, 0.5F);
//            player.getWorld().playEffect(location, Effect.MOBSPAWNER_FLAMES, 1);
        }

//        protected void spawnFirework(Location location) {
//            Firework firework = (Firework) getWorld().spawnEntity(getLocation(), EntityType.FIREWORK);
//            FireworkMeta meta = firework.getFireworkMeta();
//            meta.setPower(1);
//            meta.addEffect(this.fireworkEffect);
//            meta.addEffect(this.fireworkEffect);
//            firework.setFireworkMeta(meta);
//            firework.detonate();
//        }

        protected void onStart() {
            this.defaultSpeed = getVelocity().length();
            updateVisualLocation();
//            vEffectManager.start(vFireEffect);
        }

        protected void onTick() {
            if (getTicksLived() % 2 == 0)
                updateVisualLocation();

            if (getTicksLived() % this.heatSeekingIntervalTicks != 0) {
                if (this.currentTarget != null) {
                    addForce(getDirection().multiply(this.heatSeekForce));
                }
                return;
            }

            LivingEntity target = getClosestEntity();
            if (target != null) {
                this.currentTarget = target;
                Vector difference = target.getLocation().clone().subtract(getLocation()).toVector();
                setDirection(difference.normalize());
                addForce(getDirection().multiply(this.heatSeekForce));
            } else {
                currentTarget = null;
            }
        }

        private LivingEntity getClosestEntity() {
            double closestEntDistance = 9999;
            LivingEntity closestEntity = null;

            Collection<Entity> nearbyEnts = getWorld().getNearbyEntities(getLocation(), maxHeatSeekingDistance, maxHeatSeekingDistance * 0.5, maxHeatSeekingDistance);
            for (Entity ent : nearbyEnts) {
                if (!(ent instanceof LivingEntity))
                    continue;
                LivingEntity lEnt = (LivingEntity) ent;
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
            vEffectManager.dispose();
        }

        protected boolean onCollideWithEntity(Entity entity) {
            if (!(entity instanceof LivingEntity) || entity == player || !damageCheck(player, (LivingEntity) entity))
                return false;
            return true;
        }

        protected void onEntityHit(Entity entity, Vector hitOrigin, Vector hitForce) {
            LivingEntity target = (LivingEntity) entity;

            addSpellTarget(target, hero);
            damageEntity(target, player, this.projectileDamage, EntityDamageEvent.DamageCause.MAGIC);

            String heroSpecificEffectName = player.getName() + "-CombustionStacks";    // Using applier name so that multiple pyros can stack this at once.
            CharacterTemplate ctTarget = plugin.getCharacterManager().getCharacter(target);
            boolean addedNewStack = ctTarget.addEffectStack(
                    heroSpecificEffectName, skill, player, this.combustDuration,
                    () -> new CombustionStacks(skill, heroSpecificEffectName, player, this.combustDamage, this.combustMinStacksToExplode, this.combustMaxStacks)
            );

            // Max stacks, BLOW EM UP
            if (!addedNewStack) {
                ctTarget.removeEffect(ctTarget.getEffect(heroSpecificEffectName));
            }
        }
    }
}