package com.herocraftonline.heroes.characters.skill.pack8;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

public class SkillShadowstep extends TargettedSkill {

    //private final BlockFace[] faces = { BlockFace.NORTH, BlockFace.NORTH_EAST, BlockFace.EAST, BlockFace.SOUTH_EAST, BlockFace.SOUTH, BlockFace.SOUTH_WEST, BlockFace.WEST, BlockFace.NORTH_WEST };

    public SkillShadowstep(Heroes plugin) {
        super(plugin, "Shadowstep");
        setDescription("Shadow step your target, teleporting behind them. Has a maximum targeting range of $1 blocks.");
        setUsage("/skill shadowstep");
        setArgumentRange(0, 0);
        setIdentifiers("skill shadowstep");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.MULTI_GRESSIVE, SkillType.NO_SELF_TARGETTING, SkillType.TELEPORTING, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 4, false);
        double distanceIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT, 0.15, false);
        distance += (int) (hero.getAttributeValue(AttributeType.INTELLECT) * distanceIncrease);

        return getDescription().replace("$1", distance + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection defaultConfig = super.getDefaultConfig();

        defaultConfig.set(SkillSetting.MAX_DISTANCE.node(), 4);
        defaultConfig.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), 0.15);
        defaultConfig.set("teleport-blocks-behind-target", 1);
        defaultConfig.set(SkillSetting.USE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% ShadowStepped behind %target%!");

        return defaultConfig;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {

        Player player = hero.getPlayer();

        //int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 1500, false);
        //int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 30, false);
        //duration += hero.getAttributeValue(AttributeType.CHARISMA) * durationIncrease;

        if (target == player || !(target instanceof Player)) {
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        Location playerLoc = player.getLocation();
        Location targetLoc = target.getLocation().clone();
        targetLoc.setPitch(0);      // Reset pitch so that we don't have to worry about it.

        BlockIterator iter = null;
        try {
            Vector direction = targetLoc.getDirection().multiply(-1);
            int blocksBehindTarget = SkillConfigManager.getUseSetting(hero, this, "teleport-blocks-behind-target", 1, false);
            iter = new BlockIterator(target.getWorld(), targetLoc.toVector(), direction, 0, blocksBehindTarget);
        }
        catch (IllegalStateException e) {
            Messaging.send(player, "There was an error getting the Shadowstep location!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        Block prev = null;
        Block b;
        while (iter.hasNext()) {
            b = iter.next();
            // Messaging.send(player, "Looping through blocks. Current Block: " + b.getType().toString());      // DEBUG

            // Validate blocks near destination
            if (Util.transparentBlocks.contains(b.getType()) && (Util.transparentBlocks.contains(b.getRelative(BlockFace.UP).getType()) || Util.transparentBlocks.contains(b.getRelative(BlockFace.DOWN).getType()))) {
                prev = b;
            }
            else {
                break;
            }
        }
        if (prev != null) {
            Location targetTeleportLoc = prev.getLocation().clone();
            targetTeleportLoc.add(new Vector(.5, 0, .5));

            // Set the blink location yaw/pitch to that of the target
            targetTeleportLoc.setPitch(0);
            targetTeleportLoc.setYaw(targetLoc.getYaw());
            player.teleport(targetTeleportLoc);

            broadcastExecuteText(hero, target);

            //plugin.getCharacterManager().getCharacter(target).addEffect(new StunEffect(this, player, duration));
            player.getWorld().playEffect(playerLoc, Effect.ENDER_SIGNAL, 3);
            player.getWorld().playSound(playerLoc, CompatSound.ENTITY_ENDERMEN_TELEPORT.value(), 0.8F, 1.0F);

            return SkillResult.NORMAL;
        }
        else {
            Messaging.send(player, "No location to shadowstep to.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        // OLD METHOD. Leaving here for now just in case I decide to switch things back.

        //		Location targetLoc = target.getLocation();
        //		BlockFace targetFace = faces[Math.round(targetLoc.getYaw() / 45f) & 0x7];
        //		Block teleBlock = targetLoc.getBlock().getRelative(targetFace);
        //
        //		// Ensure that the location isn't inside of a block
        //		if (Util.transparentBlocks.contains(teleBlock.getType()) && (Util.transparentBlocks.contains(teleBlock.getRelative(BlockFace.UP).getType()) || Util.transparentBlocks.contains(teleBlock.getRelative(BlockFace.DOWN).getType()))) {
        //
        //			// Get the location
        //			Location teleLoc = teleBlock.getLocation();
        //			teleLoc.setYaw(targetLoc.getYaw());
        //			teleLoc.setPitch(targetLoc.getPitch());
        //          teleLoc.add(new Vector(.5, 0, .5));
        //
        //			// Teleport the player
        //			if (player.teleport(teleLoc)) {
        //
        //				// Play Sound
        //                player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_ENDERMEN_TELEPORT.value(), 0.8F, 1.0F);
        //
        //				// Play Effect
        //				player.getWorld().playEffect(player.getLocation(), Effect.ENDER_SIGNAL, 3);
        //
        //				// Announce skill usage
        //				broadcast(targetLoc, useText, hero.getName(), ((Player) target).getName());
        //
        //				return SkillResult.NORMAL;
        //			}
        //			else {
        //                Messaging.send(player, teleFailText);
        //				return SkillResult.FAIL;
        //			}
        //		}
        //		else {
        //            Messaging.send(player, teleFailText);
        //			return SkillResult.FAIL;
        //		}
    }
}
