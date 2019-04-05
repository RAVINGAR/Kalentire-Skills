package com.herocraftonline.heroes.characters.skill.reborn.hookwarrior;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.ncp.NCPFunction;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

public class SkillYank extends TargettedSkill {
    public SkillYank(Heroes plugin) {
        super(plugin, "Yank");
        setDescription("Yank on the chains of a hooked target, pulling them towards you and dealing $1 damage. " +
                "Chained allies are more willing to allow this to happen, and so they do not take damage and are pulled harder.");
        setUsage("/skill yank");
        setArgumentRange(0, 0);
        setIdentifiers("skill yank");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.NO_SELF_TARGETTING, SkillType.MULTI_GRESSIVE);
    }

    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        return getDescription()
                .replace("$1", Util.decFormat.format(damage));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 25);
        config.set(SkillSetting.DURATION.node(), 10000);
        config.set(SkillSetting.TARGET_HIT_TOLERANCE.node(), 0.5);
        config.set(SkillSetting.DAMAGE.node(), 40.0);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 0.0);
        config.set("vertical-power", 0.4);
        config.set("horizontal-power", 1.0);
        config.set("horizontal-power-increase-per-strength", 0.0);
        return config;
    }

    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
        if (!targetCT.hasEffect(player.getName() + "-Hooked"))
            return SkillResult.INVALID_TARGET;

        broadcastExecuteText(hero, target);

        boolean shouldWeaken = shouldWeaken(target.getLocation());

        Location playerLoc = player.getLocation();
        Location targetLoc = target.getLocation();

        double vPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", 0.4, false);
        double hPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", 0.5, false);
        double hPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "horizontal-power-increase-per-strength", 0.0125, false);
        hPower += hPowerIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        double xDir = (playerLoc.getX() - targetLoc.getX());
        double zDir = (playerLoc.getZ() - targetLoc.getZ());

        if (shouldWeaken) {
            vPower *= 0.75;
            hPower *= 0.75;
        }

        if (hero.isAlliedTo(target)) {
            pushTargetUpwards(hero, target, vPower, true);
            pullTarget(hero, target, vPower, hPower, xDir / 2, zDir / 2);
        } else {
            pushTargetUpwards(hero, target, vPower, false);
            pullTarget(hero, target, vPower, hPower, xDir / 3, zDir / 3);
            damageEnemy(hero, target, player);
        }

        return SkillResult.NORMAL;
    }

    private void damageEnemy(Hero hero, LivingEntity target, Player player) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50.0, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        if (damage > 0) {
            addSpellTarget(target, hero);
            damageEntity(target, player, damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK, false);
        }
    }

    private void pullTarget(Hero hero, LivingEntity target, double vPower, double hPower, double xDir, double zDir) {
        double delay = SkillConfigManager.getUseSetting(hero, this, "pull-delay", 0.2, false);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                Vector pushVector = new Vector(xDir, 0, zDir).normalize().multiply(hPower).setY(vPower);
                target.setVelocity(pushVector);
            }
        }, (long) (delay * 20));
    }

    private void pushTargetUpwards(Hero hero, LivingEntity target, double vPower, boolean reduceFallDamage) {
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

        if (reduceFallDamage)
            target.setFallDistance(target.getFallDistance() - 3F);
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
}