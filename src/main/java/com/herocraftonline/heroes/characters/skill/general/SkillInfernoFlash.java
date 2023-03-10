package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public class SkillInfernoFlash extends ActiveSkill {

    public SkillInfernoFlash(Heroes plugin) {
        super(plugin, "InfernoFlash");
        setDescription("Teleports you up to $1 blocks away in a fiery flash");
        setUsage("/skill infernoflash");
        setArgumentRange(0, 0);
        setIdentifiers("skill infernoflash");
        setTypes(SkillType.SILENCEABLE, SkillType.MOVEMENT_INCREASING, SkillType.TELEPORTING);
    }

    @Override
    public String getDescription(Hero hero) {
        int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 6, false);
        double distanceIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT, 0.15, false);
        distance += (int) (hero.getAttributeValue(AttributeType.INTELLECT) * distanceIncrease);

        return getDescription().replace("$1", distance + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 6);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), 0.15);
        node.set(SkillSetting.REAGENT.node(), "REDSTONE");
        node.set(SkillSetting.REAGENT_COST.node(), 3);

        return node;
    }
    
    public ArrayList<Location> circle(Location centerPoint, int particleAmount, double circleRadius)
   	{
   		World world = centerPoint.getWorld();

   		double increment = (2 * Math.PI) / particleAmount;

   		ArrayList<Location> locations = new ArrayList<>();

   		for (int i = 0; i < particleAmount; i++)
   		{
   			double angle = i * increment;
   			double x = centerPoint.getX() + (circleRadius * Math.cos(angle));
   			double z = centerPoint.getZ() + (circleRadius * Math.sin(angle));
   			locations.add(new Location(world, x, centerPoint.getY(), z));
   		}
   		return locations;
   	}

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        Location loc = player.getLocation();
        if (loc.getBlockY() > loc.getWorld().getMaxHeight() || loc.getBlockY() < 1) {
            player.sendMessage("The void prevents you from flashing!");
            return SkillResult.FAIL;
        }

        int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 6, false);
        double distanceIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT, 0.15, false);
        distance += (int) (hero.getAttributeValue(AttributeType.INTELLECT) * distanceIncrease);

        Material mat = player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType();
        switch (mat) {
            case WATER:
            case LAVA:
            case SOUL_SAND:
                distance *= 0.75;
                break;
            default:
                break;
        }

        Block validFinalBlock = null;
        Block currentBlock;

        BlockIterator iter = null;
        try {
            iter = new BlockIterator(player, distance);
        }
        catch (IllegalStateException e) {
            player.sendMessage("There was an error getting your flash location!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        while (iter.hasNext()) {
            currentBlock = iter.next();
            //currentBlock.getWorld().spigot().playEffect(currentBlock.getLocation(), Effect.MOBSPAWNER_FLAMES, 0, 0, 0.0F, 0.5F, 0.0F, 0.0F, 1, 16);
            //FIXME See if this is correct
            player.getWorld().spawnParticle(Particle.FLAME, currentBlock.getLocation(), 1, 0, 0.5, 0, 0);
            Material currentBlockType = currentBlock.getType();

            if (Util.transparentBlocks.contains(currentBlockType)) {
                if (Util.transparentBlocks.contains(currentBlock.getRelative(BlockFace.UP).getType())) {
                    validFinalBlock = currentBlock;
                }
            }
            else {
                break;
            }
        }

        // Old Method
        //        while (iter.hasNext()) {
        //            b = iter.next();
        //            if (Util.transparentBlocks.contains(b.getType()) && (Util.transparentBlocks.contains(b.getRelative(BlockFace.UP).getType()) || Util.transparentBlocks.contains(b.getRelative(BlockFace.DOWN).getType()))) {
        //                prev = b;
        //            }
        //            else {
        //                break;
        //            }
        //        }

        if (validFinalBlock != null) {
            Location teleport = validFinalBlock.getLocation().clone();
            teleport.add(new Vector(.5, 0, .5));

            // Set the flash location yaw/pitch to that of the player
            teleport.setPitch(loc.getPitch());
            teleport.setYaw(loc.getYaw());

            ArrayList<Location> locations = circle(player.getLocation(), 72, 1.5);
            for (Location location : locations) {
                //player.getWorld().spigot().playEffect(locations.get(i), org.bukkit.Effect.FLAME, 0, 0, 0, 1.2F, 0, 0, 6, 16);
                player.getWorld().spawnParticle(Particle.FLAME, location, 6, 0, 1.2, 0, 0);
            }
            //teleport.getWorld().spigot().playEffect(teleport, Effect.FLAME, 0, 0, 0.5F, 0.5F, 0.5F, 0.5F, 45, 16);
            player.getWorld().spawnParticle(Particle.FLAME, teleport, 45, 0.5, 0.5, 0.5, 0.5);
            player.teleport(teleport);
            player.getWorld().playEffect(loc, Effect.ENDER_SIGNAL, 3);
            player.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8F, 1.0F);

            return SkillResult.NORMAL;
        }
        else {
            player.sendMessage("No location to flash to.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
    }
}