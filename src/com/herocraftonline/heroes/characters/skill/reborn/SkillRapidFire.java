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
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

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

        //Grab our hero casting our skill
        final Player player = hero.getPlayer();

        //Broadcast the hero is executing RapidFire
        broadcastExecuteText(hero);

        //Distance of our Rapid Fire
        int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 10, false);

        //Duration of our RapidFire
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);

        //Period inbetween RapidFire shots
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 500, false);

        //Damage of each RapidFire blast
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 90, false);

        //Travel time? Might be useless we'll see.
        int delay = SkillConfigManager.getUseSetting(hero, this, "rapidfire-move-delay", 1, false);

        //Our method call.
        RapidFireEffect effect = new RapidFireEffect(this, player, period, duration, distance, delay, damage);
        hero.addEffect(effect);

        return SkillResult.NORMAL;
    }

    //Method to keep the arrows firing straight for our visual.
    @EventHandler
    public void onProj(ProjectileLaunchEvent event) {
        if (event.getEntityType() != EntityType.ARROW)
            return;
        if (!(event.getEntity() instanceof Arrow)) return;
        Arrow a = (Arrow) event.getEntity();
        Entity shooter = (Entity) event.getEntity().getShooter();
        a.setVelocity(shooter.getLocation().getDirection().multiply(2.0D));
    }


    private class RapidFireEffect extends PeriodicExpirableEffect {

        private final long _duration;
        private final int _distance;
        private final int _delay;
        private final double _damage;

        RapidFireEffect(Skill skill, Player applier, long period, long duration, int distance, int delay, double damage) {
            super(skill, "iterate", applier, period, duration);
            _duration = duration;
            _distance = distance;
            _delay = delay;
            _damage = damage;

            types.add(EffectType.PHYSICAL);
        }

        @Override
        public void tickMonster(Monster monster) {

        }

        @Override
        public void tickHero(Hero hero) {

            //Cancel the ability if player gets stunned, disabled or silenced (should cover all the CC methods, if not add more to the loop).
            for (Effect effect : hero.getEffects()) {
                if (effect.isType(EffectType.STUN) || effect.isType(EffectType.DISABLE) || effect.isType(EffectType.SILENCE)) {
                    hero.removeEffect(this);
                    return;
                }
            }

            performRapidFire(hero);
        }

        private void performRapidFire(Hero hero) {
            final Player player = hero.getPlayer();

            Block currentBlock;
            BlockIterator iter;
            try {
                iter = new BlockIterator(player, _distance);
            } catch (IllegalStateException e) {
                return;
            }

            Vector playerFacingDirection = player.getLocation().getDirection();
            boolean isXDirection = is_X_Direction(playerFacingDirection);

            int iteratedBlockCount = 0;
            while (iter.hasNext()) {
                currentBlock = iter.next();

                if (!Util.transparentBlocks.contains(currentBlock.getType()))
                    break;

                final List<Location> rowLocations = getLocationsForRow(currentBlock, isXDirection);

                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {

                        displayArrowVisualOnRow(player, playerFacingDirection, rowLocations);
                        damageEntitiesOnRow(hero, player, rowLocations);
                    }
                }, iteratedBlockCount * _delay);

                iteratedBlockCount++;
            }
        }

        private void displayArrowVisualOnRow(Player player, Vector facingDirection, List<Location> rowLocations) {
            for (Location rowLocation : rowLocations) {
                Arrow proj = player.getWorld().spawnArrow(rowLocation, facingDirection.normalize(), (float) 0.5, (float) 1.0);
                proj.setKnockbackStrength(0);
                proj.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
                proj.setDamage(0);
                proj.setBounce(false);
                proj.setGravity(false);

                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        proj.remove();
                    }
                }, 3L); //Edit ticks for arrows based on skill range, otherwise arrow visual will be thrown off****************
            }
        }

        @NotNull
        private List<Location> getLocationsForRow(Block sourceBlock, boolean isXDirection) {
            final List<Location> locations = new ArrayList<>();
            if (isXDirection) {
                for (int xDir = -1; xDir < 1 + 1; xDir++) {
                    Block rowBlock = sourceBlock.getRelative(xDir, 0, 0);
                    if (Util.transparentBlocks.contains(rowBlock.getType())) {
                        locations.add(rowBlock.getLocation());
                    }
                }
            } else {
                for (int zDir = -1; zDir < 1 + 1; zDir++) {
                    Block rowBlock = sourceBlock.getRelative(0, 0, zDir);
                    if (Util.transparentBlocks.contains(rowBlock.getType())) {
                        locations.add(rowBlock.getLocation());
                    }
                }
            }
            return locations;
        }

        private void damageEntitiesOnRow(Hero hero, Player player, List<Location> locations) {
            final List<Entity> allPossibleTargets = player.getNearbyEntities(_distance, _distance, _distance);
            final List<Entity> hitEnemies = new ArrayList<Entity>();
            for (Entity entity : allPossibleTargets) {
                if (!(entity instanceof LivingEntity) || hitEnemies.contains(entity))
                    continue;

                boolean targetIsWithinRange = false;
                for (Location location : locations) {
                    if (entity.getLocation().distance(location) <= 2) { // 2 blocks is kind of arbitrary... Kind of shitty logic here.
                        targetIsWithinRange = true;
                        break;
                    }
                }
                if (!targetIsWithinRange)
                    continue;
                if (!damageCheck(player, (LivingEntity) entity))
                    continue;

                LivingEntity target = (LivingEntity) entity;
                addSpellTarget(target, hero);
                damageEntity(target, player, _damage, EntityDamageEvent.DamageCause.PROJECTILE);

                hitEnemies.add(entity);
            }
        }

        private boolean is_X_Direction(Vector facingDirection) {
            facingDirection = new Vector(facingDirection.getX(), 0.0D, facingDirection.getZ()).normalize();
            Vector v = new Vector(0, 0, -1);
            double magU = Math.sqrt(Math.pow(facingDirection.getX(), 2.0D) + Math.pow(facingDirection.getZ(), 2.0D));
            double magV = Math.sqrt(Math.pow(v.getX(), 2.0D) + Math.pow(v.getZ(), 2.0D));
            double angle = Math.acos(facingDirection.dot(v) / (magU * magV));
            angle = angle * 180.0D / Math.PI;
            angle = Math.abs(angle - 180.0D);

            return (angle <= 45.0D) || (angle > 135.0D);
        }
    }
}




