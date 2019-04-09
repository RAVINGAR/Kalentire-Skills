package com.herocraftonline.heroes.characters.skill.reborn.chainwarden;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.GeometryUtil;
import de.slikey.effectlib.util.VectorUtils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.logging.Level;

public class SkillFullArsenal extends ActiveSkill {
    public SkillFullArsenal(Heroes plugin) {
        super(plugin, "FullArsenal");
        setDescription("Unleash all yo damn hooks (Tell delf to fix this)");
        setUsage("/skill fullarsenal");
        setIdentifiers("skill fullarsenal");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.MULTI_GRESSIVE, SkillType.AREA_OF_EFFECT);
    }

    public String getDescription(Hero hero) {
        return super.getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("launch-arc-degrees", 60.0);
        return config;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        SkillHook hookSkill = (SkillHook) plugin.getSkillManager().getSkill("Hook");
        if (hookSkill == null) {
            Heroes.log(Level.SEVERE, "SkillHook is missing from the server. SkillFullArsenal will no longer work. SkillHook _must_ be available to the class that has SkillFullArsenal.");
            player.sendMessage("One of the Admins or devs broke this skill. Tell them to read the heroes logs to fix it.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        int numChains = SkillChainBelt.tryGetCurrentChainCount(hero);
        if (numChains == 0) {
            player.sendMessage("    " + ChatComponents.GENERIC_SKILL + "No chains available to throw!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        broadcastExecuteText(hero);

        Location eyeLoc = player.getEyeLocation();
        double arcDegrees = SkillConfigManager.getUseSetting(hero, this, "launch-arc-degrees", 60.0, false);
        double arcRads = Math.toRadians(arcDegrees);

        double incrementDegrees = arcDegrees / numChains;
        Heroes.log(Level.INFO, "Increment Degrees: " + incrementDegrees);
        Vector direction = player.getLocation().getDirection();
        Heroes.log(Level.INFO, "Base Direction: " + direction.toString());
        Vector nextDirection = direction.clone().normalize();
        Location hookLoc = eyeLoc.add(direction.multiply(1.25D));

        for (int i = 0; i < numChains; i++) {
            if (!SkillChainBelt.tryRemoveChain(this, hero, false))
                break;
            
            nextDirection = GeometryUtil.rotateVector(nextDirection, (float) incrementDegrees, 0.0F);
            Heroes.log(Level.INFO, "Next Direction: " + nextDirection.toString());
            SkillHook.HookProjectile missile = hookSkill.createHookProjectile(hero);
            missile.setLocation(hookLoc);
            missile.setDirection(nextDirection);
            missile.fireMissile();

            if (i == 0 || i % 2 == 0) // Play sound Every 2 loops
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_2, 0.5F, 0.5F);
        }

        SkillChainBelt.showCurrentChainCount(hero);
        return SkillResult.NORMAL;
    }

//    private void damageEnemy(Hero hero, LivingEntity target, Player player) {
//        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50.0, false);
//        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.0, false);
//        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);
//
//        if (damage > 0) {
//            addSpellTarget(target, hero);
//            damageEntity(target, player, damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK, false);
//        }
//    }
//
//    private void pullTarget(Hero hero, LivingEntity target, double vPower, double hPower, double xDir, double zDir) {
//        double delay = SkillConfigManager.getUseSetting(hero, this, "pull-delay", 0.2, false);
//        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
//            public void run() {
//                Vector pushVector = new Vector(xDir, 0, zDir).normalize().multiply(hPower).setY(vPower);
//                target.setVelocity(pushVector);
//                target.getWorld().playSound(target.getLocation(), Sound.BLOCK_CHEST_LOCKED, 0.5F, 1.5F);
//            }
//        }, (long) (delay * 20));
//    }
//
//    private void pushTargetUpwards(Hero hero, LivingEntity target, double vPower, boolean reduceFallDamage) {
//        final Vector pushUpVector = new Vector(0, vPower, 0);
//
//        long exemptionDuration = SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 1000, false);
//        if (exemptionDuration > 0) {
//            NCPUtils.applyExemptions(target, new NCPFunction() {
//                @Override
//                public void execute() {
//                    target.setVelocity(pushUpVector);
//                }
//            }, Lists.newArrayList("MOVING"), exemptionDuration);
//        } else {
//            target.setVelocity(pushUpVector);
//        }
//
//        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_CHEST_LOCKED, 0.5F, 1.5F);
//
//        if (reduceFallDamage)
//            target.setFallDistance(target.getFallDistance() - 3F);
//    }
//
//    private boolean shouldWeaken(Location targetLoc) {
//        Material mat = targetLoc.getBlock().getRelative(BlockFace.DOWN).getType();
//        switch (mat) {
//            case WATER:
//            case LAVA:
//            case SOUL_SAND:
//                return true;
//            default:
//                return false;
//        }
//    }
}