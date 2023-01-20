package com.herocraftonline.heroes.characters.skill.reborn;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import com.herocraftonline.heroes.characters.skill.tools.BasicDamageMissile;
import com.herocraftonline.heroes.libs.slikey.effectlib.effect.LineEffect;
import com.herocraftonline.heroes.libs.slikey.effectlib.util.DynamicLocation;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;

public class SkillSpear extends ActiveSkill {

    public SkillSpear(final Heroes plugin) {
        super(plugin, "Spear");
        setDescription("Spear your target, pulling him back towards you and dealing $1 physical damage");
        setUsage("/skill spear");
        setArgumentRange(0, 0);
        setIdentifiers("skill spear");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.INTERRUPTING);
    }

    @Override
    public String getDescription(final Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 30, false);
        final double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.0, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        return getDescription().replace("$1", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 45);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 0.0);
        config.set("projectile-size", 0.25);
        config.set("projectile-velocity", 35.0);
        config.set("projectile-gravity", 2.5);
        config.set("projectile-max-ticks-lived", 12);
        config.set("vertical-power", 0.4);
        config.set("horizontal-power-multiplier", 1.5);
        config.set("horizontal-power-increase-per-strength", 0.0);
        config.set("pull-delay-ticks", 4);
        config.set("ncp-exemption-duration", 0);
        return config;
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        final SpearProjectile missile = new SpearProjectile(plugin, this, hero);
        missile.fireMissile();

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0F, 0.7F);

        return SkillResult.NORMAL;
    }

    private void damageEnemy(final Hero hero, final LivingEntity target, final Player player) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50.0, false);
        final double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.6, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        if (damage > 0) {
            addSpellTarget(target, hero);
            damageEntity(target, player, damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK, false);
        }
    }

    private void spearEnemy(final Hero hero, final Player player, final LivingEntity target) {
        final boolean shouldWeaken = shouldWeaken(target.getLocation());

        final Location playerLoc = player.getLocation();
        final Location targetLoc = target.getLocation();

        final Vector locDiff = playerLoc.toVector().subtract(targetLoc.toVector());
        if (shouldWeaken) {
            locDiff.multiply(0.75);
        }

        // Manually try to interrupt since we're doing custom projectile stuff
        if (target instanceof Player) {
            final Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
            targetHero.interruptDelayedSkill();
        }

        final double vPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", 0.4, false);
        pushTargetUpwards(hero, target, vPower);
        pullTarget(hero, target, vPower, locDiff);
        playLingeringEffect(player, target);
    }

    private void pullTarget(final Hero hero, final LivingEntity target, final double vPower, final Vector locDiff) {
        final double delay = SkillConfigManager.getUseSetting(hero, this, "pull-delay-ticks", 4, false);

        double hPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power-multiplier", 0.5, false);
        final double hPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "horizontal-power-increase-per-strength", 0.0, false);
        hPower += hPowerIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        final double finalHPower = hPower;
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            final Vector pushVector = locDiff.setY(0).multiply(finalHPower).setY(vPower);
            target.setVelocity(pushVector);
        }, 4);
    }

    private void pushTargetUpwards(final Hero hero, final LivingEntity target, final double vPower) {
        final Vector pushUpVector = new Vector(0, vPower, 0);

        final long exemptionDuration = SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 1000, false);
        if (exemptionDuration > 0) {
            NCPUtils.applyExemptions(target, () -> target.setVelocity(pushUpVector), Lists.newArrayList("MOVING"), exemptionDuration);
        } else {
            target.setVelocity(pushUpVector);
        }
    }

    private boolean shouldWeaken(final Location targetLoc) {
        final Material mat = targetLoc.getBlock().getRelative(BlockFace.DOWN).getType();
        switch (mat) {
            case WATER:
            case LAVA:
            case SOUL_SAND:
                return true;
            default:
                return false;
        }
    }

    private void playLingeringEffect(final Player player, final LivingEntity target) {
        final LineEffect effect = getSpearVisual(player, target);
        effect.period = 1;
        effect.iterations = 10;
        effectLib.start(effect);
    }

    private LineEffect getSpearVisual(final Player owner, final LivingEntity target) {
        final LineEffect effect = getBaseSpearVisual();

        final DynamicLocation dynamicOwnerLoc = new DynamicLocation(owner);
        effect.setDynamicOrigin(dynamicOwnerLoc);

        final DynamicLocation dynamicTargetLoc = new DynamicLocation(target);
        dynamicOwnerLoc.addOffset(new Vector(0, -0.5, 0));
        effect.setDynamicTarget(dynamicTargetLoc);

        return effect;
    }

    private LineEffect getSpearVisual(final Player owner, final Location targetLoc) {
        final LineEffect effect = getBaseSpearVisual();

        final DynamicLocation dynamicOwnerLoc = new DynamicLocation(owner);
        effect.setDynamicOrigin(dynamicOwnerLoc);
        effect.setLocation(targetLoc);

        return effect;
    }

    @Nonnull
    private LineEffect getBaseSpearVisual() {
        final LineEffect effect = new LineEffect(effectLib);
        effect.particle = Particle.CRIT;
        effect.particles = 5;
        effect.iterations = 9999;
        return effect;
    }

    private class SpearProjectile extends BasicDamageMissile {
        SpearProjectile(final Plugin plugin, final Skill skill, final Hero hero) {
            super((Heroes) plugin, skill, hero, Particle.CRIT, Color.OLIVE, EntityDamageEvent.DamageCause.MAGIC);

            setRemainingLife(SkillConfigManager.getUseSetting(hero, skill, "projectile-max-ticks-lived", 5, false));
            setGravity(SkillConfigManager.getUseSetting(hero, skill, "projectile-gravity", 2.5, false));
            this.damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 50.0, false);

            this.visualEffect = getSpearVisual(player, getLocation());
        }

        @Override
        protected void onTick() {
            this.visualEffect.setTargetLocation(getLocation());
        }

        @Override
        protected void onEntityHit(final Entity entity, final Vector hitOrigin, final Vector hitForce) {
            if (!(entity instanceof LivingEntity)) {
                return;
            }

            final LivingEntity target = (LivingEntity) entity;
            if (!damageCheck(this.player, target)) {
                return;
            }

            damageEnemy(hero, target, player);
            spearEnemy(hero, player, target);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8F, 1.0F);
        }
    }
}
