package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class SkillFlameDash extends ActiveSkill {

    public SkillFlameDash(Heroes plugin) {
        super(plugin, "FlameDash");
        setDescription("Dash straight forward at the speed of flame for up to $1 blocks away and auto-jumping any blocks that are in your path. " +
                "Any flammable blocks passed while dashing will be set on fire.");
        setUsage("/skill flamedash");
        setIdentifiers("skill flamedash");
        setArgumentRange(0, 0);
        setTypes(SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_FIRE, SkillType.TELEPORTING);
    }

    @Override
    public String getDescription(Hero hero) {
        int distance = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.MAX_DISTANCE, false);
        return getDescription().replace("$1", distance + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 8);
        config.set("max-step-climb", 2);
        config.set("max-step-drop", 6);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        Location playerLoc = player.getLocation();
        World world = playerLoc.getWorld();
        if (world == null || playerLoc.getBlockY() > world.getMaxHeight() || playerLoc.getBlockY() < world.getMinHeight()) {
            player.sendMessage("    " + ChatComponents.GENERIC_SKILL + "The void prevents you from flame dashing!");
            return SkillResult.FAIL;
        }

        Material standingBlockType = playerLoc.getBlock().getType();
        Material belowBlockType = playerLoc.getBlock().getRelative(BlockFace.DOWN).getType();
        //if (isWaterBlock(standingBlockType) || isWaterBlock(belowBlockType)) {
        //    player.sendMessage("    " + ChatComponents.GENERIC_SKILL + "You cannot flame dash in water!");
        //    return SkillResult.FAIL;
        //}

        return performFlameDash(hero, player);
    }

    private SkillResult performFlameDash(Hero hero, Player player) {
        Location currentPlayerLoc = player.getLocation().clone();
        World world = currentPlayerLoc.getWorld();
        Vector direction = currentPlayerLoc.getDirection().normalize();
        float pitch = currentPlayerLoc.getPitch();
        float yaw = currentPlayerLoc.getYaw();

        int maxDistance = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.MAX_DISTANCE, false);
        if (maxDistance < 2)
            maxDistance = 2;

        int maxStepClimb = SkillConfigManager.getUseSetting(hero, this, "max-step-climb", 2, false);
        if (maxStepClimb < 0)
            maxStepClimb = 0;

        int maxStepDrop = SkillConfigManager.getUseSetting(hero, this, "max-step-drop", 4, false);
        if (maxStepDrop < 0)
            maxStepDrop = 0;

        Block validFinalBlock = null;
        BlockIterator iter = null;
        try {
            iter = new BlockIterator(world, currentPlayerLoc.toVector(), direction.setY(0), 0, maxDistance);
        } catch (IllegalStateException e) {
            player.sendMessage("    " + ChatComponents.GENERIC_SKILL + "There was an error getting your blink location!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        List<Material> mustStepDownBlocks = new ArrayList<>(Util.transparentBlocks);
        mustStepDownBlocks.remove(Material.WATER);
        mustStepDownBlocks.remove(Material.LAVA);
        mustStepDownBlocks.remove(Material.FIRE);

        List<Block> fireTickBlocks = new ArrayList<>();
        List<Block> possibleFireTickBlocks = new ArrayList<>();
        Block previousBlock = currentPlayerLoc.getBlock();
        Block currentBlock = null;
        int totalStepUpsTaken = 0;
        while (iter.hasNext()) {
            Block iterBlock = iter.next();
            currentBlock = world.getBlockAt(iterBlock.getX(), iterBlock.getY() + totalStepUpsTaken, iterBlock.getZ());

            //if (isWaterBlock(currentBlock.getType()))
            //    break;

            possibleFireTickBlocks.add(previousBlock);

            // If we hit a transparent block and there is also space below it
            if (Util.transparentBlocks.contains(currentBlock.getType()) && mustStepDownBlocks.contains(currentBlock.getRelative(BlockFace.DOWN).getType())) {
                // We need to "step down" until we hit the limit.
                Block stepDownCurrentBlock = currentBlock;
                boolean foundValidFloorBlock = false;
                for (int i = 0; i <= maxStepDrop; i++) {    // 0 based since we don't actually step to the last thing we iterate on

                    Block tempStepDownBlock = stepDownCurrentBlock.getRelative(BlockFace.DOWN);

                    //if (isWaterBlock(tempStepDownBlock.getType())) {
                    //    break;
                    //} else
                    if (mustStepDownBlocks.contains(tempStepDownBlock.getType())) {
                        stepDownCurrentBlock = tempStepDownBlock;
                    } else {
                        foundValidFloorBlock = true;
                        totalStepUpsTaken -= i;
                        currentBlock = stepDownCurrentBlock;
                        break;
                    }
                }

                if (!foundValidFloorBlock) {
                    break;
                }
            }

            if (!Util.transparentBlocks.contains(currentBlock.getType())) {
                // Not a "passable" block for the player. We need to try and step upwards from the current AND previous locations.
                Block stepUpPreviousBlock = previousBlock;
                Block stepUpCurrentBlock = currentBlock;
                boolean foundValidNewBlock = false;
                for (int i = 1; i <= maxStepClimb; i++) {
                    stepUpCurrentBlock = stepUpCurrentBlock.getRelative(BlockFace.UP);

                    // If it's the first step, make sure we have space above the previous block.
                    if (i == 1) {
                        stepUpPreviousBlock = stepUpPreviousBlock.getRelative(BlockFace.UP);
                        if (//isWaterBlock(stepUpPreviousBlock.getType()) ||
                              !Util.transparentBlocks.contains(stepUpPreviousBlock.getType())) {
                            break;
                    }
                    }

                    if (//!isWaterBlock(stepUpCurrentBlock.getType()) &&
                          Util.transparentBlocks.contains(stepUpCurrentBlock.getType())) {
                        foundValidNewBlock = true;
                        totalStepUpsTaken += i;
                        currentBlock = stepUpCurrentBlock;
                        break;
                    }
                }

                if (!foundValidNewBlock) {
                    break;
                }
            }

            if (Util.transparentBlocks.contains(currentBlock.getRelative(BlockFace.UP).getType())) {
                // Found a spot where we have room for the player. Mark it as valid.
                validFinalBlock = currentBlock;
                fireTickBlocks.addAll(possibleFireTickBlocks);
                possibleFireTickBlocks.clear();
            }

            previousBlock = currentBlock;
        }

        boolean failed = validFinalBlock == null;

        if (validFinalBlock != null) {
            double distance = validFinalBlock.getLocation().distance(currentPlayerLoc.getBlock().getLocation());
            if (validFinalBlock.getY() == currentPlayerLoc.getBlockY() && distance <= 1)
                failed = true;
        }

        if (failed) {
            player.sendMessage("    " + ChatComponents.GENERIC_SKILL + "No location to flame dash to.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        fireTickBlocks.remove(validFinalBlock); // Just in case.
        plugin.getFireBlockManager().setBlocksOnFireIfAble(fireTickBlocks, 0.75);

        world.playSound(currentPlayerLoc, Sound.ENTITY_BLAZE_SHOOT, 1f, 0.533f);
        teleport(player, validFinalBlock, pitch, yaw);
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    private void teleport(Player player, Block validFinalBlock, float pitch, float yaw) {
        Location teleportLocation = validFinalBlock.getLocation();
        teleportLocation.add(new Vector(.5, 0, .5));
        teleportLocation.setPitch(pitch);
        teleportLocation.setYaw(yaw);
        player.teleport(teleportLocation);
    }

    private boolean isLiquidBlock(Material mat) {
        return isWaterBlock(mat) || isLavaBlock(mat);
    }

    private boolean isWaterBlock(Material mat) {
        return mat == Material.WATER;// || mat == Material.STATIONARY_WATER;
    }

    private boolean isLavaBlock(Material mat) {
        return mat == Material.LAVA;// || mat == Material.STATIONARY_LAVA;
    }
}