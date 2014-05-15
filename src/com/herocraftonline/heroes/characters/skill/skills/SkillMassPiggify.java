package com.herocraftonline.heroes.characters.skill.skills;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;

public class SkillMassPiggify extends ActiveSkill {

    private Map<Entity, CharacterTemplate> creatures = new HashMap<Entity, CharacterTemplate>();

    public SkillMassPiggify(Heroes plugin) {
        super(plugin, "MassPiggify");
        setDescription("You force targets within $1 blocks to ride a pig for $2 seconds.");
        setUsage("/skill masspiggify");
        setArgumentRange(0, 0);
        setIdentifiers("skill masspiggify");
        setTypes(SkillType.DISABLING, SkillType.ABILITY_PROPERTY_ILLUSION, SkillType.AREA_OF_EFFECT, SkillType.SILENCEABLE, SkillType.INTERRUPTING, SkillType.AGGRESSIVE);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS.node(), 5, false);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 2500, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 38, false);
        duration += hero.getAttributeValue(AttributeType.CHARISMA) * durationIncrease;

        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        return getDescription().replace("$1", radius + "").replace("$2", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.RADIUS.node(), Integer.valueOf(5));
        node.set(SkillSetting.DURATION.node(), 2500);
        node.set(SkillSetting.DURATION_INCREASE_PER_CHARISMA.node(), 38);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 2500, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 38, false);
        duration += hero.getAttributeValue(AttributeType.CHARISMA) * durationIncrease;

        long currentTime = System.currentTimeMillis();
        List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }

            LivingEntity target = (LivingEntity) entity;
            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
            if (!damageCheck(player, target)) {
                continue;
            }

            if (targetCT instanceof Hero) {
                Hero enemy = (Hero) targetCT;
                if (enemy.getDelayedSkill() != null) {
                    if (enemy.cancelDelayedSkill())
                        enemy.setCooldown("global", Heroes.properties.globalCooldown + currentTime);
                }
            }

            Material material = target.getLocation().getBlock().getType();
            EntityType type = (material.equals(Material.WATER) ||
                    material.equals(Material.STATIONARY_WATER) ?
                    EntityType.SQUID : EntityType.PIG);

            Entity creature = target.getWorld().spawnEntity(target.getLocation(), type);

            plugin.getCharacterManager().getCharacter(target).addEffect(new PigEffect(this, player, duration, (Creature) creature));
        }

        player.getWorld().playEffect(player.getLocation(), Effect.ENDER_SIGNAL, 3);
        player.getWorld().playSound(player.getLocation(), Sound.PIG_DEATH, 0.8F, 1.0F);
        player.getWorld().playSound(player.getLocation(), Sound.PIG_IDLE, 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {

        @EventHandler(priority = EventPriority.LOWEST)
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled() || event.getDamage() == 0) {
                return;
            }
            if (creatures.containsKey(event.getEntity())) {
                if (event instanceof EntityDamageByEntityEvent) {
                    EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
                    if (subEvent.getDamager().equals(event.getEntity().getPassenger())) {
                        event.setCancelled(true);
                        return;
                    }
                    else if (subEvent.getDamager() instanceof Projectile && ((Projectile) subEvent.getDamager()).getShooter().equals(event.getEntity().getPassenger())) {
                        event.setCancelled(true);
                        return;
                    }
                }
                CharacterTemplate character = creatures.remove(event.getEntity());
                character.removeEffect(character.getEffect("MassPiggify"));
            }
            else if (event.getEntity() instanceof LivingEntity) {
                final CharacterTemplate character = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
                if (character.hasEffect("MassPiggify")) {
                    Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                        @Override
                        public void run() {
                            character.removeEffect(character.getEffect("MassPiggify"));
                        }
                    }, (long) (0.1 * 20));
                }
            }
        }
    }

    //    public class PigEffect extends ExpirableEffect {
    //
    //        private final Creature creature;
    //
    //        public PigEffect(Skill skill, long duration, Creature creature) {
    //            super(skill, "MassPiggify", duration);
    //            this.creature = creature;
    //            this.types.add(EffectType.DISPELLABLE);
    //            this.types.add(EffectType.HARMFUL);
    //            this.types.add(EffectType.DISABLE);
    //        }
    //
    //        @Override
    //        public void applyToMonster(Monster monster) {
    //            super.applyToMonster(monster);
    //            creature.setPassenger(monster.getEntity());
    //            creatures.put(creature, monster);
    //        }
    //
    //        @Override
    //        public void applyToHero(Hero hero) {
    //            super.applyToHero(hero);
    //            Player player = hero.getPlayer();
    //            creature.setPassenger(player);
    //            creatures.put(creature, hero);
    //        }
    //
    //        @Override
    //        public void removeFromMonster(Monster rider) {
    //            super.removeFromMonster(rider);
    //            creatures.remove(creature);
    //            creature.remove();
    //        }
    //
    //        @Override
    //        public void removeFromHero(Hero hero) {
    //            super.removeFromHero(hero);
    //            creatures.remove(creature);
    //            creature.remove();
    //        }
    //    }

    // TEMP EXPLOIT FIX. JUST A TWEAKED ROOT
    private class PigEffect extends PeriodicExpirableEffect {

        private final Creature creature;
        private Location loc;

        public PigEffect(Skill skill, Player applier, int duration, Creature creature) {
            super(skill, "MassPiggify", applier, 100, duration);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.HARMFUL);
            types.add(EffectType.STUN);
            types.add(EffectType.DISABLE);
            types.add(EffectType.MAGIC);

            this.creature = creature;

            addMobEffect(2, (int) (duration / 1000) * 20, 127, false);      // Max slowness
            addMobEffect(8, (int) (duration / 1000) * 20, 128, false);      // Max negative jump boost
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
            creature.setPassenger(monster.getEntity());
            creatures.put(creature, monster);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            final Player player = hero.getPlayer();
            loc = hero.getPlayer().getLocation();

            // Don't allow an entangled player to sprint.
            final int currentHunger = player.getFoodLevel();
            player.setFoodLevel(1);
            player.setSprinting(false);

            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
            {
                public void run()
                {
                    player.setFoodLevel(currentHunger);
                }
            }, 0L);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            creatures.remove(creature);
            creature.remove();
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            creatures.remove(creature);
            creature.remove();
        }

        @Override
        public void tickHero(Hero hero) {
            final Location location = hero.getPlayer().getLocation();
            if ((location.getX() != loc.getX()) || (location.getZ() != loc.getZ())) {

                // If they have any velocity, we wish to remove it.
                Player player = hero.getPlayer();
                player.setVelocity(new Vector(0, 0, 0));

                // Retain the player's Y position and facing directions
                loc.setYaw(location.getYaw());
                loc.setPitch(location.getPitch());
                loc.setY(location.getY());

                // Teleport the Player back into place.
                player.teleport(loc);
            }
        }

        @Override
        public void tickMonster(Monster monster) {}
    }
}
