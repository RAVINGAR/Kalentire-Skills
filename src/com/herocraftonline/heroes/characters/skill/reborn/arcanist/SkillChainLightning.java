package com.herocraftonline.heroes.characters.skill.reborn.arcanist;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.PeriodicEffect;
import com.herocraftonline.heroes.characters.skill.*;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.LineEffect;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class SkillChainLightning extends TargettedSkill {
    private static final Random random = new Random(System.currentTimeMillis());
    private static Color DEEP_ELECTRIC_BLUE = Color.fromRGB(44, 117, 255);
    private static Color ELECTRIC_BLUE = Color.fromRGB(125, 249, 255);

    public SkillChainLightning(Heroes plugin) {
        super(plugin, "ChainLightning");
        setDescription("Conjure a bolt of lightning that strikes from you to your target, dealing $1 damage. "
                + "The bolt will strike in a chain to up to $2 times and has a maximum range of $3 blocks.");
        setUsage("/skill chainlightning");
        setArgumentRange(0, 0);
        setIdentifiers("skill chainlightning");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_LIGHTNING,
                SkillType.AREA_OF_EFFECT, SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 100, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.0, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        int maxTargets = SkillConfigManager.getUseSetting(hero, this, "max-targets", 5, false);
        int maxChainDistance = SkillConfigManager.getUseSetting(hero, this, "max-chain-distance", 5, false);

        return getDescription().replace("$1", damage + "").replace("$2", maxTargets + "").replace("$2", maxChainDistance + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 8);
        config.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), 0.0);
        config.set(SkillSetting.DAMAGE.node(), 75);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.0);
        config.set("max-targets", 5);
        config.set("max-chain-distance", 6);
        config.set("strike-period", 250);
        config.set("lightning-volume", 0.7F);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        int strikePeriod = SkillConfigManager.getUseSetting(hero, this, "strike-period", 250, false);
        ChainLightningEffect effect = new ChainLightningEffect(this, player, strikePeriod, target);

        // This shouldn't ever happen but just in case...
        if (hero.hasEffect(effect.getName()))
            hero.removeEffect(hero.getEffect(effect.getName()));

        hero.addEffect(effect);

        return SkillResult.NORMAL;
    }

    private class ChainLightningEffect extends PeriodicEffect {
        private final EffectManager effectManager;
        private final ArrayList<LivingEntity> hitTargets;
        private int maxTargets = 0;
        private int maxChainDistance;
        private double damage;
        private float lightningVolume;

        private LivingEntity lastTarget = null;
        private LivingEntity initialTarget;
        private int hitCount = 0;

        public ChainLightningEffect(Skill skill, Player player, long period, LivingEntity target) {
            super(skill, "ChainLightningDelayedCast", player, period, null, null);

            this.effectManager = new EffectManager(plugin);
            this.hitTargets = new ArrayList<LivingEntity>();
            this.initialTarget = target;
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            double baseDamage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 100, false);
            double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.0, false);
            this.damage = baseDamage + (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

            this.maxTargets = SkillConfigManager.getUseSetting(hero, skill, "max-targets", 5, false);
            this.maxChainDistance = SkillConfigManager.getUseSetting(hero, skill, "max-chain-distance", 5, false);
            this.lightningVolume = (float) SkillConfigManager.getUseSetting(hero, skill, "lightning-volume", 0.7F, false);

            addSpellTarget(initialTarget, hero);
            damageEntity(initialTarget, applier, damage, DamageCause.MAGIC, false);

            playVisualEffects(applier, initialTarget);

            hitTargets.add(initialTarget);
            lastTarget = initialTarget;
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            hitTargets.clear();
            lastTarget = null;
            effectManager.dispose();
        }

        private void playVisualEffects(LivingEntity from, LivingEntity to) {
            to.getWorld().playSound(to.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, lightningVolume, 1.8F);

            double randomX = ThreadLocalRandom.current().nextDouble(-0.035, 0.035);
            double randomZ = ThreadLocalRandom.current().nextDouble(-0.035, 0.035);
            Vector verticleOffset = new Vector(0, 0.15, 0);

            LineEffect top = getBaseLightningEffect(randomX, randomZ);
            top.color = DEEP_ELECTRIC_BLUE;
            top.setLocation(from.getEyeLocation().add(verticleOffset));
            top.setTargetLocation(to.getEyeLocation().subtract(verticleOffset));

            LineEffect middle = getBaseLightningEffect(randomX, randomZ);
            middle.color = ELECTRIC_BLUE;
            middle.setLocation(from.getEyeLocation());
            middle.setTargetLocation(to.getEyeLocation());

            LineEffect bottom = getBaseLightningEffect(randomX, randomZ);
            bottom.color = DEEP_ELECTRIC_BLUE;
            bottom.setLocation(from.getEyeLocation().subtract(verticleOffset));
            bottom.setTargetLocation(to.getEyeLocation().subtract(verticleOffset));

            effectManager.start(top);
            effectManager.start(middle);
            effectManager.start(bottom);
        }

        @NotNull
        private LineEffect getBaseLightningEffect(double randomX, double randomZ) {
            LineEffect lightning = new LineEffect(effectManager);
            lightning.particle = Particle.REDSTONE;
            lightning.isZigZag = true;
            lightning.zigZags = 7;
            lightning.particles = 50;
            lightning.zigZagOffset = new Vector(randomX, 0.05, randomZ);
            lightning.iterations = 4;
            return lightning;
        }

        @Override
        public void tickHero(Hero hero) {
            super.tickHero(hero);

            if (lastTarget == null || maxTargets == 0 || hitCount >= maxTargets) {
                hero.removeEffect(this);
                return;
            }

            boolean hitTarget = false;
            List<Entity> entities = lastTarget.getNearbyEntities(maxChainDistance, 3, maxChainDistance);
            for (Entity entity : entities) {
                if (!(entity instanceof LivingEntity) || hitTargets.contains(entity))
                    continue;

                LivingEntity newTarget = (LivingEntity) entity;
                if (!damageCheck(applier, newTarget))
                    continue;

                hitTargets.add(newTarget);
                addSpellTarget(newTarget, hero);
                damageEntity(newTarget, applier, damage, DamageCause.MAGIC);

                playVisualEffects(lastTarget, newTarget);

                lastTarget = newTarget;
                hitCount++;
                hitTarget = true;
                break;
            }

            if (!hitTarget) {
                lastTarget = null;
                hero.removeEffect(this);
            }
        }
    }
}
