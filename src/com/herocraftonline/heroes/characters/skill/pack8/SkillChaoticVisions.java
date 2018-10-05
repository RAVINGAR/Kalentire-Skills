package com.herocraftonline.heroes.characters.skill.pack8;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
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

public class SkillChaoticVisions extends ActiveSkill {

    public SkillChaoticVisions(Heroes plugin) {
        super(plugin, "ChaoticVisions");
        setDescription("Launch an illusion of chaos in front of you. The spear will travel up to $1 blocks, pass through enemies, and damage all targets hit for $2 damage.");
        setUsage("/skill chaoticvisions");
        setArgumentRange(0, 0);
        setIdentifiers("skill chaoticvisions");
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

        double tempDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 140, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.2, false);
        tempDamage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));
        final double damage = tempDamage;

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);
        final int radiusSquared = radius * radius;

        int delay = SkillConfigManager.getUseSetting(hero, this, "spear-move-delay", 1, false);

        final List<Entity> nearbyEntities = player.getNearbyEntities(distance * 2, distance, distance * 2);
        final List<Entity> hitEnemies = new ArrayList<>();

        int numBlocks = 0;
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.7F, 1);
        while (iter.hasNext()) {
            tempBlock = iter.next();
            Material tempBlockType = tempBlock.getType();
            if (Util.transparentBlocks.contains(tempBlockType)) {
                final Location targetLocation = tempBlock.getLocation().clone().add(new Vector(.5, 0, .5));

                // Schedule the action in advance
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        // Play effect
                    	
                    	//ParticleEffect.SPELL_WITCH.display(1, 1, 1, 0.1, 5, targetLocation, 20);
                    	//public void playEffect(Location location, Effect effect,  id,  data,  offsetX,  offsetY,  offsetZ,  speed,  particleCount,  radius)
                    	//targetLocation.getWorld().spigot().playEffect(targetLocation, Effect.WITCH_MAGIC, 1, 1, 0F, 0F, 0F, 0.2F, 10, 20);
                        targetLocation.getWorld().spawnParticle(Particle.SPELL_WITCH, targetLocation, 10, 0, 0, 0, 0.2, true);
                    	
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

                            // Add the target to the hitEntity map to ensure we don't ever hit them again with this specific ChaoticVisions
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
