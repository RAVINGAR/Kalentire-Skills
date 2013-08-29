package com.herocraftonline.heroes.characters.skill.skills;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.server.v1_6_R2.EntityLiving;
import net.minecraft.server.v1_6_R2.MobEffectList;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_6_R2.entity.CraftLivingEntity;
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
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Util;

public class SkillPiggify extends TargettedSkill {

    private Map<Entity, CharacterTemplate> creatures = new HashMap<Entity, CharacterTemplate>();

    public SkillPiggify(Heroes plugin) {
        super(plugin, "Piggify");
        setDescription("You force your target to ride a pig for $1 seconds.");
        setUsage("/skill piggify");
        setArgumentRange(0, 0);
        setIdentifiers("skill piggify");
        setTypes(SkillType.DISABLING, SkillType.SILENCABLE, SkillType.AGGRESSIVE, SkillType.INTERRUPTING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 3000, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 50, false);
        duration += hero.getAttributeValue(AttributeType.CHARISMA) * durationIncrease;

        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 3000);
        node.set(SkillSetting.DURATION_INCREASE_PER_CHARISMA.node(), 50);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 3000, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 50, false);
        duration += hero.getAttributeValue(AttributeType.CHARISMA) * durationIncrease;

        EntityType type = (target.getLocation().getBlock().getType().equals(Material.WATER) || target.getLocation().getBlock().getType().equals(Material.STATIONARY_WATER) ? EntityType.SQUID : EntityType.PIG);
        Entity creature = target.getWorld().spawnEntity(target.getLocation(), type);
        plugin.getCharacterManager().getCharacter(target).addEffect(new PigEffect(this, duration, (Creature) creature));

        player.getWorld().playSound(player.getLocation(), Sound.ZOMBIE_PIG_HURT, 0.8F, 1.0F);

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
                character.removeEffect(character.getEffect("Piggify"));
            }
            else if (event.getEntity() instanceof LivingEntity) {
                CharacterTemplate character = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
                if (character.hasEffect("Piggify")) {
                    character.removeEffect(character.getEffect("Piggify"));
                }
            }
        }
    }

    // ACTUAL PIGGIFY. SWITCH TO THIS AFTER FIXING EXPLOITS.
    //    public class PigEffect extends ExpirableEffect {
    //
    //        private final Creature creature;
    //
    //        public PigEffect(Skill skill, long duration, Creature creature) {
    //            super(skill, "Piggify", duration);
    //            this.creature = creature;
    //            
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

        public PigEffect(Skill skill, int duration, Creature creature) {
            super(skill, "Piggify", 100, duration);
            this.creature = creature;

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.HARMFUL);
            types.add(EffectType.STUN);
            types.add(EffectType.DISABLE);
            types.add(EffectType.MAGIC);

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

            // Don't allow an entangled player to sprint. If they are sprinting, turn it off.
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
        public void removeFromHero(final Hero hero) {

            Player player = hero.getPlayer();
            EntityLiving el = ((CraftLivingEntity) player).getHandle();

            if (el.hasEffect(MobEffectList.POISON) || el.hasEffect(MobEffectList.WITHER) || el.hasEffect(MobEffectList.HARM)) {
                // If they have a harmful effect present when removing the ability, delay effect removal by a bit.
                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override
                    public void run() {
                        PigEffect.super.removeFromHero(hero);
                        creatures.remove(creature);
                        creature.remove();
                    }
                }, (long) (0.2 * 20));
            }
            else {
                super.removeFromHero(hero);
                creatures.remove(creature);
                creature.remove();
            }
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
