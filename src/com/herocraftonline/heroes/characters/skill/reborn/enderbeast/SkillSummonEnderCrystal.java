package com.herocraftonline.heroes.characters.skill.reborn.enderbeast;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.nms.physics.NMSPhysics;
import com.herocraftonline.heroes.nms.physics.RayCastHit;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;

public class SkillSummonEnderCrystal extends ActiveSkill {

    private final NMSPhysics physics = NMSPhysics.instance();
    private static Set<SkillConstructionData> activeConstructions = new HashSet<SkillConstructionData>();

    public SkillSummonEnderCrystal(Heroes plugin) {
        super(plugin, "SummonEnderCrystal");
        setDescription("Summon an EnderCrystal to help sustain your existence while transformed. "
                + "The crystal will remain for $1 seconds. "
                + "Has no notable effects while in your human form.");
        setUsage("/skill summonendercrystal");
        setArgumentRange(0, 0);
        setIdentifiers("skill summonendercrystal", "skill endercrystal");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE, SkillType.BLOCK_CREATING, SkillType.HEALING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillBlockListener(), plugin);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("targetting-distance", 5);
        config.set(SkillSetting.DURATION.node(), 20000);
        config.set(SkillSetting.PERIOD.node(), 500);
        config.set("crystal-height", 2);
        config.set("heal-distance", 20);
        config.set("heal-tick", 20.0D);
        return config;
    }

    public String getDescription(Hero hero) {
        //int height = SkillConfigManager.getUseSetting(hero, this, "height", 3, false) * 2;
        //int width = SkillConfigManager.getUseSetting(hero, this, "width", 2, false) * 2;
//        int maxDist = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 12, false);

        return getDescription();//.replace("$1", maxDist + "");//.replace("$2", width + "").replace("$3", height + "");
    }

    @Override
    public SkillResult use(Hero hero, String[] strings) {
        Player player = hero.getPlayer();
        World world = player.getWorld();

        if (hero.hasEffect("HasSummonedEnderCrystal")) {
            hero.removeEffect(hero.getEffect("HasSummonedEnderCrystal"));
            player.sendMessage("You dispel your Ender Crystal.");
            return SkillResult.SKIP_POST_USAGE;
        }

        int maxDist = SkillConfigManager.getUseSetting(hero, this, "targetting-distance", 5, false);
        int healDist = SkillConfigManager.getUseSetting(hero, this, "heal-distance", 20, false);
        double healAmount = SkillConfigManager.getUseSetting(hero, this, "heal-tick", 20.0D, false);
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 400, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 20000, false);
        int height = SkillConfigManager.getUseSetting(hero, this, "crystal-height", 2, false);
        int heightWithoutBaseBlock = height - 1;

        Block targettedBlock = getTargetBlockViaRaycast(player, maxDist);
        if (targettedBlock == null)
            return invalidTargetWithMessage(player);

        Block baseSkillBlock = getBaseSkillBlock(player, targettedBlock, heightWithoutBaseBlock);
        if (baseSkillBlock == null)
            return invalidTargetWithMessage(player);

        SkillConstructionData constructionData = getConstructionData(player, baseSkillBlock, height);
        if (constructionData == null)
            return invalidTargetWithMessage(player);

        ExpirableEffect effect = new EnderCrystaledEffect(this, player, period, duration, constructionData, healAmount, healDist);
        hero.addEffect(effect);

        return SkillResult.NORMAL;
    }

    private Block getBaseSkillBlock(Player player, Block targetBlock, int height) {
        Block pillarRootBlock;
        if (targetBlock.isEmpty()) {
            targetBlock = getTargetBlockViaRaycast(targetBlock, new Vector(0, -1, 0), height);
            if (targetBlock == null)
                return null;

            pillarRootBlock = targetBlock.getRelative(BlockFace.UP);
            if (pillarRootBlock.getRelative(BlockFace.DOWN).isEmpty())
                pillarRootBlock = null;
        } else {
            pillarRootBlock = targetBlock.getRelative(BlockFace.UP);
        }

        if (pillarRootBlock == null || !pillarRootBlock.isEmpty())
            return null;
        return pillarRootBlock;
    }

    private SkillConstructionData getConstructionData(Player player, Block startBlock, int height) {
        List<Block> constructionBlocks = new ArrayList<Block>();

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

        boolean cantFit = false;
        while (iter.hasNext()) {
            currentBlock = iter.next();

            Material currentBlockType = currentBlock.getType();
            if (!currentBlock.isEmpty()) {
                cantFit = true;
                break;
            }

            constructionBlocks.add(currentBlock);
        }

        if (cantFit || constructionBlocks.isEmpty())
            return null;

        return new SkillConstructionData(constructionBlocks);
    }

    private Block getTargetBlockViaRaycast(Player player, int maxDist) {
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

    private Block getTargetBlockViaRaycast(Block castLocation, Vector direction, int maxDist) {
        World world = castLocation.getWorld();
        Vector start = castLocation.getLocation().toVector();
        Vector end = direction.clone().multiply(maxDist).add(start);
        RayCastHit hit = physics.rayCast(world, start, end);
        if (hit == null)
            return world.getBlockAt(end.getBlockX(), end.getBlockY(), end.getBlockZ());
        return hit.isEntity() ? hit.getEntity().getLocation().getBlock() : hit.getBlock(world);
    }

    private SkillResult invalidTargetWithMessage(Player player) {
        player.sendMessage("Unable to fit an overgrowth at this location.");
        return SkillResult.INVALID_TARGET_NO_MSG;
    }

    private class SkillConstructionData {

        final List<Block> constructionBlocks;
        final Location enderCrystalLoc;

        List<Block> activeBlocks;
        EnderCrystal activeEnderCrystal;

        SkillConstructionData(List<Block> constructionBlocks) {
            Block highestBlock = constructionBlocks.stream().max(Comparator.comparing(Block::getY)).get();
            this.enderCrystalLoc = highestBlock.getLocation().clone().add(0.5, 0.5, 0.5);

            constructionBlocks.remove(highestBlock);
            this.constructionBlocks = constructionBlocks;
            this.activeBlocks = new ArrayList<Block>();
        }

        private void construct(Player player) {
            for (Block block : constructionBlocks) {
                if (!block.isEmpty())
                    continue;

                activeBlocks.add(block);
                block.setType(Material.BEDROCK);
            }

            World world = enderCrystalLoc.getWorld();
            activeEnderCrystal = (EnderCrystal) world.spawnEntity(enderCrystalLoc, EntityType.ENDER_CRYSTAL);
            activeEnderCrystal.setCustomName(player.getCustomName() + "'s Ender Crystal");
        }

        private void deconstruct() {
            for (Block block : activeBlocks) {
                Chunk chunk = block.getChunk();
                if (!chunk.isLoaded())
                    chunk.load();

                block.setType(Material.AIR);
            }
            activeBlocks.clear();

            if (activeEnderCrystal != null) {
                activeEnderCrystal.remove();
                activeEnderCrystal = null;
            }
        }
    }

    public class EnderCrystaledEffect extends PeriodicExpirableEffect {
        private final SkillConstructionData constructionData;
        private final double healAmount;
        private final int maxHealDistance;

        EnderCrystaledEffect(Skill skill, Player applier, long period, long duration,
                             SkillConstructionData constructionData, double healAmount, int naxHealDistance) {
            super(skill, "HasSummonedEnderCrystal", applier, period, duration);
            this.constructionData = constructionData;
            this.healAmount = healAmount;
            this.maxHealDistance = naxHealDistance;

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.DARK);
            types.add(EffectType.MAGIC);
            types.add(EffectType.HEALING);
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            constructionData.construct(hero.getPlayer());
            activeConstructions.add(constructionData);

            healHeroAndUpdateBeamIfTransformed(hero);
        }

        @Override
        public void tickMonster(Monster monster) { }

        @Override
        public void tickHero(Hero hero) {
            healHeroAndUpdateBeamIfTransformed(hero);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            constructionData.deconstruct();
            activeConstructions.add(constructionData);
        }

        private void healHeroAndUpdateBeamIfTransformed(Hero hero) {
            if (constructionData.activeEnderCrystal == null)
                return;

            Player player = hero.getPlayer();
            if (!hero.hasEffect("EnderBeastTransformed") || constructionData.activeEnderCrystal.getLocation().distance(player.getLocation()) > maxHealDistance) {
                constructionData.activeEnderCrystal.setBeamTarget(constructionData.activeEnderCrystal.getLocation());
                return;
            }

            constructionData.activeEnderCrystal.setBeamTarget(player.getLocation().add(new Vector(0, -1, 0)));

            HeroRegainHealthEvent healEvent = new HeroRegainHealthEvent(hero, healAmount, skill);
            Bukkit.getPluginManager().callEvent(healEvent);
            if (!healEvent.isCancelled())
                hero.heal(healEvent.getDelta());
        }
    }

    private List<Block> getAllActiveBlocks() {
        return activeConstructions
                .stream()
                .flatMap(x -> x.activeBlocks.stream())
                .collect(Collectors.toList());
    }

    private List<EnderCrystal> getAllActiveEnderCrystals() {
        return activeConstructions
                .stream()
                .filter(x -> x.activeEnderCrystal != null)
                .map(x -> x.activeEnderCrystal)
                .collect(Collectors.toList());
    }

    private List<Block> getAllActiveEnderCrystalBlocks() {
        return getAllActiveEnderCrystals()
                .stream()
                .map(x -> x.getLocation().getBlock())
                .collect(Collectors.toList());
    }

    private List<Block> getAllActiveBlocksIncludingEnderCrystals() {
        List<Block> allBlocks = new ArrayList<Block>();
        allBlocks.addAll(getAllActiveBlocks());
        allBlocks.addAll(getAllActiveEnderCrystalBlocks());
        return allBlocks;
    }

    public class SkillBlockListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPluginDisable(PluginDisableEvent e) {
            if (e.getPlugin() != plugin)
                return;

            for (SkillConstructionData data : activeConstructions) {
                try {
                    data.deconstruct();
                } catch (Exception ex) {
                    // Ignore and hope for the best. Shouldn't ever happen, but juuuust in case.
                }
            }
            activeConstructions.clear();
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEntityExplode(EntityExplodeEvent e) {
            if (!(e.getEntity() instanceof EnderCrystal))
                return;

            EnderCrystal crystal = (EnderCrystal) e.getEntity();
            if (getAllActiveEnderCrystals().contains(crystal))
                e.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
            if (!(e.getEntity() instanceof EnderCrystal))
                return;

            EnderCrystal crystal = (EnderCrystal) e.getEntity();
            if (getAllActiveEnderCrystals().contains(crystal))
                e.setCancelled(true);
        }

        // For stopping paved grass / crops being destroyed
        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onBlockFade(BlockFadeEvent event) {
            Block block = event.getBlock();
            if (block == null)
                return;

            Block above = block.getRelative(BlockFace.UP);

            List<Block> activeBlocks = getAllActiveBlocksIncludingEnderCrystals();
            if (activeBlocks.contains(block) || activeBlocks.contains(above))
                event.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onBlockSpread(BlockSpreadEvent event) {
            Block block = event.getBlock();
            Block sourceBlock = event.getSource();

            List<Block> activeBlocks = getAllActiveBlocksIncludingEnderCrystals();
            if (activeBlocks.contains(block) || activeBlocks.contains(sourceBlock))
                event.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onBlockBreak(BlockBreakEvent event) {
            Block block = event.getBlock();

            List<Block> activeBlocks = getAllActiveBlocks();
            if (activeBlocks.contains(block))
                event.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onBlockFromTo(BlockFromToEvent event) {
            Block fromBlock = event.getBlock();
            Block toBlock = event.getToBlock();

            List<Block> activeBlocks = getAllActiveBlocksIncludingEnderCrystals();
            if (activeBlocks.contains(toBlock) || activeBlocks.contains(fromBlock))
                event.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onBlockDamage(BlockDamageEvent event) {
            Block block = event.getBlock();

            List<Block> activeBlocks = getAllActiveBlocksIncludingEnderCrystals();
            if (activeBlocks.contains(block))
                event.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEntityChangeBlock(EntityChangeBlockEvent event) {
            Block block = event.getBlock();

            List<Block> activeBlocks = getAllActiveBlocksIncludingEnderCrystals();
            if (activeBlocks.contains(block))
                event.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onHangingBreak(HangingBreakEvent event) {
            Block block = event.getEntity().getLocation().getBlock();
            BoundingBox box = event.getEntity().getBoundingBox();

            List<Block> activeBlocks = getAllActiveBlocksIncludingEnderCrystals();
            if (activeBlocks.contains(block)
                    || activeBlocks.stream().anyMatch((changedBlock) -> changedBlock.getBoundingBox().contains(box))
                    || activeBlocks.stream().anyMatch((changedBlock) -> box.contains(changedBlock.getBoundingBox()))) {
                event.setCancelled(true);
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onBlockPistonExtend(BlockPistonExtendEvent event) {

            List<Block> activeBlocks = getAllActiveBlocksIncludingEnderCrystals();
            if (event.getBlocks().stream().anyMatch(activeBlocks::contains))
                event.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onBlockPistonRetract(BlockPistonRetractEvent event) {

            List<Block> activeBlocks = getAllActiveBlocksIncludingEnderCrystals();
            if (event.getBlocks().stream().anyMatch(activeBlocks::contains))
                event.setCancelled(true);
        }
    }
}
