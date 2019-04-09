package com.herocraftonline.heroes.characters.skill.reborn.chainwarden;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SafeFallEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.ncp.NCPFunction;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SkillGrapple extends TargettedLocationSkill {

    public SkillGrapple(Heroes plugin) {
        super(plugin, "Grapple");
        setDescription("Using your chains as leverage, you use your tremendous strength to jump and grapple to them." +
                "Chained allies are more willing to allow this to happen, and so they can assist in pulling you harder.");
        setUsage("/skill grapple");
        setArgumentRange(0, 0);
        setIdentifiers("skill grapple");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.NO_SELF_TARGETTING, SkillType.MULTI_GRESSIVE);
    }

    public String getDescription(Hero hero) {
        return super.getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 25);
        config.set(SkillSetting.TARGET_HIT_TOLERANCE.node(), 0.5);
        return config;
    }

    public SkillResult use(Hero hero, Location targetLoc, String[] args) {
        Player player = hero.getPlayer();

        double grabRadius = SkillConfigManager.getUseSetting(hero, this, "grab-radius", 2.0, false);
        Location actualHookLoc = SkillHookshot.tryGetHookLocation(plugin, hero, targetLoc, grabRadius, true);
        if (actualHookLoc == null) {
            return SkillResult.INVALID_TARGET;
        }

        broadcastExecuteText(hero);

        Vector multiplier = new Vector(1, 1, 1);

        boolean shouldWeaken = shouldWeaken(player.getLocation());
        if (shouldWeaken) {
            multiplier.multiply(0.75);
        }
//        if (hero.isAlliedTo(targetLoc)) {
//            double allyMultiplier = SkillConfigManager.getUseSetting(hero, this, "ally-multiplier", 1.25, false);
//            multiplier.multiply(allyMultiplier);
//        }

        grappleToLocation(hero, actualHookLoc, multiplier);

        return SkillResult.NORMAL;
    }

    private void grappleToLocation(Hero hero, Location targetLoc, Vector multiplier) {
        final Player player = hero.getPlayer();

        Location playerLoc = player.getLocation();
        if (!(playerLoc.getWorld().equals(targetLoc.getWorld())))
            return;

        Vector playerLocVec = player.getLocation().toVector();
        Vector locVec = targetLoc.toVector();

        boolean noY = false;
        if (locVec.getY() < playerLoc.getY())
            noY = true;

        double horizontalDivider = SkillConfigManager.getUseSetting(hero, this, "horizontal-divisor", 6, false);
        double verticalDivider = SkillConfigManager.getUseSetting(hero, this, "vertical-divisor", 8, false);
        double xDir = (targetLoc.getX() - playerLoc.getX()) / horizontalDivider;
        double yDir = (targetLoc.getY() - playerLoc.getY()) / verticalDivider;
        double zDir = (targetLoc.getZ() - playerLoc.getZ()) / horizontalDivider;
        final Vector grappleVector = new Vector(xDir, yDir, zDir);
        if (grappleVector.getY() < 0.5)
            grappleVector.setY(0.5);

        // Prevent y velocity increase if told to.
        if (noY) {
            grappleVector.multiply(0.5).setY(0.5);	// Half the power of the grapple, but keep our minimum 0.5
        } else {
            // As long as we have Y, give them safefall
            long safeFallDuration = SkillConfigManager.getUseSetting(hero, this, "safe-fall-duration", 5000, false);
            hero.addEffect(new SafeFallEffect(this, player, safeFallDuration));
        }

        player.getWorld().playSound(playerLoc, Sound.ENTITY_MAGMA_CUBE_JUMP, 0.8F, 1.0F);

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