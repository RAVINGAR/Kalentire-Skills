package com.herocraftonline.heroes.characters.skill.reborn;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class SkillRapidFire extends ActiveSkill {

    public SkillRapidFire(Heroes plugin) {
        super(plugin, "RapidFire");
        setDescription("Launch a rapid fire of arrows in front of you. The spear will travel up to $1 blocks, pass through enemies, and damage all targets hit for $2 damage.");
        setUsage("/skill rapidfire");
        setArgumentRange(0, 0);
        setIdentifiers("skill rapidfire");
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_PROJECTILE, SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT);

    }

    public String getDescription(Hero hero) {
        int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 6, false);
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 90, false);
        return getDescription().replace("$1", distance + "").replace("$2", damage + "");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.MAX_DISTANCE.node(), 20);
        node.set(SkillSetting.DAMAGE.node(), 80);
        node.set(SkillSetting.PERIOD.node(), 1000);
        node.set(SkillSetting.RADIUS.node(), 2);
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set("rapidfire-move-delay", 2);
        return node;
    }

    public SkillResult use(final Hero hero, String[] args) {

        final Player player = hero.getPlayer();


        int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 10, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 500, false);

        broadcastExecuteText(hero);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 90, false);


        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 2, false);
        final int radiusSquared = radius * radius;

        int delay = SkillConfigManager.getUseSetting(hero, this, "rapidfire-move-delay", 1, false);




        iterate effect = new iterate(this, player, period, duration, distance, radiusSquared, delay, damage);
        hero.addEffect(effect);

        return SkillResult.NORMAL;
    }


    @EventHandler
    public void onProj(ProjectileLaunchEvent event) {
        if (event.getEntityType() != EntityType.ARROW)
            return;
        if(!(event.getEntity() instanceof Arrow)) return;
        Arrow a = (Arrow) event.getEntity();
        Entity shooter = (Entity) event.getEntity().getShooter();
        a.setVelocity(shooter.getLocation().getDirection().multiply(2.0D));
    }



    private class iterate extends PeriodicExpirableEffect {

        private final long _duration;
        private final int _distance;
        private final double _radiusSquared;
        private final int _delay;
        private final double _damage;
        private final double _radius;


        public iterate(Skill skill, Player applier, long period, long duration, int distance, double radius, int delay, double damage) {
            super(skill, "iterate", applier, period, duration);
            _duration = duration;
            _distance = distance;
            _radiusSquared = radius * radius;
            _radius = radius;
            _delay = delay;
            _damage = damage;
            types.add(EffectType.PHYSICAL);

        }



        //Method to check if player is facing X to help radius accuracy.
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


        @Override
        public void tickMonster(Monster monster) {

        }

        @Override
        public void tickHero(Hero hero) {

            for (Effect effect : hero.getEffects()) {
                if (effect.isType(EffectType.STUN) || effect.isType(EffectType.DISABLE) || effect.isType(EffectType.SILENCE)) {
                    hero.removeEffect(this);
                    return;
                }
            }

            final Player player = hero.getPlayer();

            Block tempBlock;
            BlockIterator iter = null;
            try {
                iter = new BlockIterator(player, _distance);
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }

            final List<Entity> nearbyEntities = player.getNearbyEntities(_radius, _radiusSquared, _radius);
            final List<Entity> hitEnemies = new ArrayList<>();

            int numBlocks = 0;
            while (iter.hasNext()) {
                tempBlock = iter.next();
                Material tempBlockType = tempBlock.getType();

                if (Util.transparentBlocks.contains(tempBlockType)) {
                    final Location targetLocation = tempBlock.getLocation().clone().add(new Vector(.5, 0, .5));

                    Location loc = player.getLocation();
                    Vector x = loc.getDirection().normalize();

                    //Arrow Visual
                    Arrow shoot = player.getWorld().spawnArrow(targetLocation, x, (float) 0.5, (float) 1.0);
                    shoot.setKnockbackStrength(0);
                    shoot.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
                    shoot.setDamage(0);
                    shoot.setGravity(false);

                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                        public void run() {
                            shoot.remove();
                        }
                    }, 3L); //Edit ticks for arrows based on skill range, otherwise arrow visual will be thrown off****************


                    // Schedule the action in advance
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                        public void run() {
                            player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, targetLocation, 4, 0.3, 0.3, 0.3, 0.02);
                            player.getWorld().spawnParticle(Particle.FLAME, targetLocation, 2, 0.3, 0.3, 0.3, 0.05);

                            

                            // Check our entity list to see if they are on this specific block at the moment the firework plays
                            for (Entity entity : nearbyEntities) {
                                // Ensure that we have a valid entity
                                if (!(entity instanceof LivingEntity) || hitEnemies.contains(entity) || entity.getLocation().distanceSquared(targetLocation) > _radiusSquared)
                                    continue;

                                // Check to see if the entity can be damaged
                                if (!damageCheck(player, (LivingEntity) entity))
                                    continue;

                                // Damage target
                                LivingEntity target = (LivingEntity) entity;
                                addSpellTarget(target, hero);
                                damageEntity(target, player, _damage, EntityDamageEvent.DamageCause.PROJECTILE);


                                // Add the target to the hitEntity map to ensure we don't ever hit them again with this specific BoneSpear
                                hitEnemies.add(entity);
                            }
                        }
                    }, numBlocks * _delay);

                    numBlocks++;

                } else
                    break;
            }


        }
    }
}

