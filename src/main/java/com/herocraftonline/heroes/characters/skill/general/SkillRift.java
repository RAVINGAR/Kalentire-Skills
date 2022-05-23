package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.GeometryUtil;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.ArrayList;
import java.util.List;

public class SkillRift extends TargettedSkill {
    public SkillRift(Heroes plugin) {
        super(plugin, "Rift");
        setDescription("Open a rift in space between you and your target, causing you to switch places. " +
                "After swapping, all enemies within $1 block(s) of both locations will take $2 damage. " +
                "Additionally, you will lose $3 harmful effect(s), and if your target was an ally, they will also lose $3 harmful effect(s). " +
                "If you targetted an enemy however, they will lose $4 beneficial effect(s) instead.");
        setUsage("/skill rift");
        setIdentifiers("skill rift");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_TEMPORAL, SkillType.SILENCEABLE, SkillType.TELEPORTING, SkillType.MULTI_GRESSIVE, SkillType.NO_SELF_TARGETTING);
    }

    @Override
    public String getDescription(Hero hero) {
        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 4.0, false);
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 40.0, false);
        int debuffRemovals = SkillConfigManager.getUseSetting(hero, this, "max-debuff-removals", 1, false);
        int buffRemovals = SkillConfigManager.getUseSetting(hero, this, "max-buff-removals", 1, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(radius))
                .replace("$2", Util.decFormat.format(damage))
                .replace("$3", debuffRemovals + "")
                .replace("$4", buffRemovals + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 10);
        config.set(SkillSetting.TARGET_HIT_TOLERANCE.node(), 2.5);
        config.set(SkillSetting.RADIUS.node(), 4.0);
        config.set(SkillSetting.DAMAGE.node(), 30.0);
        config.set("max-debuff-removals", 1);
        config.set("max-buff-removals", 1);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);

        if (targetCT.hasEffectType(EffectType.STUN) || targetCT.hasEffectType(EffectType.ROOT))
            return SkillResult.INVALID_TARGET;
        if (hero.hasEffectType(EffectType.STUN) || hero.hasEffectType(EffectType.ROOT))
            return SkillResult.INVALID_TARGET;

        broadcastExecuteText(hero, target);

        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 4.0, false);
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 40.0, false);

        Location originalTargetLoc = target.getLocation();
        Location originalPlayerLoc = player.getLocation();

        Location flippedPlayerLoc = player.getLocation();
        flippedPlayerLoc.setYaw(originalTargetLoc.getYaw());
        flippedPlayerLoc.setPitch(originalTargetLoc.getPitch());

        Location flippedTargetLoc = target.getLocation();
        flippedTargetLoc.setYaw(originalPlayerLoc.getYaw());
        flippedTargetLoc.setPitch(originalPlayerLoc.getPitch());

        dispelTarget(hero, hero);
        damageInCircle(hero, player, originalPlayerLoc, radius, damage);
        playFirework(originalPlayerLoc);

        if (hero.isAlliedTo(target)) {
            dispelTarget(hero, targetCT);
        } else {
            purgeTarget(hero, targetCT);
        }
        damageInCircle(hero, player, originalTargetLoc, radius, damage);
        playFirework(originalTargetLoc);

        player.teleport(flippedTargetLoc);
        target.teleport(flippedPlayerLoc);

        return SkillResult.NORMAL;
    }

    private void damageInCircle(Hero hero, Player player, Location damageLocation, double radius, double damage) {
        for (Entity entity : damageLocation.getWorld().getNearbyEntities(damageLocation, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }
            LivingEntity aoeTarget = (LivingEntity) entity;
            if (!damageCheck(player, aoeTarget))
                continue;

            addSpellTarget(aoeTarget, hero);
            damageEntity(aoeTarget, player, damage, EntityDamageEvent.DamageCause.MAGIC, false);
        }

        for (double r = 1.0; r < radius * 2; r++) {
            List<Location> particleLocations = GeometryUtil.circle(damageLocation, 45, r / 2);
            for (Location particleLocation : particleLocations)
                damageLocation.getWorld().spawnParticle(Particle.CRIT_MAGIC, particleLocation, 1, 0, 0.1, 0, 0.1);
        }
    }

    private void playFirework(Location location) {
        FireworkEffect firework = FireworkEffect.builder()
                .flicker(true)
                .trail(false)
                .withColor(Color.PURPLE)
                .withColor(Color.PURPLE)
                .withColor(Color.BLACK)
                .withFade(Color.BLACK)
                .with(FireworkEffect.Type.BURST)
                .build();
        VisualEffect.playInstantFirework(firework, location);
    }

    private void purgeTarget(Hero hero, CharacterTemplate targetCT) {
        int maxBuffRemovals = SkillConfigManager.getUseSetting(hero, this, "max-buff-removals", 1, false);
        if (maxBuffRemovals < 1)
            return;

        for (Effect effect : targetCT.getEffects()) {
            if (effect.isType(EffectType.DISPELLABLE) && effect.isType(EffectType.BENEFICIAL)) {
                hero.removeEffect(effect);
                maxBuffRemovals--;
                if (maxBuffRemovals == 0) {
                    break;
                }
            }
        }
    }

    private void dispelTarget(Hero hero, CharacterTemplate targetCT) {
        int maxDebuffRemovals = SkillConfigManager.getUseSetting(hero, this, "max-debuff-removals", 1, false);
        if (maxDebuffRemovals < 1)
            return;

        for (Effect effect : targetCT.getEffects()) {
            if (effect.isType(EffectType.DISPELLABLE) && effect.isType(EffectType.HARMFUL)) {
                targetCT.removeEffect(effect);
                maxDebuffRemovals--;
                if (maxDebuffRemovals == 0) {
                    break;
                }
            }
        }
    }
}