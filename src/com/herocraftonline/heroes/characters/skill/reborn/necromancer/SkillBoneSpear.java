package com.herocraftonline.heroes.characters.skill.reborn.necromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.tools.Missile;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class SkillBoneSpear extends ActiveSkill {

    public SkillBoneSpear(Heroes plugin) {
        super(plugin, "BoneSpear");
        setDescription("Launch a magical spear of bone in front of you. " +
                "The spear will $1deal $2 damage to any targets it hits.");
        setUsage("/skill bonespear");
        setIdentifiers("skill bonespear");
        setArgumentRange(0, 0);
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
    }

    public String getDescription(Hero hero) {
        boolean pierces = SkillConfigManager.getUseSetting(hero, this, "projectile-pierces-on-hit", false);
        String pierceText = pierces ? "pierce enemies and " : "";

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 75.0, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        return getDescription()
                .replace("$1", pierceText)
                .replace("$2", Util.decFormat.format(damage));
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 75.0);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.0);
        config.set("projectile-size", 1.5);
        config.set("projectile-velocity", 40.0);
        config.set("projectile-ticks-lived", 30);
        config.set("projectile-gravity", 0.0);
        config.set("projectile-pierces-on-hit", true);
        config.set("projectile-knocks-back-on-hit", false);
        config.set("projectile-effect-display-servertick-rate", 10);
        return config;
    }

    public SkillResult use(final Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        BoneSpearProjectile missile = new BoneSpearProjectile(this, hero);
        missile.fireMissile();

        return SkillResult.NORMAL;
    }

    class BoneSpearProjectile extends Missile {
        private final Hero hero;
        private final Player player;

        private final double damage;
        private final int visualTickRate;
        private final boolean knockBackOnHit;
        private final boolean shouldPierce;

        private double defaultSpeed;
        private List<LivingEntity> hitTargets = new ArrayList<LivingEntity>();

        BoneSpearProjectile(Skill skill, Hero hero) {
            this.hero = hero;
            this.player = hero.getPlayer();

            setRemainingLife(SkillConfigManager.getUseSetting(hero, skill, "projectile-max-ticks-lived", 30, false));
            setGravity(SkillConfigManager.getUseSetting(hero, skill, "projectile-gravity", 0.0, false));
            setEntityDetectRadius(SkillConfigManager.getUseSetting(hero, skill, "projectile-size", 1.5, false));
            double projectileSpeed = SkillConfigManager.getUseSetting(hero, skill, "projectile-velocity", 20.0, false);

            this.knockBackOnHit = SkillConfigManager.getUseSetting(hero, skill, "projectile-knocks-back-on-hit", false);
            this.shouldPierce = SkillConfigManager.getUseSetting(hero, skill, "projectile-pierces-on-hit", false);
            this.visualTickRate = SkillConfigManager.getUseSetting(hero, skill, "projectile-effect-display-servertick-rate", 10, false);
            this.damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 75.0, false);

            Vector playerDirection = player.getEyeLocation().getDirection().normalize();
            Location missileLoc = player.getLocation().clone().setDirection(playerDirection);

            this.setLocationAndSpeed(missileLoc, projectileSpeed);
        }

        private void updateVisualLocation() {
            FireworkEffect firework = FireworkEffect.builder()
                    .flicker(false)
                    .trail(false)
                    .withColor(Color.BLUE)
                    .withColor(Color.BLUE)
                    .withColor(Color.WHITE)
                    .with(FireworkEffect.Type.BURST)
                    .build();
            VisualEffect.playInstantFirework(firework, getLocation());
        }

        @Override
        protected void onStart() {
            this.defaultSpeed = getVelocity().length();
            updateVisualLocation();
        }

        @Override
        protected void onTick() {
            if (getTicksLived() % this.visualTickRate == 0)
                updateVisualLocation();
        }

        @Override
        protected void onFinalTick() {
            updateVisualLocation();
        }

        @Override
        protected boolean onCollideWithEntity(Entity entity) {
            if (shouldPierce)
                return false;
            return entity instanceof LivingEntity && !hero.isAlliedTo((LivingEntity) entity);
        }

        @Override
        protected void onEntityPassed(Entity entity, Vector passOrigin, Vector passForce) {
            if (!(entity instanceof LivingEntity) || hitTargets.contains(entity)) {
                return;
            }

            LivingEntity target = (LivingEntity) entity;
            if (!Skill.damageCheck(player, target))
                return;

            addSpellTarget(target, hero);
            damageEntity(target, player, damage, EntityDamageEvent.DamageCause.MAGIC, knockBackOnHit);
            hitTargets.add(target);

            if (!shouldPierce)
                this.kill();
        }
    }
}
