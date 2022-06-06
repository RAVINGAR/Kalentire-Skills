package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SkillTerror extends ActiveSkill {

    private static final Particle.DustOptions skillEffectDustOptions = new Particle.DustOptions(Color.GRAY, 1);

    private String applyText;
    private String expireText;

    public SkillTerror(Heroes plugin) {
        super(plugin, "Terror");
        setDescription("You terrify your target, impairing their movement and disabling them for $1 second(s).");
        setUsage("/skill terror");
        setArgumentRange(0, 0);
        setIdentifiers("skill terror");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.BLINDING, SkillType.MOVEMENT_SLOWING, SkillType.DISABLING,
                SkillType.AREA_OF_EFFECT, SkillType.SILENCEABLE, SkillType.MULTI_GRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        return getDescription().replace("$1", Util.decFormat.format(duration / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 7);
        config.set("slow-amplifier", 2);
        config.set(SkillSetting.DURATION.node(), 4000);
        config.set(SkillSetting.DURATION_INCREASE_PER_CHARISMA.node(), 75);
        config.set(SkillSetting.RADIUS.node(), 3D);
        config.set("cone-travel-delay", 1);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has been overcome with fear!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has overcome his fear!");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(),
                ChatComponents.GENERIC_SKILL + "%target% is terrified!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(),
                ChatComponents.GENERIC_SKILL + "%target% has overcome his fear!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        int distance = SkillConfigManager.getUseSettingInt(hero, this, SkillSetting.MAX_DISTANCE,  false);
        BlockIterator blockIterator = getLineBlockIterator(player, distance);
        if (blockIterator == null)
            return SkillResult.INVALID_TARGET_NO_MSG;

        // Terror Effect properties
        int slowAmplifier = SkillConfigManager.getUseSettingInt(hero, this, "slow-amplifier", false);
        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);

        // Properties for Cone effect
        boolean isXDirection = is_X_Direction(player);
        int delay = SkillConfigManager.getUseSettingInt(hero, this, "cone-travel-delay", false);

        double radius = SkillConfigManager.getUseSettingDouble(hero, this, SkillSetting.RADIUS,  false);
        final double radiusSquared = radius * radius;

        final List<Entity> nearbyEntities = player.getNearbyEntities(distance * 2, distance, distance * 2);
        final List<Entity> hitEnemies = new ArrayList<>();

        // Alright we're ready to start now, lets go
        broadcastExecuteText(hero);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1.2F, 2.0F);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_HOWL, 0.4F, 1.8F);

        // Iterate through each block in direction (line)
        Skill skill = this;
        int numBlocks = 0;
        Block tempMiddleRowBlock;
        while (blockIterator.hasNext()) {
            tempMiddleRowBlock = blockIterator.next();

            // Stop on 'solid' block (we've hit a wall?)
            if (!Util.transparentBlocks.contains(tempMiddleRowBlock.getType()))
                break;

            final List<Location> locations = getNewRowBlockLocations(isXDirection, tempMiddleRowBlock);

            // Delay particles and effects for each new block "row"
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

                        // Effect target
                        //addSpellTarget(target, hero); // no damage, so no need to add target?
                        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter((LivingEntity) entity);
                        targetCT.addEffect(new TerrorEffect(skill, player, duration, slowAmplifier));

                        hitEnemies.add(entity);
                    }
                }
            }, numBlocks * delay);

            numBlocks++;
        }

        return SkillResult.NORMAL;
    }

    // Original Targetted Skill use code
    /*
    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 10, false);
        BlockIterator blockIterator = getLineBlockIterator(player, distance);
        if (blockIterator == null)
            return SkillResult.INVALID_TARGET_NO_MSG;

        int slowAmplifier = SkillConfigManager.getUseSettingInt(hero, this, "slow-amplifier", false);
        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);

        broadcastExecuteText(hero, target);

        TerrorEffect dEffect = new TerrorEffect(this, player, duration, slowAmplifier);

        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
        targetCT.addEffect(dEffect);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1.2F, 2.0F);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_HOWL, 0.4F, 1.8F);
        //target.getWorld().spigot().playEffect(target.getEyeLocation(), org.bukkit.Effect.LARGE_SMOKE, 0, 0, 0.2F, 0.0F, 0.2F, 0.1F, 25, 16);
        target.getWorld().spawnParticle(Particle.SMOKE_LARGE, target.getEyeLocation(), 25, 0.2, 0, 0.2, 0.1);
        //target.getWorld().spigot().playEffect(target.getEyeLocation(), org.bukkit.Effect.EXPLOSION, 0, 0, 0.2F, 0.0F, 0.2F, 0.5F, 25, 16);
        target.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, target.getEyeLocation(), 25, 0.2, 0, 0.2, 0.5);

        return SkillResult.NORMAL;
    }
     */

    public class TerrorEffect extends SlowEffect {

        public TerrorEffect(Skill skill, Player applier, long duration, int slowAmplifier) {
            super(skill, "Terror", applier, duration, slowAmplifier, applyText, expireText);

            Collections.addAll(types, EffectType.DARK, EffectType.SLOW, EffectType.BLIND, EffectType.DISABLE,
                    EffectType.DISPELLABLE, EffectType.HARMFUL);

            addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int)(20L * duration / 1000L), 0));
        }
    }


    public boolean isEntityInRangeOfAnyLocation(Entity entity, double radiusSquared, List<Location> locations) {
        for (Location location : locations) {
            if (entity.getLocation().distanceSquared(location) <= radiusSquared) {
                return true;
            }
        }
        return false;
    }

    public void doParticlesAtLocations(World world, List<Location> locations) {
        try {
            for (Location location : locations) {
                // FIXME do we want to use smoke?
                //world.spawnParticle(Particle.SMOKE_NORMAL, location, 3, 0, 0.3, 0, 0.2);
                world.spawnParticle(Particle.REDSTONE, location, 3, 0, 0, 0, 0, skillEffectDustOptions);
                // just for ref: world.spawnParticle(Particle.REDSTONE, location, 4, 0.2F, 1.5F, 0.2F, 0, skillEffectDustOptions);
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
