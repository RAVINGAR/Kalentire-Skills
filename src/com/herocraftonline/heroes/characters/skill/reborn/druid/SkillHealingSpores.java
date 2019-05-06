package com.herocraftonline.heroes.characters.skill.reborn.druid;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
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
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;

public class SkillHealingSpores extends ActiveSkill {
    private static String sporeEffectName = "FloatingHealingSpores";

    public SkillHealingSpores(Heroes plugin) {
        super(plugin, "HealingSpores");
        setDescription("Summon $1 healing spores that will float and remain inactive around the caster for up to $2 seconds. " +
                "If you perform a melee attack with your staff it will unleash a single stored spore. " +
                "Spore pass through enemies and will heal $3 health upon hitting a friendly target.");
        setUsage("/skill healingspores");
        setIdentifiers("skill healingspores");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_EARTH, SkillType.SILENCEABLE, SkillType.HEALING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int numProjectiles = SkillConfigManager.getUseSetting(hero, this, "num-projectiles", 4, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 15000, false);
        double heal = SkillConfigManager.getUseSetting(hero, this, "projectile-heal", 25.0, false);

        return getDescription()
                .replace("$1", numProjectiles + "")
                .replace("$2", Util.decFormat.format(duration / 1000.0))
                .replace("$3", Util.decFormat.format(heal));

    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DURATION.node(), 15000);
        config.set("projectile-heal", 25.0);
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

        hero.addEffect(new HealingSporesEffect(this, player, duration));
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
            if (!hero.hasEffect(sporeEffectName))
                return;
//
            if (hero.hasEffect(sporeEffectName)) {
                HealingSporesEffect effect = (HealingSporesEffect) hero.getEffect(sporeEffectName);
                effect.launchSpore(hero);

            }

        }
    }

    private class HealingSporesEffect extends ExpirableEffect {
        private int firedProjectiles = 0;
        private int maxProjectiles;
        private double projectileRadius;
        private List<Pair<EffectManager, SphereEffect>> missileVisuals = new ArrayList<Pair<EffectManager, SphereEffect>>();
        HealingSporesEffect(Skill skill, Player applier, long duration) {
            super(skill, sporeEffectName, applier, duration);
            this.types.add(EffectType.HEALING);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            this.maxProjectiles = SkillConfigManager.getUseSetting(hero, skill, "num-projectiles", 4, false);
            this.projectileRadius = SkillConfigManager.getUseSetting(hero, skill, "projectile-radius", 0.15, false);
            int projDurationTicks = SkillConfigManager.getUseSetting(hero, skill, "projectile-max-ticks-lived", 30, false);

            List<Location> missileLocations = GeometryUtil.circle(applier.getLocation().clone().add(new Vector(0, 0.8, 0)), maxProjectiles, 1.5);
            if (missileLocations.size() < maxProjectiles) {
                Heroes.log(Level.INFO, "HEALING SPORES IS BROKEN DUE TO A CHANGE IN HEROES, YO");
                return;
            }

            for (int i = 0; i < maxProjectiles; i++) {
                EffectManager effectManager = new EffectManager(plugin);
                SphereEffect missileVisual = new SphereEffect(effectManager);
                DynamicLocation dynamicLoc = new DynamicLocation(applier);
                Location missileLocation = missileLocations.get(i);
                dynamicLoc.addOffset(missileLocation.toVector().subtract(applier.getLocation().toVector()));
                missileVisual.setDynamicOrigin(dynamicLoc);
                missileVisual.iterations = (int) (getDuration() / 50) + projDurationTicks;
                missileVisual.radius = this.projectileRadius;
                missileVisual.particle = Particle.VILLAGER_HAPPY;
                missileVisual.particles = 15;
                missileVisual.radiusIncrease = 0;
                effectManager.start(missileVisual);

                missileVisuals.add(new Pair<EffectManager, SphereEffect>(effectManager, missileVisual));
            }
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            for (Pair<EffectManager, SphereEffect> pair : missileVisuals) {
                pair.getLeft().dispose();
            }
            missileVisuals.clear();
        }

        public void launchSpore(Hero hero) {
            final Player player = hero.getPlayer();

            Pair<EffectManager, SphereEffect> pair = missileVisuals.get(firedProjectiles);
            SphereEffect missileVisual = pair.getRight();
            Location eyeLocation = hero.getPlayer().getEyeLocation();
            Vector eyeOffset = eyeLocation.getDirection().add(new Vector(0, -1, 0));
            missileVisual.setLocation(eyeLocation.clone().add(eyeOffset));
            HealingSpore spore = new HealingSpore(hero, skill, projectileRadius, pair.getLeft(), missileVisual);
            spore.fireMissile();

            firedProjectiles++;
            if (firedProjectiles == maxProjectiles) {
               removeFromHero(hero);
            }


        }
    }

    interface MissileDeathCallback {
        void onMissileDeath(Missile missile);
    }

    private class HealingSpore extends Missile {

        private final EffectManager effectManager;
        private final SphereEffect visualEffect;
        private final Hero hero;
        private final Player player;
        private final Skill skill;

        private final int durationTicks;
        private final double projectileHeal;

        private double defaultSpeed;

        HealingSpore(Hero hero, Skill skill, double radius, EffectManager effectManager, SphereEffect visualEffect) {
            this.hero = hero;
            this.skill = skill;
            this.player = hero.getPlayer();
            this.effectManager = effectManager;
            this.visualEffect = visualEffect;

            double projectileSpeed = SkillConfigManager.getUseSetting(hero, skill, "projectile-velocity", 20.0, false);
            this.projectileHeal = SkillConfigManager.getUseSetting(hero, skill, "projectile-heal", 25.0, false);
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
            return entity instanceof LivingEntity && !entity.equals(player) && hero.isAlliedTo((LivingEntity) entity);
        }

        protected void onEntityHit(Entity entity, Vector hitOrigin, Vector hitForce) {
            LivingEntity target = (LivingEntity) entity;
            Hero targetHero = plugin.getCharacterManager().getHero((Player) target);

            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
            targetHero.tryHeal(hero, skill, projectileHeal);
        }
    }
}