package com.herocraftonline.heroes.characters.skill.reborn.druid;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.SafeFallEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.nms.physics.NMSPhysics;
import com.herocraftonline.heroes.nms.physics.RayCastHit;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.logging.Level;

public class SkillOvergrowth extends TargettedLocationSkill {

    private static final Random random = new Random(System.currentTimeMillis());

    public SkillOvergrowth(Heroes plugin) {
        super(plugin, "Overgrowth");
        setDescription("Create a large overgrowth in front of you that is $1 blocks wide and $2 blocks tall. " +
                "Anyone that is near the overgrowth when it grows will be transported on top of it. " +
                "The overgrowth lasts for a maximum of $3 seconds, and anyone that remains on top of it when it ends will be granted safety from fall damage.");
        setUsage("/skill overgrowth");
        setArgumentRange(0, 0);
        setIdentifiers("skill overgrowth");
        setToggleableEffectName("Overgrowth");
        setTypes(SkillType.ABILITY_PROPERTY_EARTH, SkillType.SILENCEABLE, SkillType.BLOCK_CREATING);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 12);
        config.set(ALLOW_TARGET_AIR_BLOCK_NODE, false);
        config.set(TRY_GET_SOLID_BELOW_BLOCK_NODE, true);
        config.set(MAXIMUM_FIND_SOLID_BELOW_BLOCK_HEIGHT_NODE, 10);
        config.set("height", 16);
        config.set(SkillSetting.RADIUS.node(), 4);
        config.set(SkillSetting.DURATION.node(), 7500);
        return config;
    }

    public String getDescription(Hero hero) {
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 4, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 7500, false);
        int height = SkillConfigManager.getUseSetting(hero, this, "height", 16, false);

        return getDescription()
                .replace("$1", radius + "")
                .replace("$2", height + "")
                .replace("$3", Util.decFormat.format((double) duration / 1000));
    }

    public SkillResult use(Hero hero, Location targetLocation, String[] args) {
        Player player = hero.getPlayer();

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 4, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 7500, false);
        int height = SkillConfigManager.getUseSetting(hero, this, "height", 18, false);
        int heightWithoutBaseBlock = height - 1;

        Block targetBlock = targetLocation.getBlock();
        if (targetLocation.getBlock().getType() != Material.AIR) {
            targetBlock = targetBlock.getRelative(BlockFace.UP);
        }

        OvergrowthConstructionData overgrowthConstructionData = tryGetOvergrowthConstructionData(hero, targetBlock, heightWithoutBaseBlock, radius);
        if (overgrowthConstructionData == null) {
            player.sendMessage("Unable to fit an overgrowth at this location.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        broadcastExecuteText(hero);
        hero.addEffect(new OvergrowthEffect(this, player, duration, overgrowthConstructionData));

        return SkillResult.NORMAL;
    }

    private OvergrowthConstructionData tryGetOvergrowthConstructionData(Hero hero, Block startBlock, int height, int radius) {
        Player player = hero.getPlayer();
        List<Block> conversionBlocks = new ArrayList<Block>();
        List<LivingEntity> targets = new ArrayList<LivingEntity>();

        int requiredUpwardFreeSpace = 3;    // 2 for player + 1 for the block itself
        Block validTopBlock = null;
        Block currentBlock = null;
        BlockIterator iter = null;
        try {
            Vector startCoords = startBlock.getLocation().toVector();
            Vector straightUp = new Vector(0, 1, 0);
            iter = new BlockIterator(startBlock.getWorld(), startCoords, straightUp, 0, height);
        } catch (IllegalStateException e) {
            return null;
        }

        while (iter.hasNext()) {
            currentBlock = iter.next();

            Material currentBlockType = currentBlock.getType();
            if (!currentBlock.isEmpty())
                break;

            boolean hitMaxValidHeight = false;
            for (LivingEntity confirmedTarget : targets) {
                Location theoreticalPlatformLocation = confirmedTarget.getLocation().clone();
                theoreticalPlatformLocation.setY(currentBlock.getY());
                if (cannotGoAnyHigher(theoreticalPlatformLocation.getBlock(), 3)) {
                    hitMaxValidHeight = true;
                }
            }

            Collection<LivingEntity> currentLevelTargets = getLivingEntitiesWithinFlatCircle(currentBlock.getLocation(), radius);
            for (LivingEntity target : currentLevelTargets) {
                if (!hero.isAlliedTo(target) && !damageCheck(player, target)) {
                    hitMaxValidHeight = false;
                    validTopBlock = null;
                    break;
                }
                Location entLoc = target.getLocation();
                Block entBlock = entLoc.getBlock();
                if (cannotGoAnyHigher(entBlock, 3)) {
                    hitMaxValidHeight = true;
                }
                targets.add(target);
            }

            // Never found a valid block
            if (hitMaxValidHeight && validTopBlock == null)
                break;

            for (Block block : getBlocksWithinFlatCircle(currentBlock.getLocation(), radius)) {
                if (block.isEmpty()) {
                    conversionBlocks.add(block);
                }
            }

            validTopBlock = currentBlock;
            if (hitMaxValidHeight)
                break;
        }

        if (validTopBlock == null || validTopBlock == startBlock)
            return null;

        return new OvergrowthConstructionData(startBlock, validTopBlock, radius, conversionBlocks, targets);
    }

//    private static Block safelyIterateFromBlock(Block block, Vector direction, int maxDist, int requiredUpwardFreeSpace) {
//        Block validFinalBlock = null;
//        BlockIterator iter = null;
//        try {
//            iter = new BlockIterator(block.getWorld(), block.getLocation().toVector(), direction, 0, maxDist);
//        } catch (IllegalStateException e) {
//            return null;
//        }
//
//        Block currentBlock;
//        while (iter.hasNext()) {
//            currentBlock = iter.next();
//            Material currentBlockType = currentBlock.getType();
//            if (!currentBlock.isEmpty())
//                break;
//
//            validFinalBlock = currentBlock;
//            if (cannotGoAnyHigher(currentBlock, requiredUpwardFreeSpace))
//                break;
//        }
//        if (validFinalBlock == null)
//            return null;
//        return validFinalBlock;
//    }

    private static boolean cannotGoAnyHigher(Block sourceBlock, int requiredUpwardFreeSpace) {
        boolean cannotGoAnyHigher = false;
        int i = 0;
        Block currentAboveBlock = sourceBlock;
        while (i < requiredUpwardFreeSpace) {
            currentAboveBlock = currentAboveBlock.getRelative(BlockFace.UP);
            if (!Util.transparentBlocks.contains(currentAboveBlock.getType())) {
                cannotGoAnyHigher = true;
                break;
            }
            i++;
        }
        return cannotGoAnyHigher;
    }

    private List<LivingEntity> getLivingEntitiesWithinSphere(Location center, int radius) {
        World world = center.getWorld();
        List<LivingEntity> worldEntities = world.getLivingEntities();
        List<LivingEntity> entitiesWithinRadius = new ArrayList<LivingEntity>();
        List<Block> blocksInRadius = getBlocksWithinSphere(center, radius, false);

        for (LivingEntity entity : worldEntities) {
            Block standingBlock = entity.getLocation().getBlock();
            if (blocksInRadius.contains(standingBlock))
                entitiesWithinRadius.add(entity);
        }
        return entitiesWithinRadius;
    }

    private List<Entity> getEntitiesWithinSphere(Location center, int radius) {
        World world = center.getWorld();
        List<Entity> worldEntities = world.getEntities();
        List<Entity> entitiesWithinRadius = new ArrayList<Entity>();
        List<Block> blocksInRadius = getBlocksWithinSphere(center, radius, false);

        for (Entity entity : worldEntities) {
            Block standingBlock = entity.getLocation().getBlock();
            if (blocksInRadius.contains(standingBlock))
                entitiesWithinRadius.add(entity);
        }
        return entitiesWithinRadius;
    }

    private List<LivingEntity> getLivingEntitiesWithinFlatCircle(Location center, int radius) {
        World world = center.getWorld();
        List<LivingEntity> worldEntities = world.getLivingEntities();
        List<LivingEntity> entitiesWithinRadius = new ArrayList<LivingEntity>();
        List<Block> blocksInRadius = getBlocksWithinFlatCircle(center, radius);

        for (LivingEntity entity : worldEntities) {
            Block standingBlock = entity.getLocation().getBlock();
            if (blocksInRadius.contains(standingBlock))
                entitiesWithinRadius.add(entity);
        }
        return entitiesWithinRadius;
    }

//    private List<Entity> getEntitiesWithinFlatCircle(Location center, int radius) {
//        World world = center.getWorld();
//        List<Entity> worldEntities = world.getEntities();
//        List<Entity> entitiesWithinRadius = new ArrayList<Entity>();
//        List<Block> blocksInRadius = getBlocksWithinFlatCircle(center, radius);
//
//        for (Entity entity : worldEntities) {
//            Block standingBlock = entity.getLocation().getBlock();
//            if (blocksInRadius.contains(standingBlock))
//                entitiesWithinRadius.add(entity);
//        }
//        return entitiesWithinRadius;
//    }

    private List<Block> getBlocksWithinSphere(Location center, int radius, boolean hollow) {
        List<Block> sphereBlocks = new ArrayList<Block>();
        World world = center.getWorld();
        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = centerY - radius; y <= centerY + radius; y++) {
                for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                    double distance = ((centerX - x) * (centerX - x) + ((centerZ - z) * (centerZ - z)) + ((centerY - y) * (centerY - y)));
                    if (distance < radius * radius && !(hollow && distance < ((radius - 1) * (radius - 1)))) {
                        Block block = world.getBlockAt(x, y, z);
                        sphereBlocks.add(block);
                    }
                }
            }
        }
        return sphereBlocks;
    }

    private List<Block> getBlocksWithinFlatCircle(Location center, int radius) {
        List<Block> flatCircleBlocks = new ArrayList<Block>();
        World world = center.getWorld();
        int centerY = center.getBlockY();
        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();
        int rSquared = radius * radius;

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                if ((centerX - x) * (centerX - x) + (centerZ - z) * (centerZ - z) <= rSquared) {
                    Block block = world.getBlockAt(x, centerY, z);
                    flatCircleBlocks.add(block);
                }
            }
        }
        return flatCircleBlocks;
    }

    private boolean isBlockWithinFlatCircle(Block block, Location center, int radius) {
        int centerY = center.getBlockY();
        if (block.getY() != centerY)
            return false;

        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();
        int rSquared = radius * radius;

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                if ((centerX - x) * (centerX - x) + (centerZ - z) * (centerZ - z) <= rSquared) {
                    if (block.getX() == x && block.getZ() == z)
                        return true;
                }
            }
        }
        return false;
    }

    private class OvergrowthConstructionData {
        Block bottomCenterBlock;
        Block topCenterBlock;
        int radius;
        List<Block> possibleConversionBlocks = new ArrayList<Block>();
        List<LivingEntity> targets = new ArrayList<LivingEntity>();

        OvergrowthConstructionData(Block bottomCenterBlock, Block topCenterBlock, int radius, List<Block> possibleConversionBlocks, List<LivingEntity> targets) {
            this.bottomCenterBlock = bottomCenterBlock;
            this.topCenterBlock = topCenterBlock;
            this.radius = radius;
            this.possibleConversionBlocks = possibleConversionBlocks;
            this.targets = targets;
        }

        int getTopY() {
            return topCenterBlock.getY();
        }

        int getBottomY() {
            return bottomCenterBlock.getY();
        }

        int getDistance() {
            return getTopY() - getBottomY();
        }

        Location getCenterFor(Block block) {
            return new Location(block.getWorld(), bottomCenterBlock.getX(), block.getY(), bottomCenterBlock.getZ());
        }
    }

    public class OvergrowthEffect extends ExpirableEffect {
        private final OvergrowthConstructionData data;

        private Set<Block> changedBlocks = new HashSet<Block>();
        private SkillBlockListener listener = new SkillBlockListener();

        OvergrowthEffect(Skill skill, Player applier, long duration, OvergrowthConstructionData data) {
            super(skill, "Overgrowth", applier, duration);
            this.data = data;

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.EARTH);
            types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Bukkit.getServer().getPluginManager().registerEvents(listener, plugin);
            Player player = hero.getPlayer();
            createPlatformAndBoostEntitiesUp();
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            Location endLocation = data.topCenterBlock.getLocation();

            Collection<Entity> nearbyEntities = endLocation.getWorld().getNearbyEntities(endLocation, data.radius, 3, data.radius);
            for (Entity entity : nearbyEntities) {
                if (!(entity instanceof LivingEntity))
                    continue;
                Location entLoc = entity.getLocation();
                Location inAirLoc = new Location(entLoc.getWorld(), entLoc.getBlockX(), entLoc.getBlockY() + 1, entLoc.getBlockZ(), entLoc.getYaw(), entLoc.getPitch());
                entity.teleport(inAirLoc, PlayerTeleportEvent.TeleportCause.PLUGIN);
                entity.setFallDistance(-512);
                CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter((LivingEntity)entity);
                int duration = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DURATION, 6000, false);
                targetCT.addEffect(new SafeFallEffect(skill, player, duration));
            }
            revertBlocks();
        }

        private void createPlatformAndBoostEntitiesUp() {
            for (Block block : data.possibleConversionBlocks) {
                boolean isTopLevel = block.getY() == data.getTopY();
                boolean isUpperLevel = block.getY() >= (data.getTopY() - (data.getDistance() * 0.15d));

                if (isTopLevel) {
                    block.setType(Material.OAK_LEAVES);
                } else {
                    int chance = random.nextInt(100);
                    if (chance >= 20) {
                        if (isUpperLevel) {
                            block.setType(Material.OAK_LEAVES);
                        } else if (isBlockWithinFlatCircle(block, data.getCenterFor(block), (int) (data.radius * 0.60))) {
                            block.setType(Material.OAK_WOOD);
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                }
                changedBlocks.add(block);
            }

            for (LivingEntity entity : data.targets) {
                Location entLoc = entity.getLocation();
                Location onPillarLoc = new Location(entLoc.getWorld(), entLoc.getX(), data.getTopY() + 1, entLoc.getZ(), entLoc.getYaw(), entLoc.getPitch());
                entity.teleport(onPillarLoc, PlayerTeleportEvent.TeleportCause.PLUGIN);
                if (entity instanceof Player) {
                    entity.sendMessage(ChatComponents.GENERIC_SKILL + ChatColor.GOLD + "You've been transported onto an Overgrowth! Stay on the tree to avoid fall damage!");
                }
            }
        }

        private void revertBlocks() {
            try {
                for (Block block : changedBlocks) {
                    block.setType(Material.AIR);
                }
            } catch (Exception e) {
                // Eat the exception
            }
            changedBlocks.clear();
            HandlerList.unregisterAll(listener);
        }

        public class SkillBlockListener implements Listener {

            @EventHandler(priority = EventPriority.LOWEST)
            public void onPluginDisable(PluginDisableEvent e) {
                if (e.getPlugin() != plugin)
                    return;
                revertBlocks();
            }

            // Possible other events to register if we feel the need...
            //BlockFormEvent
            //BlockPhysicsEvent

            @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
            public void onLeafDecay(LeavesDecayEvent event) {
                Block block = event.getBlock();
                if (block == null)
                    return;

                if (changedBlocks.contains(block))
                    event.setCancelled(true);
            }

            // For stopping paved grass / crops being destroyed
            @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
            public void onBlockFade(BlockFadeEvent event) {
                Block block = event.getBlock();
                if (block == null)
                    return;

                Block above = block.getRelative(BlockFace.UP);
                if (changedBlocks.contains(block) || changedBlocks.contains(above))
                    event.setCancelled(true);
            }

            @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
            public void onBlockBurn(BlockBurnEvent event) {
                Block block = event.getBlock();
                if (changedBlocks.contains(block))
                    event.setCancelled(true);
            }

            @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
            public void onBlockSpread(BlockSpreadEvent event) {
                Block block = event.getBlock();
                Block sourceBlock = event.getSource();
                if (changedBlocks.contains(block) || changedBlocks.contains(sourceBlock))
                    event.setCancelled(true);
            }

//            @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
//            public void onBlockPlace(BlockPlaceEvent event) {
//                Block block = event.getBlock();
//                if (changedBlocks.contains(block))
//                    event.setCancelled(true);
//            }

            @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
            public void onBlockBreak(BlockBreakEvent event) {
                Block block = event.getBlock();
                if (changedBlocks.contains(block))
                    event.setCancelled(true);
            }

            @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
            public void onBlockFromTo(BlockFromToEvent event) {
                Block fromBlock = event.getBlock();
                Block toBlock = event.getToBlock();
                if (changedBlocks.contains(toBlock) || changedBlocks.contains(fromBlock))
                    event.setCancelled(true);
            }

            @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
            public void onBlockDamage(BlockDamageEvent event) {
                Block block = event.getBlock();
                if (changedBlocks.contains(block))
                    event.setCancelled(true);
            }

            @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
            public void onEntityChangeBlock(EntityChangeBlockEvent event) {
                Block block = event.getBlock();
                if (changedBlocks.contains(block))
                    event.setCancelled(true);
            }

            @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
            public void onHangingBreak(HangingBreakEvent event) {
                Block block = event.getEntity().getLocation().getBlock();
                BoundingBox box = event.getEntity().getBoundingBox();

                if (changedBlocks.contains(block)
                        || changedBlocks.stream().anyMatch((changedBlock)-> changedBlock.getBoundingBox().contains(box))
                        || changedBlocks.stream().anyMatch((changedBlock)-> box.contains(changedBlock.getBoundingBox()))) {
                    event.setCancelled(true);
                }
            }

            @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
            public void onBlockPistonExtend(BlockPistonExtendEvent event) {
                if (event.getBlocks().stream().anyMatch(changedBlocks::contains))
                    event.setCancelled(true);
            }

            @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
            public void onBlockPistonRetract(BlockPistonRetractEvent event) {
                if (event.getBlocks().stream().anyMatch(changedBlocks::contains))
                    event.setCancelled(true);
            }
        }
    }
}
