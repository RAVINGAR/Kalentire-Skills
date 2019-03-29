package com.herocraftonline.heroes.characters.skill.reborn.arcanist;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterManager;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.tools.Missile;
import com.herocraftonline.heroes.util.Pair;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.SphereEffect;
import de.slikey.effectlib.util.DynamicLocation;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class SkillAetherMissiles extends ActiveSkill {

    private static final Color FIRE_ORANGE = Color.fromRGB(226, 88, 34);
    private static final Color FIRE_RED = Color.fromRGB(236, 60, 30);
    private static String toggleableEffectName = "FloatingAetherMissiles";

    public SkillAetherMissiles(Heroes plugin) {
        super(plugin, "AetherMissiles");
        setDescription("TBD");
        setUsage("/skill aethermissiles");
        setArgumentRange(0, 0);
        setIdentifiers("skill aethermissiles");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);

        setToggleableEffectName(toggleableEffectName);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 80, false);
        return getDescription().replace("%1", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DURATION.node(), 15000);
        config.set("projectile-damage", 25.0);
        config.set("projectile-damage-increase-per-hit", 10.0);
        config.set("projectile-velocity", 20.0);
        config.set("projectile-max-duration", 1500);
        config.set("projectile-radius", 0.15);
        config.set("projectile-launch-delay-ticks", 3);
        config.set("num-projectiles", 4);

        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 15000, false);
        hero.addEffect(new AetherMissilesEffect(this, player, duration));
        return SkillResult.NORMAL;
    }

    class AetherMissilesEffect extends ExpirableEffect {
        private int numProjectiles;
        private double projectileRadius;
        private List<Pair<EffectManager, SphereEffect>> missileVisuals = new ArrayList<Pair<EffectManager, SphereEffect>>();

        public AetherMissilesEffect(Skill skill, Player applier, long duration) {
            super(skill, toggleableEffectName, applier, duration);

            this.types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            this.numProjectiles = SkillConfigManager.getUseSetting(hero, skill, "num-projectiles", 4, false);
            this.projectileRadius = SkillConfigManager.getUseSetting(hero, skill, "projectile-radius", 0.15, false);
            int projDurationTicks = SkillConfigManager.getUseSetting(hero, skill, "projectile-max-duration", 2000, false) / 50;

            for (int i = 0; i < numProjectiles; i++) {
                EffectManager effectManager = new EffectManager(plugin);
                SphereEffect missileVisual = new SphereEffect(effectManager);
                DynamicLocation dynamicLoc = new DynamicLocation(applier);
//                missileVisual.setLocation(applier.getEyeLocation());
//                missileVisual.particleOffsetY = 0.8f;
                missileVisual.offset = new Vector(0, 0.8, 0);
                missileVisual.setDynamicOrigin(dynamicLoc);
                //missileVisual.disappearWithOriginEntity = true;
                missileVisual.iterations = (int) (getDuration() / 50) + projDurationTicks;
                missileVisual.radius = this.projectileRadius;
                missileVisual.particle = Particle.SPELL_WITCH;
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
                        Pair<EffectManager, SphereEffect> pair = missileVisuals.get(finalI);
                        AetherMissile missile = new AetherMissile(hero, skill, projectileRadius, pair.getLeft(), pair.getRight());
                        missile.fireMissile();
                    }
                }, projectileLaunchDelay * i);
            }
        }
    }

    interface MissileDeathCallback {
        void onMissileDeath(Missile missile);
    }

    class AetherMissile extends Missile {

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
            damageEntity(target, player, damage, EntityDamageEvent.DamageCause.MAGIC);
        }

        private String getMultiHitEffectName(Player player) {
            return player.getName() + "-AetherMissileMultiHit";
        }
    }

    private class MultiMissileHitEffect extends ExpirableEffect {

        private int hitCount = 1;

        public MultiMissileHitEffect(Skill skill, String name, Player applier, long duration) {
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