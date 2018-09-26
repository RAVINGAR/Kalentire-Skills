package com.herocraftonline.heroes.characters.skill.public1;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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

import java.util.HashMap;
import java.util.Map;

public class SkillPiggify extends TargettedSkill {

    private final Map<Entity, CharacterTemplate> creatures = new HashMap<Entity, CharacterTemplate>();

    public SkillPiggify(Heroes plugin) {
        super(plugin, "Piggify");
        this.setDescription("You force your target to ride a pig for $1 seconds.");
        this.setUsage("/skill piggify <target>");
        this.setArgumentRange(0, 1);
        this.setIdentifiers("skill piggify");
        this.setTypes(SkillType.DEBUFFING, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 10000);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        final EntityType type = (target.getLocation().getBlock().getType().equals(Material.WATER) ? EntityType.SQUID : EntityType.PIG);

        final Entity creature = target.getWorld().spawnEntity(target.getLocation(), type);
        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        this.plugin.getCharacterManager().getCharacter(target).addEffect(new PigEffect(this, hero.getPlayer(), duration, (Creature) creature));
        this.broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    public class PigEffect extends ExpirableEffect {

        private final Creature creature;

        public PigEffect(Skill skill, Player applier, long duration, Creature creature) {
            super(skill, "Piggify", applier, duration);
            this.creature = creature;
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.HARMFUL);
            this.types.add(EffectType.DISABLE);
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
            this.creature.setPassenger(monster.getEntity());
            SkillPiggify.this.creatures.put(this.creature, monster);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            this.creature.setPassenger(player);
            SkillPiggify.this.creatures.put(this.creature, hero);
        }

        @Override
        public void removeFromMonster(Monster rider) {
            super.removeFromMonster(rider);
            SkillPiggify.this.creatures.remove(this.creature);
            this.creature.remove();
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            SkillPiggify.this.creatures.remove(this.creature);
            this.creature.remove();
        }
    }

    public class SkillEntityListener implements Listener {

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onEntityDamage(EntityDamageEvent event) {
            if (SkillPiggify.this.creatures.containsKey(event.getEntity())) {
                if (event instanceof EntityDamageByEntityEvent) {
                    final EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
                    if (subEvent.getDamager().equals(event.getEntity().getPassenger())) {
                        event.setCancelled(true);
                        return;
                    } else if ((subEvent.getDamager() instanceof Projectile) && ((Projectile) subEvent.getDamager()).getShooter().equals(event.getEntity().getPassenger())) {
                        event.setCancelled(true);
                        return;
                    }
                }
                final CharacterTemplate character = SkillPiggify.this.creatures.remove(event.getEntity());
                character.removeEffect(character.getEffect("Piggify"));
            } else if (event.getEntity() instanceof LivingEntity) {
                final CharacterTemplate character = SkillPiggify.this.plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
                if (character.hasEffect("Piggify")) {
                    character.removeEffect(character.getEffect("Piggify"));
                }
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        return this.getDescription().replace("$1", (duration / 1000) + "");
    }
}
