package com.herocraftonline.heroes.characters.skill.reborn;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.nms.physics.NMSPhysics;
import com.herocraftonline.heroes.nms.physics.RayCastHit;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.*;

public class SkillOvergrowth extends ActiveSkill {

    private static final Random random = new Random(System.currentTimeMillis());
    private final NMSPhysics physics = NMSPhysics.instance();
    private String applyText;
    private String expireText;

    public SkillOvergrowth(Heroes plugin) {
        super(plugin, "Overgrowth");
        setDescription("Create a large overgrowth up to $1 blocks in front of you that is $2 blocks wide and $3 blocks tall.");
        setUsage("/skill overgrowth");
        setArgumentRange(0, 0);
        setIdentifiers("skill overgrowth");
        setTypes(SkillType.ABILITY_PROPERTY_EARTH, SkillType.SILENCEABLE, SkillType.BLOCK_CREATING);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("height", 18);
        node.set(SkillSetting.RADIUS.node(), 3);
        node.set(SkillSetting.MAX_DISTANCE.node(), 12);
        node.set(SkillSetting.DURATION.node(), 7500);
//        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% conjures a wall of Water!");
//        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero%'s wall has crumbled");

        return node;
    }

    public String getDescription(Hero hero) {
        //int height = SkillConfigManager.getUseSetting(hero, this, "height", 3, false) * 2;
        //int width = SkillConfigManager.getUseSetting(hero, this, "width", 2, false) * 2;
//        int maxDist = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 12, false);

        return getDescription();//.replace("$1", maxDist + "");//.replace("$2", width + "").replace("$3", height + "");
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% creates an overgrowth!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero%'s overgrowth has vanished").replace("%hero%", "$1");
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        World world = player.getWorld();

        int maxDist = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 12, false);
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 4, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 7500, false);
        int height = SkillConfigManager.getUseSetting(hero, this, "height", 18, false);
        int offsetHeight = height - 1;

        Block targetBlock = GetBlockViaRaycast(player, maxDist);
        if (targetBlock == null) {
            player.sendMessage("Unable to find a valid block for the bottom of the overgrowth pillar.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        Block pillarRootBlock;
        if (targetBlock.isEmpty()) {
            targetBlock = GetBlockViaRaycast(targetBlock, new Vector(0, -1, 0), offsetHeight);
            if (targetBlock == null) {
                player.sendMessage("Failing to shoot downwards properly.");
                return SkillResult.INVALID_TARGET_NO_MSG;
            }
            pillarRootBlock = targetBlock.getRelative(BlockFace.UP);
            if (pillarRootBlock.getRelative(BlockFace.DOWN).isEmpty())
                pillarRootBlock = null;
        } else {
            pillarRootBlock = targetBlock.getRelative(BlockFace.UP);
        }

        if (pillarRootBlock == null || !Util.transparentBlocks.contains(pillarRootBlock.getType())) {
            player.sendMessage("Unable to find a valid block for the bottom of the overgrowth pillar.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        Block platformCenterBlock = SafelyIterateFromBlock(pillarRootBlock, new Vector(0, 1, 0), offsetHeight, 3);
        if (platformCenterBlock == null || platformCenterBlock.getY() <= pillarRootBlock.getLocation().getY()) {
            player.sendMessage("Unable to fit an overgrowth at this location.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        hero.addEffect(new OvergrowthEffect(this, player, duration, pillarRootBlock, platformCenterBlock, radius));

        return SkillResult.NORMAL;
    }

    private String getLocationCoordsString(Location location) {
        return "(" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ")";
    }

    private String getVectorCoordsString(Vector vector) {
        return "(" + (int) vector.getX() + ", " + (int) vector.getY() + ", " + (int) vector.getZ() + ")";
    }

    private Block GetBlockViaRaycast(Player player, int maxDist) {
        World world = player.getWorld();
        Location eyeLocation = player.getEyeLocation();
        Vector normal = eyeLocation.getDirection();
        Vector start = eyeLocation.toVector();
        Vector end = normal.clone().multiply(maxDist).add(start);
        RayCastHit hit = physics.rayCast(world, player, start, end);
        if (hit == null)
            return world.getBlockAt(end.getBlockX(), end.getBlockY(), end.getBlockZ());
        return hit.isEntity() ? hit.getEntity().getLocation().getBlock() : hit.getBlock(world);
    }

    private Block GetBlockViaRaycast(Block castLocation, Vector direction, int maxDist) {
        World world = castLocation.getWorld();
        Vector start = castLocation.getLocation().toVector();
        Vector end = direction.clone().multiply(maxDist).add(start);
        RayCastHit hit = physics.rayCast(world, start, end);
        if (hit == null)
            return world.getBlockAt(end.getBlockX(), end.getBlockY(), end.getBlockZ());
        return hit.isEntity() ? hit.getEntity().getLocation().getBlock() : hit.getBlock(world);
    }


//    private RayCastHit RayCastFromBlock(Player player, Block startBlock, Vector direction, int maxDist) {
//        World world = startBlock.getWorld();
//        Vector start = startBlock.getLocation().toVector();
//        Vector end = direction.clone().multiply(maxDist).add(start);
//        //player.sendMessage("RayCastFromBlock:");
//        //player.sendMessage("start: " + getVectorCoordsString(start));
//        //player.sendMessage("end: " + getVectorCoordsString(start));
//        return physics.rayCastBlock(startBlock, start, end);
//    }

//    private Block GetBlockViaRaycast(Player player, int maxDist) {
//
//        Block validFinalBlock = null;
//        Block currentBlock;
//        BlockIterator iter = null;
//        try {
//            iter = new BlockIterator(player, maxDist);
//        } catch (IllegalStateException e) {
//            player.sendMessage("There was an error getting your overgrowth location!");
//            return null;
//        }
//
//        while (iter.hasNext()) {
//            currentBlock = iter.next();
//            Material currentBlockType = currentBlock.getType();
//
//            if (!Util.transparentBlocks.contains(currentBlockType))
//                break;
//            if (Util.transparentBlocks.contains(currentBlock.getRelative(BlockFace.UP).getType()))
//                validFinalBlock = currentBlock;
//        }
//        if (validFinalBlock == null) {
//            player.sendMessage("Could not find a valid location to create an overgrowth.");
//            return null;
//        }
//        return validFinalBlock;
//    }

    private Block SafelyIterateFromBlock(Block block, Vector direction, int maxDist, int requiredUpwardFreeSpace) {
        Block validFinalBlock = null;
        BlockIterator iter = null;
        try {
            iter = new BlockIterator(block.getWorld(), block.getLocation().toVector(), direction, 0, maxDist);
        } catch (IllegalStateException e) {
            return null;
        }

        Block currentBlock;
        while (iter.hasNext()) {
            currentBlock = iter.next();
            Material currentBlockType = currentBlock.getType();
            if (!currentBlock.isEmpty())
                break;

            validFinalBlock = currentBlock;
            boolean cannotGoAnyHigher = false;

            int i = 0;
            Block currentAboveBlock = currentBlock;
            while (i < requiredUpwardFreeSpace) {
                currentAboveBlock = currentAboveBlock.getRelative(BlockFace.UP);
                if (!Util.transparentBlocks.contains(currentAboveBlock.getType())) {
                    cannotGoAnyHigher = true;
                    break;
                }
                i++;
            }
            if (cannotGoAnyHigher)
                break;
        }
        if (validFinalBlock == null)
            return null;
        return validFinalBlock;
    }

    public class OvergrowthEffect extends ExpirableEffect {

        private final Block startBlock;
        private final Block platformTopBlock;
        private final int radius;

        private Set<Block> changedBlocks = new HashSet<Block>();
        private SkillBlockListener listener = new SkillBlockListener();

        public OvergrowthEffect(Skill skill, Player applier, long duration, Block startBlock, Block platformTopBlock, int radius) {
            super(skill, "Overgrowth", applier, duration);
            this.startBlock = startBlock;
            this.platformTopBlock = platformTopBlock;
            this.radius = radius;

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
            Location startLocation = startBlock.getLocation();
            Location endLocation = platformTopBlock.getLocation();

            Collection<Entity> nearbyEntities = endLocation.getWorld().getNearbyEntities(endLocation, radius, 3, radius);
            for (Entity entity : nearbyEntities) {
                if (!(entity instanceof LivingEntity))
                    continue;
                Location entLoc = entity.getLocation();
                Location inAirLoc = new Location(entLoc.getWorld(), entLoc.getX(), entLoc.getY() + 1, entLoc.getZ(), entLoc.getYaw(), entLoc.getPitch());
                entity.teleport(inAirLoc);
                entity.setFallDistance(-512);
            }
            revertBlocks();
        }

        private void createPlatformAndBoostEntitiesUp() {
            BlockIterator iter = null;
            Location startLocation = startBlock.getLocation();
            Location endLocation = platformTopBlock.getLocation();
            int topYValue = platformTopBlock.getY();
            int maxDist = (int) (endLocation.getY() - startLocation.getY());
            try {
                iter = new BlockIterator(startLocation.getWorld(), startLocation.toVector(), new Vector(0, 1, 0), 0, maxDist);
            } catch (IllegalStateException e) {
                return;
            }

            World world = startLocation.getWorld();
            Block currentBlock;
            while (iter.hasNext()) {
                currentBlock = iter.next();
                Material currentBlockType = currentBlock.getType();
                if (!currentBlock.isEmpty())
                    break;

                Set<BoundingBox> blocksToIgnore = new HashSet<>();
                Collection<Entity> nearbyEntities = getEntitiesWithinSphere(currentBlock.getLocation(), radius);
                for (Entity entity : nearbyEntities) {
                    if ((entity instanceof LivingEntity)) {
                        Location entLoc = entity.getLocation();
                        Location onPillarLoc = new Location(entLoc.getWorld(), entLoc.getX(), topYValue + 1, entLoc.getZ(), entLoc.getYaw(), entLoc.getPitch());
                        entity.teleport(onPillarLoc);
                        //teleportedEntitites.add((LivingEntity) entity);
                    } else if (entity instanceof Hanging) {
                        blocksToIgnore.add(entity.getBoundingBox());
                    }
                }

                boolean isTopLevel = currentBlock.getY() == topYValue;
                boolean isUpperLevel = currentBlock.getY() >= (topYValue - (maxDist * 0.15d));
                List<Block> blocks;
                if (isUpperLevel)
                    blocks = getBlocksInFlatCircle(currentBlock.getLocation(), radius);
                else
                    blocks = getBlocksInFlatCircle(currentBlock.getLocation(), (int) (radius * 0.60));

                for (Block block : blocks) {
                    if (!block.isEmpty() || blocksToIgnore.stream().anyMatch(x -> x.contains(block.getX(), block.getY(), block.getZ())))
                        continue;

                    changedBlocks.add(block);
                    if (isTopLevel) {
                        block.setType(Material.OAK_LEAVES);
                    } else {
                        int chance = random.nextInt(100);
                        if (chance >= 20) {
                            if (isUpperLevel)
                                block.setType(Material.OAK_LEAVES);
                            else
                                block.setType(Material.OAK_WOOD);
                        }
                    }
                }
            }
        }

        private List<Entity> getEntitiesWithinSphere(Location centerLocation, int radius) {
            World world = centerLocation.getWorld();
            List<Entity> worldEntities =world.getEntities();
            List<Entity> entitiesWithinSphere = new ArrayList<Entity>();
            List<Block> blocksInSphereRadius = getBlocksInSphere(centerLocation, radius, false);

            for (Entity entity : worldEntities) {
                Block standingBlock = entity.getLocation().getBlock();
                if (blocksInSphereRadius.contains(standingBlock))
                    entitiesWithinSphere.add(entity);
            }
            return entitiesWithinSphere;
        }

        private List<Block> getBlocksInSphere(Location centerLocation, int radius, boolean hollow) {
            List<Block> sphereBlocks = new ArrayList<Block>();
            World world = centerLocation.getWorld();
            int centerX = centerLocation.getBlockX();
            int centerY = centerLocation.getBlockY();
            int centerZ = centerLocation.getBlockZ();

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

        private List<Block> getBlocksInFlatCircle(Location location, int radius) {
            List<Block> flatCircleBlocks = new ArrayList<Block>();
            World world = location.getWorld();
            int centerY = location.getBlockY();
            int centerX = location.getBlockX();
            int centerZ = location.getBlockZ();
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
            @EventHandler(priority = EventPriority.MONITOR)
            public void onPluginDisable(PluginDisableEvent e) {
                if (e.getPlugin() != plugin)
                    return;
                revertBlocks();
            }

            // Possible other events to register if we feel the need...
            //BlockFormEvent
            //LeavesDecayEvent
            //BlockPhysicsEvent

            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void onBlockFade(BlockFadeEvent event) {
                Block block = event.getBlock();
                if (block == null)
                    return;

                Block above = block.getRelative(BlockFace.UP);
                if (changedBlocks.contains(block) || changedBlocks.contains(above))
                    event.setCancelled(true);
            }

            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void onBlockBurn(BlockBurnEvent event) {
                Block block = event.getBlock();
                if (changedBlocks.contains(block))
                    event.setCancelled(true);
            }

            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void onBlockSpread(BlockSpreadEvent event) {
                Block block = event.getBlock();
                Block sourceBlock = event.getSource();
                if (changedBlocks.contains(block) || changedBlocks.contains(sourceBlock))
                    event.setCancelled(true);
            }

            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void onBlockPlace(BlockPlaceEvent event) {
                Block block = event.getBlock();
                if (changedBlocks.contains(block))
                    event.setCancelled(true);
            }

            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void onBlockBreak(BlockBreakEvent event) {
                Block block = event.getBlock();
                if (changedBlocks.contains(block))
                    event.setCancelled(true);
            }

            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void onBlockFromTo(BlockFromToEvent event) {
                Block fromBlock = event.getBlock();
                Block toBlock = event.getToBlock();
                if (changedBlocks.contains(toBlock) || changedBlocks.contains(fromBlock))
                    event.setCancelled(true);
            }

            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void onBlockDamage(BlockDamageEvent event) {
                Block block = event.getBlock();
                if (changedBlocks.contains(block))
                    event.setCancelled(true);
            }

            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void onEntityChangeBlock(EntityChangeBlockEvent event) {
                Block block = event.getBlock();
                if (changedBlocks.contains(block))
                    event.setCancelled(true);
            }

            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void onHangingBreak(HangingBreakEvent event) {
                Block block = event.getEntity().getLocation().getBlock();
                if (block != null && changedBlocks.contains(block))
                    event.setCancelled(true);
            }

            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void onBlockPistonExtend(BlockPistonExtendEvent event) {
                if (event.getBlocks().stream().anyMatch(changedBlocks::contains))
                    event.setCancelled(true);
            }

            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void onBlockPistonRetract(BlockPistonRetractEvent event) {
                if (event.getBlocks().stream().anyMatch(changedBlocks::contains))
                    event.setCancelled(true);
            }
        }
    }
}
