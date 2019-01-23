package com.herocraftonline.heroes.characters.skill.reborn;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.material.Attachable;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.*;

public class SkillFuckPaintings extends ActiveSkill {
    private final NMSPhysics physics = NMSPhysics.instance();

    public SkillFuckPaintings(Heroes plugin) {
        super(plugin, "FuckPaintings");
        setDescription("Attempt to Entomb a player for $1 seconds.");
        setUsage("/skill fuckpaintings");
        setArgumentRange(0, 0);
        setIdentifiers("skill fuckpaintings");
        setTypes(SkillType.ABILITY_PROPERTY_EARTH, SkillType.SILENCEABLE, SkillType.BLOCK_CREATING, SkillType.MULTI_GRESSIVE);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        return node;
    }

    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public SkillResult use(Hero hero, String[] strings) {
        Player player = hero.getPlayer();
        World world = player.getWorld();

        int maxDist = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 12, false);

        Block targetBlock = getBlockViaRaycast(player, maxDist);
        if (targetBlock == null)
            return SkillResult.INVALID_TARGET_NO_MSG;

        for (Entity entity : getEntitiesWithinSphere(targetBlock.getLocation(), 3)) {
            BoundingBox box = entity.getBoundingBox();
            Block minBlock = world.getBlockAt(new Location(world, box.getMinX(), box.getMinY(), box.getMinZ()));
            Block maxBlock = world.getBlockAt(new Location(world, box.getMaxX(), box.getMaxY(), box.getMaxZ()));
            minBlock.setType(Material.BLUE_STAINED_GLASS);
            maxBlock.setType(Material.RED_STAINED_GLASS);
        }

        return SkillResult.NORMAL;
    }

    private Block getBlockViaRaycast(Player player, int maxDist) {
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
}
