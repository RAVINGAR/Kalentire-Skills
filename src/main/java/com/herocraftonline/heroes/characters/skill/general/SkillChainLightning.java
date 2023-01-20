package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.PeriodicEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.libs.slikey.effectlib.effect.LineEffect;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class SkillChainLightning extends TargettedSkill {
    private static final Color DEEP_ELECTRIC_BLUE = Color.fromRGB(44, 117, 255);
    private static final Color ELECTRIC_BLUE = Color.fromRGB(125, 249, 255);

    public SkillChainLightning(final Heroes plugin) {
        super(plugin, "ChainLightning");
        setDescription("Conjure a bolt of lightning that strikes from you to your target, dealing $1 damage. " +
                "The bolt will strike in a chain to up to $2 times and has a maximum range of $3 blocks. " +
                "The same target cannot be hit multiple times. Each chain bounce does $4% less damage.");
        setUsage("/skill chainlightning");
        setArgumentRange(0, 0);
        setIdentifiers("skill chainlightning");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_LIGHTNING,
                SkillType.AREA_OF_EFFECT, SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(final Hero hero) {
        final double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);

        final int maxTargets = SkillConfigManager.getUseSetting(hero, this, "max-targets", 5, false);
        final int maxChainDistance = SkillConfigManager.getUseSetting(hero, this, "max-chain-distance", 5, false);
        final double damageReductionPercentPerJump = SkillConfigManager.getUseSetting(hero, this, "damage-reduction-percent-per-jump", 0.1, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(damage))
                .replace("$2", maxTargets + "")
                .replace("$3", maxChainDistance + "")
                .replace("$4", Util.decFormat.format(damageReductionPercentPerJump * 100));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
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
    public SkillResult use(final Hero hero, final LivingEntity target, final String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        final int strikePeriod = SkillConfigManager.getUseSetting(hero, this, "strike-period", 250, false);
        final ChainLightningEffect effect = new ChainLightningEffect(this, player, strikePeriod, target);

        // This shouldn't ever happen but just in case...
        if (hero.hasEffect(effect.getName())) {
            hero.removeEffect(hero.getEffect(effect.getName()));
        }

        hero.addEffect(effect);

        return SkillResult.NORMAL;
    }

    private class ChainLightningEffect extends PeriodicEffect {
        private final ArrayList<LivingEntity> hitTargets;
        private final LivingEntity initialTarget;
        private int maxTargets = 0;
        private int maxChainDistance;
        private double damage;
        private double damageReductionPercentPerJump;
        private float lightningVolume;
        private LivingEntity lastTarget = null;
        private int jumpCount = 0;

        public ChainLightningEffect(final Skill skill, final Player player, final long period, final LivingEntity target) {
            super(skill, "ChainLightningDelayedCast", player, period, null, null);
            this.hitTargets = new ArrayList<>();
            this.initialTarget = target;
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);

            this.damage = SkillConfigManager.getScaledUseSettingDouble(hero, skill, SkillSetting.DAMAGE, false);

            this.maxTargets = SkillConfigManager.getUseSetting(hero, skill, "max-targets", 5, false);
            this.maxChainDistance = SkillConfigManager.getUseSetting(hero, skill, "max-chain-distance", 5, false);
            this.damageReductionPercentPerJump = SkillConfigManager.getUseSetting(hero, skill, "damage-reduction-percent-per-jump", 0.1, false);
            this.lightningVolume = (float) SkillConfigManager.getUseSetting(hero, skill, "lightning-volume", 0.7F, false);

            addSpellTarget(initialTarget, hero);
            damageEntity(initialTarget, applier, damage, DamageCause.MAGIC, 0.0f);

            playVisualEffects(applier, initialTarget);

            jumpCount++;
            hitTargets.add(initialTarget);
            lastTarget = initialTarget;
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);

            hitTargets.clear();
            lastTarget = null;
        }

        private void playVisualEffects(final LivingEntity from, final LivingEntity to) {
            to.getWorld().playSound(to.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, lightningVolume, 1.8F);

            final double randomX = ThreadLocalRandom.current().nextDouble(-0.035, 0.035);
            final double randomZ = ThreadLocalRandom.current().nextDouble(-0.035, 0.035);
            final Vector verticleOffset = new Vector(0, 0.15, 0);

            final LineEffect top = getBaseLightningEffect(randomX, randomZ);
            top.color = DEEP_ELECTRIC_BLUE;
            top.setLocation(from.getEyeLocation().add(verticleOffset));
            top.setTargetLocation(to.getEyeLocation().subtract(verticleOffset));

            final LineEffect middle = getBaseLightningEffect(randomX, randomZ);
            middle.color = ELECTRIC_BLUE;
            middle.setLocation(from.getEyeLocation());
            middle.setTargetLocation(to.getEyeLocation());

            final LineEffect bottom = getBaseLightningEffect(randomX, randomZ);
            bottom.color = DEEP_ELECTRIC_BLUE;
            bottom.setLocation(from.getEyeLocation().subtract(verticleOffset));
            bottom.setTargetLocation(to.getEyeLocation().subtract(verticleOffset));

            effectLib.start(top);
            effectLib.start(middle);
            effectLib.start(bottom);
        }

        private LineEffect getBaseLightningEffect(final double randomX, final double randomZ) {
            final LineEffect lightning = new LineEffect(effectLib);
            lightning.offset = new Vector(0, -0.5, 0);
            lightning.particle = Particle.REDSTONE;
            lightning.isZigZag = true;
            lightning.zigZags = 7;
            lightning.particles = 50;
            lightning.zigZagOffset = new Vector(randomX, 0.05, randomZ);
            lightning.iterations = 4;
            return lightning;
        }

        @Override
        public void tickHero(final Hero hero) {
            super.tickHero(hero);

            final double damageThisJump = damage - (damage * (damageReductionPercentPerJump * jumpCount));
            if (lastTarget == null || maxTargets == 0 || jumpCount >= maxTargets || damageThisJump <= 0.0) {
                hero.removeEffect(this);
                return;
            }

            boolean hitTarget = false;
            final List<Entity> entities = lastTarget.getNearbyEntities(maxChainDistance, 3, maxChainDistance);
            for (final Entity entity : entities) {
                if (!(entity instanceof LivingEntity) || hitTargets.contains(entity)) {
                    continue;
                }

                final LivingEntity newTarget = (LivingEntity) entity;
                if (!damageCheck(applier, newTarget)) {
                    continue;
                }

                hitTargets.add(newTarget);
                addSpellTarget(newTarget, hero);
                damageEntity(newTarget, applier, damageThisJump, DamageCause.MAGIC, 0.0f);

                playVisualEffects(lastTarget, newTarget);

                lastTarget = newTarget;
                jumpCount++;
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
