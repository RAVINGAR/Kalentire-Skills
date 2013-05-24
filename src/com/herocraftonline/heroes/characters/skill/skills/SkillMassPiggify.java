package com.herocraftonline.heroes.characters.skill.skills;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
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

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;

public class SkillMassPiggify extends ActiveSkill {
    
    private Map<Entity, CharacterTemplate> creatures = new HashMap<Entity, CharacterTemplate>();

    public SkillMassPiggify(Heroes plugin) {
        super(plugin, "MassPiggify");
        setDescription("You force targets within $1 meters to ride a pig for $2 seconds.");
        setUsage("/skill masspiggify");
        setArgumentRange(0, 0);
        setIdentifiers("skill masspiggify");
        setTypes(SkillType.DEBUFF, SkillType.SILENCABLE, SkillType.HARMFUL);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(), plugin);
    }
    
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.RADIUS.node(), 5);
        node.set(SkillSetting.DURATION.node(), 10000);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
    	Player player = hero.getPlayer();
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);
        List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
        //boolean didHit = false;
        for (Entity entity : entities) {
            if(!(entity instanceof LivingEntity)) {
                    continue;
            }
            LivingEntity target = (LivingEntity)entity;
            if (!damageCheck(player, target)) {
                continue;
            }
            //didHit = true;
            EntityType type = (target.getLocation().getBlock().getType().equals(Material.WATER) || 
            					target.getLocation().getBlock().getType().equals(Material.STATIONARY_WATER) ? 
            							EntityType.SQUID : EntityType.PIG);
            
            Entity creature = target.getWorld().spawnEntity(target.getLocation(), type);
            long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
            plugin.getCharacterManager().getCharacter(target).addEffect(new PigEffect(this, duration, (Creature) creature));
        }
        /*
        if(!didHit) {
            Messaging.send(player, "No valid targets within range!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        */
        player.getWorld().playEffect(player.getLocation(), Effect.ENDER_SIGNAL, 3);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.PIG_DEATH , 0.8F, 1.0F); 
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.PIG_IDLE , 0.8F, 1.0F); 
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }
    
    public class PigEffect extends ExpirableEffect {

        private final Creature creature;

        public PigEffect(Skill skill, long duration, Creature creature) {
            super(skill, "MassPiggify", duration);
            this.creature = creature;
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.HARMFUL);
            this.types.add(EffectType.DISABLE);
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
            Player player = hero.getPlayer();
            creature.setPassenger(player);
            creatures.put(creature, hero);
        }

        @Override
        public void removeFromMonster(Monster rider) {
            super.removeFromMonster(rider);
            creatures.remove(creature);
            creature.remove();
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            creatures.remove(creature);
            creature.remove();
        }
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
                    } else if (subEvent.getDamager() instanceof Projectile && ((Projectile) subEvent
                    		.getDamager()).getShooter().equals(event.getEntity().getPassenger())) {
                        event.setCancelled(true);
                        return;
                    }
                }
                CharacterTemplate character = creatures.remove(event.getEntity());
                character.removeEffect(character.getEffect("MassPiggify"));
            } else if (event.getEntity() instanceof LivingEntity) {
                CharacterTemplate character = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
                if (character.hasEffect("MassPiggify")) {
                    character.removeEffect(character.getEffect("MassPiggify"));
                }
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);
        return getDescription().replace("$1", radius + "").replace("$2", duration / 1000 + "");
    }

}
