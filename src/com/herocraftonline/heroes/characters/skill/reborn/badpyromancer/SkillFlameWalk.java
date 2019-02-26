package com.herocraftonline.heroes.characters.skill.reborn.badpyromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.nms.physics.FluidCollision;
import com.herocraftonline.heroes.nms.physics.NMSPhysics;
import com.herocraftonline.heroes.nms.physics.RayCastHit;
import com.herocraftonline.heroes.nms.physics.RayCastInfo;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SkillFlameWalk extends ActiveSkill {

    private final NMSPhysics physics = NMSPhysics.instance();

    public SkillFlameWalk(Heroes plugin) {
        super(plugin, "FlameWalk");
        setDescription("Upon targetting a flame within $1 blocks with this skill, you walk instantly within the flame itself, consuming it and teleporting to that location.");
        setUsage("/skill flamewalk");
        setArgumentRange(0, 0);
        setIdentifiers("skill flamewalk");
        setTypes(SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_FIRE, SkillType.TELEPORTING);
    }

    @Override
    public String getDescription(Hero hero) {
        int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 18, false);
        return getDescription().replace("$1", distance + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 18);
//        node.set(SkillSetting.REAGENT.node(), 331);
//        node.set(SkillSetting.REAGENT_COST.node(), 3);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        Location loc = player.getLocation();
        if (loc.getBlockY() > loc.getWorld().getMaxHeight() || loc.getBlockY() < 1) {
            player.sendMessage("The void prevents you from flamewalking!");
            return SkillResult.FAIL;
        }

        int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 18, false);

        Location rayCastLoc = tryGetValidTargetViaRaycast(player, distance);
        if (rayCastLoc != null)
            return teleportPlayer(hero, rayCastLoc);

        Block targetBlock = tryGetValidTargetViaBlockIterator(player, distance);
        if (targetBlock != null) {
            return teleportPlayer(hero, targetBlock.getLocation().clone());
        } else {
            player.sendMessage("Unable to find a valid flame to walk to.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
    }

    @Nullable
    private Block tryGetValidTargetViaBlockIterator(Player player, int distance) {
        Block validFlameBlock = null;
        Block currentBlock;

        BlockIterator iter = null;
        try {
            iter = new BlockIterator(player, distance);
        } catch (IllegalStateException e) {
            player.sendMessage("There was an error getting your blink location!");
            return null;
        }

        while (iter.hasNext()) {
            currentBlock = iter.next();

            if (!Util.transparentBlocks.contains(currentBlock.getType()) && !(isValidTeleportBlock(currentBlock)))
                break;

            Block aboveBlock = currentBlock.getRelative(BlockFace.UP);
            if (isValidTeleportBlock(currentBlock) && (isValidTeleportBlock(aboveBlock) || Util.transparentBlocks.contains(aboveBlock.getType())))
                validFlameBlock = currentBlock;
        }
        return validFlameBlock;
    }

    private boolean isValidTeleportBlock(Block block) {
        Material blockType = block.getType();
        //BlockData blockData = block.getBlockData();       // Couldn't find anything useful here...
        return blockType == Material.FIRE || blockType == Material.LAVA;
    }

    private Location tryGetValidTargetViaRaycast(Player player, int maxDist) {
        Block targetBlock = null;
        World world = player.getWorld();
        Location eyeLocation = player.getEyeLocation();
        Vector normal = eyeLocation.getDirection();
        Vector start = eyeLocation.toVector();
        Vector end = normal.clone().multiply(maxDist).add(start);

        RayCastInfo rayCastInfo = new RayCastInfo();
        rayCastInfo.setEntityFilter(x -> x.getFireTicks() > 0);
        rayCastInfo.setBlockFluidCollision(FluidCollision.SOURCE_ONLY);

        RayCastHit rayCastHit = physics.rayCast(world, start, end, rayCastInfo);
        if (rayCastHit != null) {
            if (rayCastHit.isEntity())
                return rayCastHit.getEntity().getLocation().clone();
            if (isValidTeleportBlock(rayCastHit.getBlock(world)))
                return rayCastHit.getBlock(world).getLocation().clone();
        }
        return null;
    }

    private SkillResult teleportPlayer(Hero hero, Location teleportLocation) {
        Player player = hero.getPlayer();
        Location playerLoc = player.getLocation();
        teleportLocation.add(new Vector(.5, 0, .5));

        teleportLocation.setPitch(playerLoc.getPitch());
        teleportLocation.setYaw(playerLoc.getYaw());

//        player.getWorld().spawnParticle(Particle.REDSTONE, player.getLocation(), 45, 0.6, 1, 0.6, 0.2, new Particle.DustOptions(Color.FUCHSIA, 1));
        player.teleport(teleportLocation);
//        player.getWorld().playEffect(playerLoc, Effect.ENDER_SIGNAL, 3);
//        player.getWorld().spawnParticle(Particle.REDSTONE, player.getLocation(), 45, 0.6, 1, 0.6, 0.2, new Particle.DustOptions(Color.FUCHSIA, 1));
//        player.getWorld().playSound(playerLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8F, 1.0F);

        if (teleportLocation.getBlock().getType() == Material.FIRE) {
            teleportLocation.getBlock().setType(Material.AIR);
        }

        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }
}