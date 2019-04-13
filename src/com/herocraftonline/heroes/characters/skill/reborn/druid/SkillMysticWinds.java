package com.herocraftonline.heroes.characters.skill.reborn.druid;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.tools.Missile;
import com.herocraftonline.heroes.util.GeometryUtil;
import com.herocraftonline.heroes.util.Pair;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.SphereEffect;
import de.slikey.effectlib.util.DynamicLocation;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class SkillMysticWinds extends ActiveSkill {

    private static final Color FIRE_ORANGE = Color.fromRGB(226, 88, 34);
    private static final Color FIRE_RED = Color.fromRGB(236, 60, 30);
    private static String toggleableEffectName = "FloatingMysticWinds";

    public SkillMysticWinds(Heroes plugin) {
        super(plugin, "MysticWinds");
        setDescription("Call $1 mystical gales of wind that will float and remain inactive around the caster for up to $2 seconds. " +
                "If this ability is cast again within that time, it will unleash each stored missile in a stream. " +
                "Each missile does $3 damage, increased by $4 for each number of missile you previously landed on the same target. " +
                "Total healing for hitting every projectile is $5!");
        setUsage("/skill mysticwinds");
        setArgumentRange(0, 0);
        setIdentifiers("skill mysticwinds");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);

        setToggleableEffectName(toggleableEffectName);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int numProjectiles = SkillConfigManager.getUseSetting(hero, this, "num-projectiles", 4, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 15000, false);
        double damage = SkillConfigManager.getUseSetting(hero, this, "projectile-damage", 25.0, false);
        double damageIncreasePerHit = SkillConfigManager.getUseSetting(hero, this, "projectile-damage-increase-per-hit", 10.0, false);

        // lmao
        double totalMaxPossibleDamage = numProjectiles*damage + damageIncreasePerHit*numProjectiles*(numProjectiles-1)/2;

        return getDescription()
                .replace("$1", numProjectiles + "")
                .replace("$2", Util.decFormat.format((double) duration / 1000))
                .replace("$3", Util.decFormat.format(damage))
                .replace("$4", Util.decFormat.format(damageIncreasePerHit))
                .replace("$5", Util.decFormat.format(totalMaxPossibleDamage));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DURATION.node(), 15000);
        config.set("projectile-damage", 25.0);
        config.set("projectile-damage-increase-per-hit", 10.0);
        config.set("projectile-velocity", 65.0);
        config.set("projectile-max-ticks-lived", 30);
        config.set("projectile-radius", 0.25);
        config.set("projectile-launch-delay-ticks", 15);
        config.set("num-projectiles", 5);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 15000, false);
        hero.addEffect(new MysticWindsEffect(this, player, duration));
        return SkillResult.NORMAL;
    }

    private class SkillHeroListener implements Listener {
        private Skill skill;

        SkillHeroListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onLeftClick(PlayerInteractEvent event) {
            if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK)
                return;

            Player player = event.getPlayer();
            Hero hero = plugin.getCharacterManager().getHero(player);
            if (!hero.hasEffect(toggleableEffectName))
                return;

            hero.removeEffect(hero.getEffect(toggleableEffectName));
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (event.isProjectile() || !(event.getDamager() instanceof Hero) || !(event.getDamager().hasEffect(toggleableEffectName)))
                return;

            event.getDamager().removeEffect(event.getDamager().getEffect(toggleableEffectName));
        }
    }

    private class MysticWindsEffect extends ExpirableEffect {
        private int numProjectiles;
        private double projectileRadius;
        private List<Pair<EffectManager, SphereEffect>> missileVisuals = new ArrayList<Pair<EffectManager, SphereEffect>>();

        MysticWindsEffect(Skill skill, Player applier, long duration) {
            super(skill, toggleableEffectName, applier, duration);

            this.types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            this.numProjectiles = SkillConfigManager.getUseSetting(hero, skill, "num-projectiles", 4, false);
            this.projectileRadius = SkillConfigManager.getUseSetting(hero, skill, "projectile-radius", 0.15, false);
            int projDurationTicks = SkillConfigManager.getUseSetting(hero, skill, "projectile-max-ticks-lived", 30, false);

            List<Location> missileLocations = GeometryUtil.circle(applier.getLocation().clone().add(new Vector(0, 0.8, 0)), numProjectiles, 1.5);
            if (missileLocations.size() < numProjectiles) {
                Heroes.log(Level.INFO, "AETHER MISSILES IS BROKEN DUE TO A CHANGE IN HEROES, YO");
                return;
            }
            for (int i = 0; i < numProjectiles; i++) {
                EffectManager effectManager = new EffectManager(plugin);
                SphereEffect missileVisual = new SphereEffect(effectManager);
                DynamicLocation dynamicLoc = new DynamicLocation(applier);
                Location missileLocation = missileLocations.get(i);
                dynamicLoc.addOffset(missileLocation.toVector().subtract(applier.getLocation().toVector()));
                missileVisual.setDynamicOrigin(dynamicLoc);
                missileVisual.iterations = (int) (getDuration() / 50) + projDurationTicks;
                missileVisual.radius = this.projectileRadius;
                missileVisual.particle = Particle.SWEEP_ATTACK;
                missileVisual.particles = 15;
                missileVisual.radiusIncrease = 0;
                effectManager.start(missileVisual);

                missileVisuals.add(new Pair<EffectManager, SphereEffect>(effectManager, missileVisual));
            }
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            int projectileLaunchDelay = SkillConfigManager.getUseSetting(hero, skill, "projectile-launch-delay-ticks", 3, false);

            for (int i = 0; i < numProjectiles; i++) {
                int finalI = i;
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        final Player player = hero.getPlayer();
                        if (player.isDead() || player.getHealth() <= 0) {
                            return;
                        }
                        Pair<EffectManager, SphereEffect> pair = missileVisuals.get(finalI);
                        SphereEffect missileVisual = pair.getRight();

                        Location eyeLocation = hero.getPlayer().getEyeLocation();
                        missileVisual.setLocation(eyeLocation.clone().add(eyeLocation.getDirection()));
                        AetherMissile missile = new AetherMissile(hero, skill, projectileRadius, pair.getLeft(), missileVisual);
                        missile.fireMissile();
                        eyeLocation.getWorld().playSound(eyeLocation, Sound.ENTITY_PLAYER_LEVELUP, 2F, 0.5F);
                    }
                }, projectileLaunchDelay * i);
            }
        }
    }

    interface MissileDeathCallback {
        void onMissileDeath(Missile missile);
    }

    private class AetherMissile extends Missile {

        private final EffectManager effectManager;
        private final SphereEffect visualEffect;
        private final Hero hero;
        private final Player player;
        private final Skill skill;

        private final int durationTicks;
        private final double projectileDamage;
        private final double damageIncreasePerHit;

        private double defaultSpeed;

        AetherMissile(Hero hero, Skill skill, double radius, EffectManager effectManager, SphereEffect visualEffect) {
            this.hero = hero;
            this.skill = skill;
            this.player = hero.getPlayer();
            this.effectManager = effectManager;
            this.visualEffect = visualEffect;

            double projectileSpeed = SkillConfigManager.getUseSetting(hero, skill, "projectile-velocity", 20.0, false);
            this.projectileDamage = SkillConfigManager.getUseSetting(hero, skill, "projectile-damage", 25.0, false);
            this.damageIncreasePerHit = SkillConfigManager.getUseSetting(hero, skill, "projectile-damage-increase-per-hit", 10.0, false);
            this.durationTicks = SkillConfigManager.getUseSetting(hero, skill, "projectile-max-duration", 2000, false) / 50;

            setNoGravity();
            setEntityDetectRadius(radius);
            setRemainingLife(this.durationTicks);

            Vector playerDirection = player.getEyeLocation().getDirection().normalize();
            Location missileLoc = visualEffect.getLocation().clone().setDirection(playerDirection);
            visualEffect.setLocation(missileLoc);

            this.setLocationAndSpeed(missileLoc, projectileSpeed);
        }

        private void updateVisualLocation() {
            this.visualEffect.setLocation(getLocation());
        }

        protected void onStart() {
            this.defaultSpeed = getVelocity().length();
            updateVisualLocation();
        }

        protected void onTick() {
            updateVisualLocation();
        }

        protected void onFinalTick() {
            effectManager.dispose();
        }

        protected boolean onCollideWithEntity(Entity entity) {
            if (!(entity instanceof LivingEntity) || entity.equals(player) || !damageCheck(player, (LivingEntity) entity))
                return false;
            return true;
        }

        protected void onEntityHit(Entity entity, Vector hitOrigin, Vector hitForce) {
            LivingEntity target = (LivingEntity) entity;
            if (!(target instanceof Player) || player.equals(target))
                return;

            Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
            // only works on party ?
            if (!(hero.getParty().isPartyMember(targetHero)))
                return;

            double damage = this.projectileDamage;
            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
            String effectName = getMultiHitEffectName(player);
            if (targetCT.hasEffect(effectName)) {
                MultiMissileHitEffect multiHitEffect = (MultiMissileHitEffect) targetCT.getEffect(effectName);
                damage+= this.damageIncreasePerHit * multiHitEffect.getHitCount();
                multiHitEffect.addHit();
            } else {
                targetCT.addEffect(new MultiMissileHitEffect(skill, effectName, player, 5000));
            }

            addSpellTarget(target, hero);
            HeroRegainHealthEvent event = new HeroRegainHealthEvent(targetHero, damage, skill, hero);
            plugin.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                hero.getPlayer().sendMessage("Unable to heal the target at this time!");
                return;
            }
            targetHero.heal(event.getDelta());
        }

        private String getMultiHitEffectName(Player player) {
            return player.getName() + "-AetherMissileMultiHit";
        }
    }

    private class MultiMissileHitEffect extends ExpirableEffect {
        private int hitCount = 1;

        MultiMissileHitEffect(Skill skill, String name, Player applier, long duration) {
            super(skill, name, applier, duration);
        }

        private int getHitCount() {
            return this.hitCount;
        }

        private void addHit() {
            this.hitCount++;
        }
    }
}