package com.herocraftonline.heroes.characters.skill.reborn.chainwarden;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SafeFallEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.ncp.NCPFunction;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import com.herocraftonline.heroes.nms.physics.NMSPhysics;
import com.herocraftonline.heroes.nms.physics.RayCastHit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.logging.Level;

public class SkillGrapple extends ActiveSkill {
    protected final NMSPhysics physics = NMSPhysics.instance();

    public SkillGrapple(Heroes plugin) {
        super(plugin, "Grapple");
        setDescription("After aiming at the hook you wish to use as leverage, you use your tremendous strength to jump and grapple your way down the chains." +
                "Enemies are haulted slightly by this motion, whereas allies assist in pulling you further.");
        setUsage("/skill grapple");
        setArgumentRange(0, 0);
        setIdentifiers("skill grapple");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.MULTI_GRESSIVE);
    }

    public String getDescription(Hero hero) {
        return super.getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 25);
        config.set("grab-radius", 2.5);
        config.set("horizontal-divider", 7.0);
        config.set("vertical-divider", 6.0);
        config.set("ally-multiplier", 1.25);
        config.set("safe-fall-duration", 5000);
        config.set("ncp-exemption-duration", 0);
        return config;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        World world = player.getWorld();

        double maxDistance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 25.0, false);
        Location eyeLocation = player.getEyeLocation();
        Vector normal = eyeLocation.getDirection();
        Vector start = eyeLocation.toVector();
        Vector end = normal.clone().multiply(maxDistance).add(start);
        RayCastHit hit = this.physics.rayCast(world, player, start, end);

        Vector multiplier = new Vector(1, 1, 1);
        double grabRadius = SkillConfigManager.getUseSetting(hero, this, "grab-radius", 2.0, false);

        // Check for whether our raycast hit a location, not an entity
        Location targetLoc = null;
        if (hit == null) {  // Null means it hit air. Dumb I kno
            targetLoc = new Location(world, end.getX(), end.getY(), end.getZ());
        } else if (hit.getResult() == RayCastHit.Result.BLOCK) {
            targetLoc = new Location(world, hit.getPointX(), hit.getPointY(), hit.getPointZ());
        } else if (hit.getResult() == RayCastHit.Result.ENTITY && hit.getEntity() != null && !(hit.getEntity() instanceof LivingEntity)) {
            // We hit an entity that wasn't alive. Like a painting or some shit.
            targetLoc = hit.getEntity().getLocation();
        }

        // If we have a valid raycast target location...
        if (targetLoc != null) {
            Location actualHookLoc = SkillHook.tryGetHookLocation(plugin, hero, targetLoc, grabRadius, true);
            if (actualHookLoc != null) {
                broadcastExecuteText(hero);
                grappleToLocation(hero, actualHookLoc, multiplier);
                return SkillResult.NORMAL;
            } else {
                SkillHook.InvalidHookTargetReason invalidHookTargetReason = SkillHook.hasValidHookLocation(
                        plugin, hero, targetLoc, grabRadius);
                SkillHook.broadcastInvalidHookTargetText(hero, invalidHookTargetReason);
                return SkillResult.INVALID_TARGET_NO_MSG;
            }
        }

        // Check if we hit a valid living entity.
        if (hit != null && hit.getEntity() != null && hit.getEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) hit.getEntity();
            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter((LivingEntity) target);

            SkillHook.InvalidHookTargetReason invalidHookTargetReason = SkillHook.tryRemoveHook(hero, targetCT);
            if (invalidHookTargetReason == SkillHook.InvalidHookTargetReason.VALID_TARGET) {
                // Found a valid hook target.

                if (hero.isAlliedTo(target)) {
                    double allyMultiplier = SkillConfigManager.getUseSetting(hero, this, "ally-multiplier", 1.25, false);
                    multiplier.multiply(allyMultiplier);
                } else {
                    target.setVelocity(new Vector(0, 0, 0));
                }

                broadcastExecuteText(hero);
                grappleToLocation(hero, target.getLocation(), multiplier);
                return SkillResult.NORMAL;
            }
        }

        return SkillResult.INVALID_TARGET;
    }

    private class TargetFinder {
        public LivingEntity Target = null;
    }

    private void grappleToLocation(Hero hero, Location targetLoc, Vector multiplier) {
        final Player player = hero.getPlayer();

        Location playerLoc = player.getLocation();
        if (!(playerLoc.getWorld().equals(targetLoc.getWorld())))
            return;

        boolean shouldWeaken = shouldWeaken(player.getLocation());
        if (shouldWeaken) {
            multiplier.multiply(0.75);
        }

        Vector playerLocVec = player.getLocation().toVector();
        Vector locVec = targetLoc.toVector();

        double horizontalDivider = SkillConfigManager.getUseSetting(hero, this, "horizontal-divider", 6.0, false);
        double verticalDivider = SkillConfigManager.getUseSetting(hero, this, "vertical-divider", 8.0, false);
        double xDir = (targetLoc.getX() - playerLoc.getX()) / horizontalDivider;
        double yDir = (targetLoc.getY() - playerLoc.getY()) / verticalDivider;
        double zDir = (targetLoc.getZ() - playerLoc.getZ()) / horizontalDivider;
        final Vector grappleVector = new Vector(xDir, yDir, zDir);

        if (grappleVector.getY() < 0.5) {
            grappleVector.setY(0.4);
        }
        if (locVec.getY() < playerLoc.getY()) {
            grappleVector.setY(0.4);
        }

        double horizontalPower = grappleVector.clone().setY(0).length();
        double verticalPower = grappleVector.clone().setX(0).setZ(0).length();

//        double minVerticalVelocity = SkillConfigManager.getUseSetting(hero, this, "min-vertical-velocity", 0.4, false);
//        double maxVerticalVelocity = SkillConfigManager.getUseSetting(hero, this, "max-vertical-velocity", 1.85, false);
//        if (verticalPower < minVerticalVelocity) {
//            locVec.setY()
//        }
//        if (verticalPower > maxVerticalVelocity) {
//            locVec.setY(maxVerticalVelocity);
//        } else

        Heroes.log(Level.INFO, "Grapple Horizontal Power: " + horizontalPower);
        Heroes.log(Level.INFO, "Grapple Vertical Power: " + verticalPower);

        // As long as we have Y, give them safefall
        long safeFallDuration = SkillConfigManager.getUseSetting(hero, this, "safe-fall-duration", 5000, false);
        hero.addEffect(new SafeFallEffect(this, player, safeFallDuration, null, null));

        long exemptionDuration = SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 0, false);
        if (exemptionDuration > 0) {
            NCPUtils.applyExemptions(player, new NCPFunction() {
                @Override
                public void execute() {
                    player.setVelocity(grappleVector);
                }
            }, Lists.newArrayList("MOVING"), exemptionDuration);
        } else {
            player.setVelocity(grappleVector);
        }

        player.getWorld().playSound(playerLoc, Sound.ENTITY_MAGMACUBE_JUMP, 0.8F, 1.0F);
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