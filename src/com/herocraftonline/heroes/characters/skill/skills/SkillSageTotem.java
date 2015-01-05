package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.HeroLeavePartyEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.AttributeIncreaseEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.totem.SkillBaseTotem;
import com.herocraftonline.heroes.characters.skill.skills.totem.Totem;
import com.herocraftonline.heroes.util.Messaging;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SkillSageTotem extends SkillBaseTotem implements Listener {

    Map<Hero, List<LivingEntity>> afflictedTargets;
    
    public SkillSageTotem(Heroes plugin) {
        super(plugin, "SageTotem");
        setArgumentRange(0,0);
        setUsage("/skill sagetotem");
        setIdentifiers("skill sagetotem");
        setDescription("Places a sage totem at target location that enlightens party members in a $1 radius, increasing Wisdom by $2. Lasts for $3 seconds.");
        setTypes(SkillType.BUFFING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE, SkillType.AREA_OF_EFFECT);
        material = Material.GLOWSTONE;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getDescription(Hero h) {
        return getDescription()
                .replace("$1", getRange(h) + "")
                .replace("$2", getWisdomIncrease(h) + "")
                .replace("$3", getDuration(h)*0.001 + "");
    }

    @Override
    public void usePower(Hero hero, Totem totem) {
        Location totemLoc = totem.getLocation();

        Set<Hero> party = hero.hasParty() ? hero.getParty().getMembers() : new HashSet<>(Arrays.asList(hero));
        double rangeSquared = Math.pow(getRange(hero), 2);
        Player heroP = hero.getPlayer();

        for(Hero member : party) {
            Location memberLoc = member.getPlayer().getLocation();
            if(member.hasEffect("SageTotemWisdomEffect")) {
                if(memberLoc.getWorld() != totemLoc.getWorld()) {
                    AttributeIncreaseEffect oldEffect = (AttributeIncreaseEffect) member.getEffect("SageTotemWisdomEffect");
                    if(oldEffect.getApplier() == heroP) {
                        oldEffect.expire();
                    }
                }
                continue;
            }
            else if(memberLoc.distanceSquared(totemLoc) <= rangeSquared) {
                AttributeIncreaseEffect mEffect = new AttributeIncreaseEffect(this, "SageTotemWisdomEffect", heroP, totem.getEffect().getRemainingTime(), AttributeType.WISDOM, (int)getWisdomIncrease(hero), getApplyText(), getExpireText());
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
                            memberP.getWorld().spigot().playEffect(location, Effect.COLOURED_DUST, 0, 0, 0, 0, 0, 0.1f, 1, 16);
                        } else {
                            memberP.getLocation(location).add(0, 2.3, 0);
                            memberP.getWorld().spigot().playEffect(location, Effect.TILE_BREAK, Material.LAPIS_BLOCK.getId(), 0, 0, 0, 0, 1f, 500, 16);
                            cancel();
                        }
                        time += 0.01;
                    }
                }.runTaskTimer(plugin, 20, 1);
                member.addEffect(mEffect);
            }
        }
    }

    @Override
    public void totemDestroyed(Hero hero, Totem totem) {
        Set<Hero> party = hero.hasParty() ? hero.getParty().getMembers() : new HashSet<>(Arrays.asList(hero));
        Player heroP = hero.getPlayer();

        for(Hero member : party) {
            if(member.hasEffect("SageTotemWisdomEffect")) {
                AttributeIncreaseEffect oldEffect = (AttributeIncreaseEffect) member.getEffect("SageTotemWisdomEffect");
                if(oldEffect.getApplier() == heroP) {
                    oldEffect.expire();
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeroLeaveParty(HeroLeavePartyEvent event) {
        Hero hero = event.getHero();
        if(hero.hasEffect("SageTotemWisdomEffect")) {
            ((AttributeIncreaseEffect) hero.getEffect("SageTotemWisdomEffect")).expire();
        }
    }
    
    @Override
    public ConfigurationSection getSpecificDefaultConfig(ConfigurationSection node) {
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "$1 is enlightened by the sage toem!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "$1 is unenlightened.");
        node.set("wisdom-increase", 5);
        node.set("wisdom-increase-per-intellect", 0.1);
        return node;
    }

    // Methods to grab config info that is specific to this skill
    public double getWisdomIncrease(Hero h) {
        return SkillConfigManager.getUseSetting(h, this, "wisdom-increase", 5, false) + SkillConfigManager.getUseSetting(h, this, "wisdom-increase-per-intellect", 0.1, false) * h.getAttributeValue(AttributeType.INTELLECT);
    }

    public String getApplyText() {
        return SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "$1 is enlightened by the sage toem!");
    }

    public String getExpireText() {
        return SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "$1 is unenlightened.");
    }

}