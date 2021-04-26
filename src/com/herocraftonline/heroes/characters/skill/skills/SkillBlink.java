package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;

public class SkillBlink extends ActiveSkill {

    public SkillBlink(Heroes plugin) {
        super(plugin, "Blink");
        setDescription("Teleports you up to $1 blocks away.");
        setUsage("/skill blink");
        setArgumentRange(0, 0);
        setIdentifiers("skill blink");
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
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 6);
        config.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), 0.15);
        //config.set(SkillSetting.REAGENT.node(), Material.GUNPOWDER.name());
        config.set(SkillSetting.REAGENT.node(), 331);
        config.set(SkillSetting.REAGENT_COST.node(), 3);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        Location loc = player.getLocation();
        if (loc.getBlockY() > loc.getWorld().getMaxHeight() || loc.getBlockY() < 1) {
            player.sendMessage("The void prevents you from blinking!");
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
            player.sendMessage("There was an error getting your blink location!");
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

            // Set the blink location yaw/pitch to that of the player
            teleport.setPitch(loc.getPitch());
            teleport.setYaw(loc.getYaw());

            player.getWorld().spigot().playEffect(player.getLocation(), Effect.COLOURED_DUST, 0, 0, 0.6F, 1.0F, 0.6F, 0.2F, 45, 16);
            //player.getWorld().spawnParticle(Particle.REDSTONE, player.getLocation(), 45, 0.6, 1, 0.6, 0.2, new Particle.DustOptions(Color.FUCHSIA, 1));
            player.teleport(teleport);
            player.getWorld().playEffect(loc, Effect.ENDER_SIGNAL, 3);
            player.getWorld().spigot().playEffect(player.getLocation(), Effect.COLOURED_DUST, 0, 0, 0.6F, 1.0F, 0.6F, 0.2F, 45, 16);
            //player.getWorld().spawnParticle(Particle.REDSTONE, player.getLocation(), 45, 0.6, 1, 0.6, 0.2, new Particle.DustOptions(Color.FUCHSIA, 1));
            //player.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8F, 1.0F);
            player.getWorld().playSound(loc, Sound.ENTITY_ENDERMEN_TELEPORT, 0.8F, 1.0F);

            return SkillResult.NORMAL;
        }
        else {
            player.sendMessage("No location to blink to.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
    }
}