//package com.herocraftonline.heroes.characters.skill.reborn.unused;
//
//import com.herocraftonline.heroes.Heroes;
//import com.herocraftonline.heroes.api.SkillResult;
//import com.herocraftonline.heroes.characters.CharacterTemplate;
//import com.herocraftonline.heroes.characters.Hero;
//import com.herocraftonline.heroes.characters.Monster;
//import com.herocraftonline.heroes.characters.effects.EffectType;
//import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
//import com.herocraftonline.heroes.characters.skill.*;
//import com.herocraftonline.heroes.util.Util;
//import org.bukkit.Bukkit;
//import org.bukkit.Location;
//import org.bukkit.Material;
//import org.bukkit.World;
//import org.bukkit.block.Block;
//import org.bukkit.block.BlockFace;
//import org.bukkit.configuration.ConfigurationSection;
//import org.bukkit.entity.Entity;
//import org.bukkit.entity.LivingEntity;
//import org.bukkit.entity.Player;
//import org.bukkit.event.EventHandler;
//import org.bukkit.event.EventPriority;
//import org.bukkit.event.HandlerList;
//import org.bukkit.event.Listener;
//import org.bukkit.event.block.*;
//import org.bukkit.event.hanging.HangingBreakEvent;
//import org.bukkit.event.server.PluginDisableEvent;
//import org.bukkit.material.Attachable;
//import org.bukkit.util.BlockIterator;
//import org.bukkit.util.BoundingBox;
//import org.bukkit.util.Vector;
//import org.jetbrains.annotations.NotNull;
//
//import java.util.*;
//
//public class SkillEntomb extends TargettedSkill {
//    private static final Random random = new Random(System.currentTimeMillis());
//    private static final List<Material> blockTypes = Arrays.asList(
//        Material.STONE,
////        Material.COARSE_DIRT,
//        Material.DIRT,
////        Material.GRASS_BLOCK,
////        Material.ANDESITE,
////        Material.DIORITE,
////        Material.GRANITE
//    );
//
//    private String applyText;
//    private String expireText;
//
//    public SkillEntomb(Heroes plugin) {
//        super(plugin, "Entomb");
//        setDescription("Attempt to Entomb a player for $1 second(s).");
//        setUsage("/skill entomb");
//        setArgumentRange(0, 0);
//        setIdentifiers("skill entomb");
//        setTypes(SkillType.ABILITY_PROPERTY_EARTH, SkillType.SILENCEABLE, SkillType.BLOCK_CREATING, SkillType.MULTI_GRESSIVE);
//    }
//
//    public ConfigurationSection getDefaultConfig() {
//        ConfigurationSection node = super.getDefaultConfig();
//
//        node.set(SkillSetting.RADIUS.node(), 3);
//        node.set(SkillSetting.MAX_DISTANCE.node(), 12);
//        node.set(SkillSetting.DURATION.node(), 5000);
////        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% conjures a wall of Water!");
////        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero%'s wall has crumbled");
//
//        return node;
//    }
//
//    public String getDescription(Hero hero) {
//        //int height = SkillConfigManager.getUseSetting(hero, this, "height", 3, false) * 2;
//        //int width = SkillConfigManager.getUseSetting(hero, this, "width", 2, false) * 2;
////        int maxDist = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 12, false);
//
//        return getDescription();//.replace("$1", maxDist + "");//.replace("$2", width + "").replace("$3", height + "");
//    }
//
//    //@Override
////    public void init() {
////        super.init();
////
////        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% creates an entomb!").replace("%hero%", "$1");
////        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero%'s entomb has vanished").replace("%hero%", "$1");
////    }
//
//    @Override
//    public SkillResult use(Hero hero, LivingEntity targetEnt, String[] strings) {
//        Player player = hero.getPlayer();
//        World world = player.getWorld();
//
//        int maxDist = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 12, false);
//        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 4, false);
//        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
//
//        if (!(damageCheck(player, targetEnt)))
//            return SkillResult.INVALID_TARGET_NO_MSG;
//
//        ExpirableEffect effect = new EntombedEffect(this, player, duration, radius);
//        CharacterTemplate character = plugin.getCharacterManager().getCharacter(targetEnt);
//        character.addEffect(effect);
//
//        return SkillResult.NORMAL;
//    }
//
//    public class EntombedEffect extends ExpirableEffect {
//        private final int radius;
//
//        private Set<Block> blocksToLock = new HashSet<Block>();
//        private Set<Block> changedBlocks = new HashSet<Block>();
//        private SkillBlockListener listener = new SkillBlockListener();
//
//        public EntombedEffect(Skill skill, Player applier, long duration, int radius) {
//            super(skill, "Entombed", applier, duration);
//            this.radius = radius;
//
//            types.add(EffectType.HARMFUL);
//            types.add(EffectType.EARTH);
//            types.add(EffectType.MAGIC);
//        }
//
//        public void applyToMonster(Monster monster) {
//            super.applyToMonster(monster);
//            Bukkit.getServer().getPluginManager().registerEvents(listener, plugin);
//            EntombTarget(monster.getEntity());
//        }
//
//        public void applyToHero(Hero hero) {
//            super.applyToHero(hero);
//            Bukkit.getServer().getPluginManager().registerEvents(listener, plugin);
//            EntombTarget(hero.getPlayer());
//        }
//
//        private void EntombTarget(LivingEntity origignalTarget) {
//            Location location = origignalTarget.getLocation();
//            World world = location.getWorld();
//            Set<BoundingBox> hangingBlockBoxes = new HashSet<>();
//            Collection<Entity> nearbyEntities = world.getNearbyEntities(location, radius, radius, radius);
//            for(Entity entity : nearbyEntities) {
//                if (entity == origignalTarget)
//                    continue;
//                if (entity instanceof Attachable)
//                    hangingBlockBoxes.add(entity.getBoundingBox());
//            }
//
//            List<Block> sphereBlocks = getBlocksInSphere(location, radius, true);
//            for (Block block : sphereBlocks) {
//                blocksToLock.add(block);
//                if (!block.isEmpty() || hangingBlockBoxes.stream().anyMatch(x -> x.contains(block.getX(), block.getY(), block.getZ())))
//                    continue;
//
//                int randomIndex = random.nextInt(blockTypes.size() - 1);
//                block.setType(blockTypes.get(randomIndex));
//                changedBlocks.add(block);
//            }
//        }
//
//        @Override
//        public void removeFromMonster(Monster monster) {
//            super.removeFromMonster(monster);
//            revertBlocks();
//        }
//
//        @Override
//        public void removeFromHero(Hero hero) {
//            super.removeFromHero(hero);
//            revertBlocks();
//        }
//
//        private List<Block> getBlocksInSphere(Location centerLocation, int radius, boolean hollow) {
//            List<Block> circleBlocks = new ArrayList<Block>();
//            World world = centerLocation.getWorld();
//            int bx = centerLocation.getBlockX();
//            int by = centerLocation.getBlockY();
//            int bz = centerLocation.getBlockZ();
//
//            for (int x = bx - radius; x <= bx + radius; x++) {
//                for (int y = by - radius; y <= by + radius; y++) {
//                    for (int z = bz - radius; z <= bz + radius; z++) {
//                        double distance = ((bx - x) * (bx - x) + ((bz - z) * (bz - z)) + ((by - y) * (by - y)));
//                        if (distance < radius * radius && !(hollow && distance < ((radius - 1) * (radius - 1)))) {
//                            Block block = world.getBlockAt(x, y, z);
//                            circleBlocks.add(block);
//                        }
//                    }
//                }
//            }
//            return circleBlocks;
//        }
//
//        private Block SafelyIterateFromBlock(Block block, Vector direction, int maxDist, int requiredUpwardFreeSpace) {
//            Block validFinalBlock = null;
//            BlockIterator iter = null;
//            try {
//                iter = new BlockIterator(block.getWorld(), block.getLocation().toVector(), direction, 0, maxDist);
//            } catch (IllegalStateException e) {
//                return null;
//            }
//
//            Block currentBlock;
//            while (iter.hasNext()) {
//                currentBlock = iter.next();
//                Material currentBlockType = currentBlock.getType();
//                if (!currentBlock.isEmpty())
//                    break;
//
//                validFinalBlock = currentBlock;
//                boolean cannotGoAnyHigher = false;
//
//                int i = 0;
//                Block currentAboveBlock = currentBlock;
//                while (i < requiredUpwardFreeSpace) {
//                    currentAboveBlock = currentAboveBlock.getRelative(BlockFace.UP);
//                    if (!Util.transparentBlocks.contains(currentAboveBlock.getType())) {
//                        cannotGoAnyHigher = true;
//                        break;
//                    }
//                    i++;
//                }
//                if (cannotGoAnyHigher)
//                    break;
//            }
//            if (validFinalBlock == null)
//                return null;
//            return validFinalBlock;
//        }
//
//        private void revertBlocks() {
//            try {
//                for (Block block : changedBlocks) {
//                    block.setType(Material.AIR);
//                }
//            } catch(Exception e) {
//                // Eat the exception
//            }
//            changedBlocks.clear();
//            blocksToLock.clear();
//            HandlerList.unregisterAll(listener);
//        }
//
//        public class SkillBlockListener implements Listener {
//            @EventHandler(priority = EventPriority.MONITOR)
//            public void onPluginDisable(PluginDisableEvent e) {
//                if (e.getPlugin() != plugin) {
//                    return;
//                }
//                revertBlocks();
//            }
//
//            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
//            public void onBlockPlace(BlockSpreadEvent event) {
//                Block block = event.getBlock();
//                if (block != null && blocksToLock.contains(block)) {
//                    event.setCancelled(true);
//                }
//            }
//
//            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
//            public void onBlockBreak(BlockBreakEvent event) {
//                Block block = event.getBlock();
//                if (block != null && blocksToLock.contains(block)) {
//                    event.setCancelled(true);
//                }
//            }
//
//            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
//            public void onBlockFromTo(BlockFromToEvent event) {
//                Block fromBlock = event.getBlock();
//                Block toBlock = event.getToBlock();
//                if (blocksToLock.contains(toBlock) || blocksToLock.contains(fromBlock))
//                    event.setCancelled(true);
//            }
//
//            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
//            public void onBlockPistonRetract(BlockPistonRetractEvent event) {
//                if (event.getBlocks().stream().anyMatch(blocksToLock::contains))
//                    event.setCancelled(true);
//            }
//
//            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
//            public void onHangingBreak(HangingBreakEvent event) {
//                Block block = event.getEntity().getLocation().getBlock();
//                if (block != null && blocksToLock.contains(block))
//                    event.setCancelled(true);
//            }
//
//            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
//            public void onBlockPistonExtend(BlockPistonExtendEvent event) {
//                if (event.getBlocks().stream().anyMatch(blocksToLock::contains))
//                    event.setCancelled(true);
//            }
//        }
//    }
//}
