package com.herocraftonline.heroes.characters.skill.reborn.druid;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.tools.BasicMissile;
import com.herocraftonline.heroes.characters.skill.tools.Missile;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class SkillGust extends ActiveSkill {

    public SkillGust(Heroes plugin) {
        super(plugin, "Gust");
        setDescription("Summon a gust of wind in front of you. " +
                "The gust will $1deal $2 damage to any targets it hits$3");
        setUsage("/skill gust");
        setIdentifiers("skill gust");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_AIR, SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
    }

    public String getDescription(Hero hero) {
        boolean pierces = SkillConfigManager.getUseSetting(hero, this, "projectile-pierces-on-hit", false);
        String pierceText = pierces ? "pass through enemies and " : "";

        double knockbackPower = SkillConfigManager.getUseSetting(hero, this, "projectile-knockback-force", 2.0, false);
        String knockbackText = "";
        if (knockbackPower <= 0) {
            knockbackText = ".";
        } else {
            knockbackText = ", while knocking them back ";
            if (knockbackPower >= 2.0) {
                knockbackText+= "with exessive force.";
            } else if (knockbackPower >= 1.5) {
                knockbackText+= "by a significant amount.";
            } else if (knockbackPower >= 1.0) {
                knockbackText+= "by a decent amount.";
            } else {
                knockbackText += "slightly.";
            }
        }

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 75.0, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        return getDescription()
                .replace("$1", pierceText)
                .replace("$2", Util.decFormat.format(damage)
                .replace("$3", knockbackText));
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 40.0);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.0);
        config.set("projectile-size", 2.5);
        config.set("projectile-velocity", 35.0);
        config.set("projectile-max-ticks-lived", 20);
        config.set("projectile-block-collision-size", 0.35);
        config.set("projectile-gravity", 0.0);
        config.set("projectile-pierces-on-hit", true);
        config.set("projectile-knockback-force", 2.0);
        return config;
    }

    public SkillResult use(final Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        GustProjectile missile = new GustProjectile(this, hero);
        missile.fireMissile();

        return SkillResult.NORMAL;
    }

    class GustProjectile extends BasicMissile {
        private final double blockCollisionSizeSquared;
        private final boolean shouldPierce;
        private final double knockbackPower;
        private final double knockbackYPower;

        private double defaultSpeed;
        private List<LivingEntity> hitTargets = new ArrayList<LivingEntity>();

        GustProjectile(Skill skill, Hero hero) {
            super(plugin, skill, hero,
                    SkillConfigManager.getUseSetting(hero, skill, "projectile-size", 1.5, false),
                    Particle.SWEEP_ATTACK,
                    SkillConfigManager.getUseSetting(hero, skill, "projectile-velocity", 20.0, false),
                    SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 75.0, false)
            );

            setRemainingLife(SkillConfigManager.getUseSetting(hero, skill, "projectile-max-ticks-lived", 20, false));
            setGravity(SkillConfigManager.getUseSetting(hero, skill, "projectile-gravity", 0.0, false));
            double size = SkillConfigManager.getUseSetting(hero, skill, "projectile-block-collision-size", 0.35, false);
            this.blockCollisionSizeSquared = size * size;

            this.knockbackPower = SkillConfigManager.getUseSetting(hero, skill, "projectile-knockback-force", 2.0, false);
            this.knockbackYPower = SkillConfigManager.getUseSetting(hero, skill, "projectile-knockback-y-multiplier", 0.5, false);
            this.shouldPierce = SkillConfigManager.getUseSetting(hero, skill, "projectile-pierces-on-hit", false);
        }

        @Override
        protected boolean onCollideWithEntity(Entity entity) {
            if (shouldPierce)
                return false;
            return entity instanceof LivingEntity && !hero.isAlliedTo((LivingEntity) entity);
        }

        @Override
        protected boolean onCollideWithBlock(Block block, Vector point, BlockFace face) {
            return getLocation().distanceSquared(block.getLocation()) >= this.blockCollisionSizeSquared;
        }

        @Override
        protected void onEntityPassed(Entity entity, Vector passOrigin, Vector passForce) {
            if (!(entity instanceof LivingEntity) || hitTargets.contains(entity)) {
                return;
            }

            LivingEntity target = (LivingEntity) entity;
            if (!Skill.damageCheck(player, target))
                return;

            hitTargets.add(target);

            addSpellTarget(target, hero);
            damageEntity(target, player, damage, EntityDamageEvent.DamageCause.MAGIC, false);

            if (knockbackPower > 0.0) {
                target.setVelocity(passForce.normalize().multiply(new Vector(this.knockbackPower, this.knockbackYPower, this.knockbackPower)));
            }

            if (!shouldPierce)
                this.kill();
        }
    }
}
