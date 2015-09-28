package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.HeroLeavePartyEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.PeriodicHealEffect;
import com.herocraftonline.heroes.characters.effects.common.AttributeIncreaseEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.totem.SkillBaseTotem;
import com.herocraftonline.heroes.characters.skill.skills.totem.Totem;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SkillHealingTotem extends SkillBaseTotem {

    public SkillHealingTotem(Heroes plugin) {
        super(plugin, "HealingTotem");
        setArgumentRange(0,0);
        setUsage("/skill healingtotem");
        setIdentifiers("skill healingtotem");
        setDescription("Places a healing totem at target location that heals allied players for $1 HP per second in a $2 radius. Lasts for $3 seconds.");
        setTypes(SkillType.HEALING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE, SkillType.AREA_OF_EFFECT);
        material = Material.MYCEL;
    }

    @Override
    public String getDescription(Hero h) {
        return getDescription()
                .replace("$1", getHealing(h) + "")
                .replace("$2", getRange(h) + "")
                .replace("$3", getDuration(h)*0.001 + "");
    }

    // Old code for old HealingTotem
    /*@Override
    public void usePower(Hero hero, Totem totem) {
        Location totemLoc = totem.getLocation();
        
        Set<Hero> party;
        double rangeSquared = Math.pow(getRange(hero), 2);
        
        if(hero.hasParty()) {
            party = hero.getParty().getMembers();
        }
        else {
            party = new HashSet<>(Arrays.asList(hero));
        }
        
        for(Hero member : party) {
            Location memberLoc = member.getPlayer().getLocation();
            if(memberLoc.getWorld() != totemLoc.getWorld() || memberLoc.distanceSquared(totemLoc) > rangeSquared) {
                continue;
            }
            HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(member, getHealing(hero), this, hero);
            Bukkit.getPluginManager().callEvent(hrhEvent);
            if(!hrhEvent.isCancelled()) {
                member.heal(hrhEvent.getAmount());
            }
        }
    }*/
    
    @Override
    public void usePower(Hero hero, Totem totem) {
        Location totemLoc = totem.getLocation();

        Set<Hero> party = hero.hasParty() ? hero.getParty().getMembers() : new HashSet<>(Arrays.asList(hero));
        double rangeSquared = Math.pow(getRange(hero), 2);
        Player heroP = hero.getPlayer();

        for(Hero member : party) {
            Location memberLoc = member.getPlayer().getLocation();
            if(member.hasEffect("HealingTotemHealEffect")) {
                if(memberLoc.getWorld() != totemLoc.getWorld()) {
                    PeriodicHealEffect oldEffect = (PeriodicHealEffect) member.getEffect("HealingTotemHealEffect");
                    if(oldEffect.getApplier() == heroP) {
                        oldEffect.expire();
                    }
                }
                continue;
            }
            else if(memberLoc.getWorld() == totemLoc.getWorld() && memberLoc.distanceSquared(totemLoc) <= rangeSquared) {
                PeriodicHealEffect hEffect = new PeriodicHealEffect(this, "HealingTotemHealEffect", heroP, totem.getEffect().getPeriod(), totem.getEffect().getRemainingTime(), getHealing(hero));
                final Player memberP = member.getPlayer();
                new BukkitRunnable() {

                    private Location location = memberP.getLocation();

                    private double time = 0;

                    @SuppressWarnings("deprecation")
                    @Override
                    public void run() {
                        if (time < 1.0) {
                            memberP.getLocation(location).add(0.7 * Math.sin(time * 16), time * 2.2, 0.7 * Math.cos(time * 16));
                            /* This is the new Particle API system for Spigot - the first few int = id, data, offsetX/Y/Z, speed, count, radius)
                             * offset controls how spread out the particles are
                             * id and data only work for two particles: ITEM_BREAK and TILE_BREAK
                             * */
                            memberP.getWorld().spigot().playEffect(location, Effect.HEART, 0, 0, 0, 0, 0, 0.1f, 1, 16);
                        } else {
                            memberP.getLocation(location).add(0, 2.3, 0);
                            memberP.getWorld().spigot().playEffect(location, Effect.TILE_BREAK, Material.REDSTONE_BLOCK.getId(), 0, 0, 0, 0, 1f, 500, 16);
                            cancel();
                        }
                        time += 0.01;
                    }
                }.runTaskTimer(plugin, 20, 1);
                member.addEffect(hEffect);
            }
        }
    }
    
    @Override
    public void totemDestroyed(Hero hero, Totem totem) {
        Set<Hero> party = hero.hasParty() ? hero.getParty().getMembers() : new HashSet<>(Arrays.asList(hero));
        Player heroP = hero.getPlayer();

        for(Hero member : party) {
            if(member.hasEffect("HealingTotemHealEffect")) {
                PeriodicHealEffect oldEffect = (PeriodicHealEffect) member.getEffect("HealingTotemHealEffect");
                if(oldEffect.getApplier() == heroP) {
                    oldEffect.expire();
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeroLeaveParty(HeroLeavePartyEvent event) {
        Hero hero = event.getHero();
        if(hero.hasEffect("HealingTotemHealEffect")) {
            ((PeriodicHealEffect) hero.getEffect("HealingTotemHealEffect")).expire();
        }
    }

    @Override
    public ConfigurationSection getSpecificDefaultConfig(ConfigurationSection node) {
        node.set(SkillSetting.HEALING.node(), 25.0);
        node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 1.0);
        return node;
    }

    // Methods to grab config info that is specific to this skill
    public double getHealing(Hero h) {
        return SkillConfigManager.getUseSetting(h, this, SkillSetting.HEALING, 25.0, false) + SkillConfigManager.getUseSetting(h, this, SkillSetting.HEALING_INCREASE_PER_WISDOM, 1.0, false) * h.getAttributeValue(AttributeType.WISDOM);
    }

}