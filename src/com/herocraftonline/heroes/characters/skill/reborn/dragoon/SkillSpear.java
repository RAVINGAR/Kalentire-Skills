package com.herocraftonline.heroes.characters.skill.reborn.dragoon;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.ncp.NCPFunction;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import com.herocraftonline.heroes.characters.skill.tools.BasicMissile;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.LineEffect;
import de.slikey.effectlib.util.DynamicLocation;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public class SkillSpear extends ActiveSkill {

    public SkillSpear(Heroes plugin) {
        super(plugin, "Spear");
        setDescription("Spear your target, pulling him back towards you and dealing $1 physical damage");
        setUsage("/skill spear");
        setArgumentRange(0, 0);
        setIdentifiers("skill spear");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.INTERRUPTING);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 30, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.0, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        return getDescription().replace("$1", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.ON_INTERRUPT_FORCE_COOLDOWN.node(), 1500);
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
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        double projSize = SkillConfigManager.getUseSetting(hero, this, "projectile-size", 0.25, false);
        double projVelocity = SkillConfigManager.getUseSetting(hero, this, "projectile-velocity", 20.0, false);
        SpearProjectile missile = new SpearProjectile(plugin, this, hero, projSize, projVelocity);
        missile.fireMissile();

//        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0F, 0.7F);

        return SkillResult.NORMAL;
    }

    private class SpearProjectile extends BasicMissile {
        SpearProjectile(Plugin plugin, Skill skill, Hero hero, double projectileSize, double projVelocity) {
            super(plugin, skill, hero, projectileSize, projVelocity);

            setRemainingLife(SkillConfigManager.getUseSetting(hero, skill, "projectile-max-ticks-lived", 5, false));
            setGravity(SkillConfigManager.getUseSetting(hero, skill, "projectile-gravity", 2.5, false));
            this.damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 50.0, false);

            this.visualEffect = getSpearVisual(this.effectManager, player, getLocation());
        }

        @Override
        protected void onTick() {
            this.visualEffect.setTargetLocation(getLocation());
        }

        @Override
        protected void onEntityHit(Entity entity, Vector hitOrigin, Vector hitForce) {
            if (!(entity instanceof LivingEntity))
                return;

            LivingEntity target = (LivingEntity) entity;
            if (!damageCheck(this.player, target))
                return;

            damageEnemy(hero, target, player);
            spearEnemy(hero, player, target);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8F, 1.0F);
        }
    }

    private void damageEnemy(Hero hero, LivingEntity target, Player player) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50.0, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.6, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        if (damage > 0) {
            addSpellTarget(target, hero);
            damageEntity(target, player, damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK, false);
        }
    }

    private void spearEnemy(Hero hero, Player player, LivingEntity target) {
        boolean shouldWeaken = shouldWeaken(target.getLocation());

        Location playerLoc = player.getLocation();
        Location targetLoc = target.getLocation();

        Vector locDiff = playerLoc.toVector().subtract(targetLoc.toVector());
        if (shouldWeaken) {
            locDiff.multiply(0.75);
        }

        // Manually try to interrupt since we're doing custom projectile stuff
        if (target instanceof Player) {
            Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
            long interruptCd = SkillConfigManager.getUseSetting(hero, this, SkillSetting.ON_INTERRUPT_FORCE_COOLDOWN, 1500, false);
            targetHero.interruptDelayedSkill(interruptCd);
        }

        double vPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", 0.4, false);
        pushTargetUpwards(hero, target, vPower);
        pullTarget(hero, target, vPower, locDiff);
        playLingeringEffect(player, target);
    }

    private void pullTarget(Hero hero, LivingEntity target, double vPower, Vector locDiff) {
        double delay = SkillConfigManager.getUseSetting(hero, this, "pull-delay-ticks", 4, false);

        double hPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power-multiplier", 0.5, false);
        double hPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "horizontal-power-increase-per-strength", 0.0, false);
        hPower+= hPowerIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        final double finalHPower = hPower;
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                Vector pushVector = locDiff.setY(0).multiply(finalHPower).setY(vPower);
                target.setVelocity(pushVector);
            }
        }, 4);
    }

    private void pushTargetUpwards(Hero hero, LivingEntity target, double vPower) {
        final Vector pushUpVector = new Vector(0, vPower, 0);

        long exemptionDuration = SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 1000, false);
        if (exemptionDuration > 0) {
            NCPUtils.applyExemptions(target, new NCPFunction() {
                @Override
                public void execute() {
                    target.setVelocity(pushUpVector);
                }
            }, Lists.newArrayList("MOVING"), exemptionDuration);
        } else {
            target.setVelocity(pushUpVector);
        }
    }

    private boolean shouldWeaken(Location targetLoc) {
        Material mat = targetLoc.getBlock().getRelative(BlockFace.DOWN).getType();
        switch (mat) {
            case WATER:
            case LAVA:
            case SOUL_SAND:
                return true;
            default:
                return false;
        }
    }

    private void playLingeringEffect(Player player, LivingEntity target) {
        EffectManager effectManager = new EffectManager(plugin);
        LineEffect effect = getSpearVisual(effectManager, player, target);
        effect.period = 1;
        effect.iterations = 10;
        effectManager.start(effect);
        effectManager.disposeOnTermination();
    }

    private LineEffect getSpearVisual(EffectManager effectManager, Player owner, LivingEntity target) {
        LineEffect effect = getBaseSpearVisual(effectManager);

        DynamicLocation dynamicOwnerLoc = new DynamicLocation(owner);
        effect.setDynamicOrigin(dynamicOwnerLoc);

        DynamicLocation dynamicTargetLoc = new DynamicLocation(target);
        dynamicOwnerLoc.addOffset(new Vector(0, -0.5, 0));
        effect.setDynamicTarget(dynamicTargetLoc);

        return effect;
    }

    private LineEffect getSpearVisual(EffectManager effectManager, Player owner, Location targetLoc) {
        LineEffect effect = getBaseSpearVisual(effectManager);

        DynamicLocation dynamicOwnerLoc = new DynamicLocation(owner);
        effect.setDynamicOrigin(dynamicOwnerLoc);
        effect.setLocation(targetLoc);

        return effect;
    }

    private LineEffect getBaseSpearVisual(EffectManager effectManager) {
        LineEffect effect = new LineEffect(effectManager);
        effect.particle = Particle.CRIT;
        effect.particles = 5;
        effect.iterations = 9999;
        return effect;
    }
}
