package com.herocraftonline.heroes.characters.skill.reborn.druid;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.ArrayList;
import java.util.List;

public class SkillYggdrasilsTouch extends ActiveSkill {

    public SkillYggdrasilsTouch(Heroes plugin) {
        super(plugin, "YggdrasilsTouch");
        setDescription("You mark all nearby Allies with Yggdrasil's touch. After a short period Yggdrasil's touch explodes and heals you and all nearby allies");
        setUsage("/skill yggdrasilstouch");
        setArgumentRange(0, 0);
        setIdentifiers("skill yggdrassilstouch");
        setTypes(SkillType.HEALING, SkillType.AREA_OF_EFFECT, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING, 60, false);
        int particleradius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);

        return getDescription().replace("$1", healing + "").replace("$2", radius + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.HEALING.node(), 60);
        node.set(SkillSetting.RADIUS.node(), 5);
        node.set(SkillSetting.DURATION.node(), 5000);
        return node;
    }

    //Math for particle circle
    public ArrayList<Location> circle(Location centerPoint, int particleAmount, double circleRadius) {
        World world = centerPoint.getWorld();

        double increment = (2 * Math.PI) / particleAmount;

        ArrayList<Location> locations = new ArrayList<Location>();

        for (int i = 0; i < particleAmount; i++) {
            double angle = i * increment;
            double x = centerPoint.getX() + (circleRadius * Math.cos(angle));
            double z = centerPoint.getZ() + (circleRadius * Math.sin(angle));
            locations.add(new Location(world, x, centerPoint.getY(), z));
        }
        return locations;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING, 60, false);


        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);


        List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
        for (Entity entity : entities) {
            // Check to see if we've exceeded the max targets

            if (!(entity instanceof LivingEntity)) {
                continue;
            }

            LivingEntity target = (LivingEntity) entity;
            if (!damageCheck(player, target))
                continue;


        }
        final int radiusSquared = (int) Math.pow(SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false), 2);
        final Location heroLoc = player.getLocation();
        if (hero.getParty() == null) {
            player.sendMessage(ChatColor.GRAY + "Must be in a party");
            return SkillResult.CANCELLED;
        }

        for (final Hero partyHero : hero.getParty().getMembers()) {
            for (double r = 1; r < radius * 2; r++) {
                if (!player.getWorld().equals(partyHero.getPlayer().getWorld())) {
                    continue;
                }
                ArrayList<Location> particleLocations = circle(partyHero.getPlayer().getLocation(), 45, r / 2);
                if (partyHero.getPlayer().getLocation().distanceSquared(heroLoc) <= radiusSquared) {
                    partyHero.getPlayer().sendMessage( ChatColor.LIGHT_PURPLE + "You have been touched by Yggdrasils");
                    final HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(partyHero, healing, this, hero);
                    this.plugin.getServer().getPluginManager().callEvent(hrhEvent);
                    if (hrhEvent.isCancelled()) {
                        player.sendMessage(ChatColor.GRAY + "Unable to heal the target at this time!");
                        return SkillResult.CANCELLED;
                    }

                    for (int i = 0; i < particleLocations.size(); i++) {
                        MarkBuff markBuff = new MarkBuff(this, player, duration);
                        partyHero.addEffect(markBuff);
                        player.getWorld().spawnParticle(Particle.HEART, particleLocations.get(i), 1, 0, 0.1, 0, 0.1);
                    }
                }
            }
        }
//        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.2F);
//        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1.0F, 1.2F);

        return SkillResult.NORMAL;
    }

    


    public class MarkBuff extends ExpirableEffect {

        public MarkBuff(Skill skill, Player applier, long duration) {
            super(skill, "MarkBuff", applier, duration);
            types.add(EffectType.HEALING);


        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);


        }
    }
}