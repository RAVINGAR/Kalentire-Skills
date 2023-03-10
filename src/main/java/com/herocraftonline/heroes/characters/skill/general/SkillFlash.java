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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

public class SkillFlash extends ActiveSkill {

    public SkillFlash(Heroes plugin) {
        super(plugin, "Flash");
        setDescription("Teleports you up to $1 blocks away.");
        setUsage("/skill flash");
        setArgumentRange(0, 0);
        setIdentifiers("skill flash");
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

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        Location loc = player.getLocation();
        if (loc.getBlockY() > loc.getWorld().getMaxHeight() || loc.getBlockY() < loc.getWorld().getMinHeight() + 1) {
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

            //player.getWorld().spigot().playEffect(player.getLocation(), Effect.CLOUD, 0, 0, 0, 0, 0, 1.5F, 35, 16);
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 35, 0, 0, 0, 1.5);
            player.teleport(teleport);
            //player.getWorld().spigot().playEffect(player.getLocation(), Effect.CLOUD, 0, 0, 0, 0, 0, 1.5F, 35, 16);
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 35, 0, 0, 0, 1.5);
            player.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8F, 1.0F);

            return SkillResult.NORMAL;
        }
        else {
            player.sendMessage("No location to flash to.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
    }
}