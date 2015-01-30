package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
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

public class SkillDragonsBreath extends ActiveSkill {

    public VisualEffect fplayer = new VisualEffect();

    public SkillDragonsBreath(Heroes plugin) {
        super(plugin, "DragonsBreath");
        setDescription("You unleash the furious breath of a dragon in front of you, up to $1 blocks. Targets hit will will be dealt $2 fire damage.");
        setUsage("/skill dragonsbreath");
        setArgumentRange(0, 0);
        setIdentifiers("skill dragonsbreath");
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_FIRE, SkillType.AREA_OF_EFFECT, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
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

        node.set(SkillSetting.MAX_DISTANCE.node(), 6);
        node.set(SkillSetting.DAMAGE.node(), 80);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.125);
        node.set(SkillSetting.RADIUS.node(), 3);
        node.set("breath-travel-delay", 1);

        return node;
    }

    public SkillResult use(final Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 10, false);

        Block tempBlock;
        BlockIterator iter;
        try {
            iter = new BlockIterator(player, distance);
        }
        catch (IllegalStateException e) {
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        broadcastExecuteText(hero);

        boolean isXDirection = is_X_Direction(player);

        double tempDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 90, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.2, false);
        tempDamage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);
        final double damage = tempDamage;

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 2, false);
        final int radiusSquared = radius * radius;

        int delay = SkillConfigManager.getUseSetting(hero, this, "breath-travel-delay", 1, false);

        final List<Entity> nearbyEntities = player.getNearbyEntities(distance * 2, distance, distance * 2);
        final List<Entity> hitEnemies = new ArrayList<>();

        int numBlocks = 0;
        
        player.getWorld().spigot().playEffect(player.getLocation(), Effect.BLAZE_SHOOT);
        
        while (iter.hasNext()) {
            tempBlock = iter.next();

            if (Util.transparentBlocks.contains(tempBlock.getType())) {
                final List<Location> locations = new ArrayList<>();
                if (isXDirection) {
                    for (int xDir = -1; xDir < 1 + 1; xDir++) {
                        Block radiusBlocks = tempBlock.getRelative(xDir, 0, 0);

                        if (Util.transparentBlocks.contains(radiusBlocks.getType())) {
                            locations.add(radiusBlocks.getLocation().clone().add(new Vector(.5, 0, .5)));
                        }
                    }
                }
                else {
                    for (int zDir = -1; zDir < 1 + 1; zDir++) {
                        Block radiusBlocks = tempBlock.getRelative(0, 0, zDir);

                        if (Util.transparentBlocks.contains(radiusBlocks.getType())) {
                            locations.add(radiusBlocks.getLocation().clone().add(new Vector(.5, 0, .5)));
                        }
                    }
                }

                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        try {
                            for (Location location : locations) {
                                //fplayer.playFirework(location.getWorld(), location, FireworkEffect.builder().flicker(false).trail(true).with(FireworkEffect.Type.BURST).withColor(Color.MAROON).withFade(Color.ORANGE).build());
                            	player.getWorld().spigot().playEffect(location, Effect.MOBSPAWNER_FLAMES, 1, 1, 0F, 0.3F, 0F, 0.2F, 3, 10);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        for (Entity entity : nearbyEntities) {
                            if (!(entity instanceof LivingEntity) || hitEnemies.contains(entity))
                                continue;

                            boolean exitLoop = true;
                            for (Location location : locations) {
                                if (entity.getLocation().distanceSquared(location) <= radiusSquared) {
                                    exitLoop = false;
                                    break;
                                }
                            }

                            if (exitLoop)
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
                break;
        }

        return SkillResult.NORMAL;
    }

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
