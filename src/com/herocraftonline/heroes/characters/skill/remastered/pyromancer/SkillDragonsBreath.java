package com.herocraftonline.heroes.characters.skill.remastered.pyromancer;

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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class SkillDragonsBreath extends ActiveSkill {

    public SkillDragonsBreath(Heroes plugin) {
        super(plugin, "DragonsBreath");
        setDescription("You unleash the furious breath of a dragon in front of you, up to $1 blocks. Targets hit will will be dealt $2 fire damage.");
        setUsage("/skill dragonsbreath");
        setArgumentRange(0, 0);
        setIdentifiers("skill dragonsbreath");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_FIRE, SkillType.AREA_OF_EFFECT, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
    }

    public String getDescription(Hero hero) {
        int distance = SkillConfigManager.getUseSettingInt(hero, this, SkillSetting.MAX_DISTANCE, false);
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);

        return getDescription()
                .replace("$1", distance + "")
                .replace("$2", Util.decFormat.format(damage));
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 6);
        config.set(SkillSetting.DAMAGE.node(), 80);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.125);
        config.set(SkillSetting.RADIUS.node(), 3D);
        config.set("breath-travel-delay", 1);
        return config;
    }

    public SkillResult use(final Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 10, false);
        BlockIterator blockIterator = getLineBlockIterator(player, distance);
        if (blockIterator == null)
            return SkillResult.INVALID_TARGET_NO_MSG;

        broadcastExecuteText(hero);

        boolean isXDirection = is_X_Direction(player);

        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE,  false);

        double radius = SkillConfigManager.getUseSettingDouble(hero, this, SkillSetting.RADIUS,  false);
        final double radiusSquared = radius * radius;

        int delay = SkillConfigManager.getUseSettingInt(hero, this, "breath-travel-delay", false);

        final List<Entity> nearbyEntities = player.getNearbyEntities(distance * 2, distance, distance * 2);
        final List<Entity> hitEnemies = new ArrayList<>();

        //player.getWorld().spigot().playEffect(player.getLocation(), Effect.BLAZE_SHOOT);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 1);

        // Iterate through each block in direction (line)
        int numBlocks = 0;
        Block tempMiddleRowBlock;
        while (blockIterator.hasNext()) {
            tempMiddleRowBlock = blockIterator.next();

            if (Util.transparentBlocks.contains(tempMiddleRowBlock.getType())) {
                final List<Location> locations = getNewRowBlockLocations(isXDirection, tempMiddleRowBlock);

                // Delay particles and damage for each new block "row"
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        doParticlesAtLocations(player.getWorld(), locations);

                        for (Entity entity : nearbyEntities) {
                            if (!(entity instanceof LivingEntity) || hitEnemies.contains(entity))
                                continue; // Skip invalid entities, and entities already hit

                            // Check if entity is in range and should be effected
                            if (!isEntityInRangeOfAnyLocation(entity, radiusSquared, locations))
                                continue;
                            
                            // Check to see if the entity can be damaged
                            if (!damageCheck(player, (LivingEntity) entity))
                                continue;

                            // Damage target
                            LivingEntity target = (LivingEntity) entity;

                            addSpellTarget(target, hero);
                            damageEntity(target, player, damage, DamageCause.MAGIC);

                            hitEnemies.add(entity);
                        }
                    }
                }, numBlocks * delay);

                numBlocks++;
            }
            else
                break; // Stop on 'solid' block (we've hit a wall?)
        }

        return SkillResult.NORMAL;
    }

    public boolean isEntityInRangeOfAnyLocation(Entity entity, double radiusSquared, List<Location> locations) {
        boolean inRange = false;
        for (Location location : locations) {
            if (entity.getLocation().distanceSquared(location) <= radiusSquared) {
                inRange = true;
                break;
            }
        }
        return inRange;
    }

    public void doParticlesAtLocations(World world, List<Location> locations) {
        try {
            for (Location location : locations) {
                //player.getWorld().spigot().playEffect(location, Effect.MOBSPAWNER_FLAMES, 1, 1, 0F, 0.3F, 0F, 0.2F, 3, 10);
                //FIXME See if this is correct
                world.spawnParticle(Particle.FLAME, location, 3, 0, 0.3, 0, 0.2);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Location> getNewRowBlockLocations(boolean isXDirection, Block middleLocationBlock) {
        // Get left, right and middle block, based on whether its x or z direction
        final List<Location> locations = new ArrayList<>();
        if (isXDirection) {
            for (int xDir = -1; xDir < 1 + 1; xDir++) {
                Block radiusBlocks = middleLocationBlock.getRelative(xDir, 0, 0);

                if (Util.transparentBlocks.contains(radiusBlocks.getType())) {
                    locations.add(radiusBlocks.getLocation().clone().add(new Vector(.5, 0, .5)));
                }
            }
        }
        else { // Z Direction
            for (int zDir = -1; zDir < 1 + 1; zDir++) {
                Block radiusBlocks = middleLocationBlock.getRelative(0, 0, zDir);

                if (Util.transparentBlocks.contains(radiusBlocks.getType())) {
                    locations.add(radiusBlocks.getLocation().clone().add(new Vector(.5, 0, .5)));
                }
            }
        }
        return locations;
    }

    @Nullable
    public BlockIterator getLineBlockIterator(Player player, int distance) {
        BlockIterator blockIterator;
        try {
            blockIterator = new BlockIterator(player, distance);
        } catch (IllegalStateException e) {
            return null;
        }
        return blockIterator;
    }

    /**
     * @return true if X direction, otherwise false for Z direction
     */
    private boolean is_X_Direction(Player player) {
        Vector u = player.getLocation().getDirection();
        u = new Vector(u.getX(), 0.0D, u.getZ()).normalize();
        Vector v = new Vector(0, 0, -1);
        double magU = Math.sqrt(Math.pow(u.getX(), 2.0D) + Math.pow(u.getZ(), 2.0D));
        double magV = Math.sqrt(Math.pow(v.getX(), 2.0D) + Math.pow(v.getZ(), 2.0D));
        double angle = Math.acos(u.dot(v) / (magU * magV));
        angle = angle * 180.0D / Math.PI;
        angle = Math.abs(angle - 180.0D);

        return (angle <= 45.0D) || (angle > 135.0D);
    }
}
