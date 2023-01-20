package com.herocraftonline.heroes.characters.skill.general;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import com.herocraftonline.heroes.characters.skill.tools.BasicDamageMissile;
import com.herocraftonline.heroes.characters.skill.tools.BasicMissile;
import com.herocraftonline.heroes.libs.slikey.effectlib.effect.LineEffect;
import com.herocraftonline.heroes.libs.slikey.effectlib.util.DynamicLocation;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

public class SkillCulling extends ActiveSkill {

    public SkillCulling(final Heroes plugin) {
        super(plugin, "Culling");
        setDescription("Spear your target, pulling him back towards you and dealing $1 physical damage");
        setUsage("/skill culling");
        setArgumentRange(0, 0);
        setIdentifiers("skill culling");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.INTERRUPTING);
    }

    @Override
    public String getDescription(final Hero hero) {
        final double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(damage));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.ON_INTERRUPT_FORCE_COOLDOWN.node(), 1500);
        config.set(SkillSetting.DAMAGE.node(), 45);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 0.0);
        config.set(BasicMissile.PROJECTILE_SIZE_NODE, 0.25);
        config.set(BasicMissile.PROJECTILE_VELOCITY_NODE, 35.0);
        config.set(BasicMissile.PROJECTILE_GRAVITY_NODE, 2.5);
        config.set(BasicMissile.PROJECTILE_DURATION_TICKS_NODE, 12);
        config.set("vertical-power", 0.4);
        config.set("horizontal-power-multiplier", 1.0);
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
        final double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);

        if (damage > 0) {
            addSpellTarget(target, hero);
            damageEntity(target, player, damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK, 0.0f);
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
            final long interruptCd = SkillConfigManager.getUseSetting(hero, this, SkillSetting.ON_INTERRUPT_FORCE_COOLDOWN, 1500, false);
            targetHero.interruptDelayedSkill(interruptCd);
        }

        final double vPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", 0.4, false);
        pushTargetUpwards(hero, target, vPower);
        pullTarget(hero, target, vPower, locDiff);
        playLingeringEffect(player, target);
    }

    private void pullTarget(final Hero hero, final LivingEntity target, final double vPower, final Vector locDiff) {
        final long delay = SkillConfigManager.getUseSetting(hero, this, "pull-delay-ticks", 4, false);

        final double finalHPower = SkillConfigManager.getScaledUseSettingDouble(hero, this, "horizontal-power-multiplier", false);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            final Vector pushVector = locDiff.setY(0).multiply(finalHPower).setY(vPower);
            target.setVelocity(pushVector);
        }, delay);
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

    private LineEffect getBaseSpearVisual() {
        final LineEffect effect = new LineEffect(effectLib);
        effect.particle = Particle.CRIT;
        effect.particles = 5;
        effect.iterations = 600;
        return effect;
    }

    private class SpearProjectile extends BasicDamageMissile {
        SpearProjectile(final Heroes plugin, final Skill skill, final Hero hero) {
            super(plugin, skill, hero);

            this.visualEffect = getSpearVisual(player, getLocation());
        }

        @Override
        protected void onTick() {
            this.visualEffect.setTargetLocation(getLocation());
        }

        @Override
        protected void onValidTargetFound(final LivingEntity target, final Vector origin, final Vector force) {
            damageEnemy(hero, target, player);
            spearEnemy(hero, player, target);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8F, 1.0F);
        }
    }
}
