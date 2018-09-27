package com.herocraftonline.heroes.characters.skill.pack5;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.CompatSound;
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

import java.util.ArrayList;
import java.util.List;

public class SkillBoneSpear extends ActiveSkill {

    public SkillBoneSpear(Heroes plugin) {
        super(plugin, "BoneSpear");
        setDescription("Launch a spear of bone in front of you. The spear will travel up to $1 blocks, pass through enemies, and damage all targets hit for $2 damage.");
        setUsage("/skill bonespear");
        setArgumentRange(0, 0);
        setIdentifiers("skill bonespear");
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT);
    }

    public String getDescription(Hero hero) {

        int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 6, false);

        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 90, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.2, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        return getDescription().replace("$1", distance + "").replace("$2", damage + "");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 20);
        node.set(SkillSetting.DAMAGE.node(), 80);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.125);
        node.set(SkillSetting.RADIUS.node(), 2);
        node.set("spear-move-delay", 2);

        return node;
    }

    public SkillResult use(final Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 10, false);

        Block tempBlock;
        BlockIterator iter = null;
        try {
            iter = new BlockIterator(player, distance);
        }
        catch (IllegalStateException e) {
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        broadcastExecuteText(hero);

        double tempDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 90, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.2, false);
        tempDamage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));
        final double damage = tempDamage;

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 2, false);
        final int radiusSquared = radius * radius;

        int delay = SkillConfigManager.getUseSetting(hero, this, "spear-move-delay", 1, false);

        final List<Entity> nearbyEntities = player.getNearbyEntities(distance * 2, distance, distance * 2);
        final List<Entity> hitEnemies = new ArrayList<>();
        
        // This looks out of place, but it's up here because it turns into ear-explosion-death-sound if it's in the loop.
        // Also I just wanted to use the phrase "ear-explosion-death-sound."
        player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_SKELETON_HURT.value(), 6.0F, 1);

        int numBlocks = 0;
        while (iter.hasNext()) {
            tempBlock = iter.next();
            Material tempBlockType = tempBlock.getType();
            if (Util.transparentBlocks.contains(tempBlockType)) {
                final Location targetLocation = tempBlock.getLocation().clone().add(new Vector(.5, 0, .5));

                // Schedule the action in advance
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        //Play effect
                    	
                        //attempting spigot particles
                    	// Why does it play a bunch of crit particles every block the spear travels?
                        //player.getWorld().spigot().playEffect(player.getLocation().add(0, 0.5, 0), org.bukkit.Effect.CRIT, 0, 0, 0, 0, 0, 1, 25, 16);
                        player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 0.5, 0), 25, 0, 0, 0, 1);
                        //player.getWorld().spigot().playEffect(targetLocation, org.bukkit.Effect.TILE_BREAK, Material.QUARTZ_BLOCK.getId(), 0, 0.3F, 0.3F, 0.3F, 0.1F, 4, 16);
                        player.getWorld().spawnParticle(Particle.BLOCK_CRACK, targetLocation, 4, 0.3, 0.3, 0.3, 0.1, Bukkit.createBlockData(Material.QUARTZ_BLOCK));
                        
                        // Check our entity list to see if they are on this specific block at the moment the firework plays
                        for (Entity entity : nearbyEntities) {
                            // Ensure that we have a valid entity
                            if (!(entity instanceof LivingEntity) || hitEnemies.contains(entity) || entity.getLocation().distanceSquared(targetLocation) > radiusSquared)
                                continue;

                            // Check to see if the entity can be damaged
                            if (!damageCheck(player, (LivingEntity) entity))
                                continue;

                            // Damage target
                            LivingEntity target = (LivingEntity) entity;
                            addSpellTarget(target, hero);
                            damageEntity(target, player, damage, DamageCause.MAGIC);

                            // Add the target to the hitEntity map to ensure we don't ever hit them again with this specific BoneSpear
                            hitEnemies.add(entity);
                        }
                    }
                }, numBlocks * delay);

                numBlocks++;
            }
            else
                break;
        }

        return SkillResult.NORMAL;
    }
}
