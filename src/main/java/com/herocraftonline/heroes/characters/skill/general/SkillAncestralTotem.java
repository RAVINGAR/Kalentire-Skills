package com.herocraftonline.heroes.characters.skill.general;

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
import com.herocraftonline.heroes.chat.ChatComponents;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class SkillAncestralTotem extends SkillBaseTotem implements Listener {

    Map<Hero, List<LivingEntity>> afflictedTargets;
    
    public SkillAncestralTotem(Heroes plugin) {
        super(plugin, "AncestralTotem");
        setArgumentRange(0,0);
        setUsage("/skill ancestraltotem");
        setIdentifiers("skill ancestraltotem");
        setDescription("Places an ancestral totem at target location that educates the intelligence of party members in a $1 radius, increasing it by $2. Lasts for $3 second(s).");
        setTypes(SkillType.BUFFING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE, SkillType.AREA_OF_EFFECT);
        material = Material.BOOKSHELF;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getDescription(Hero h) {
        return getDescription()
                .replace("$1", getRange(h) + "")
                .replace("$2", getIntellectIncrease(h) + "")
                .replace("$3", getDuration(h)*0.001 + "");
    }

    @Override
    public void usePower(Hero hero, Totem totem) {
        Location totemLoc = totem.getLocation();

        Set<Hero> party = hero.hasParty() ? hero.getParty().getMembers() : new HashSet<>(Collections.singletonList(hero));
        double rangeSquared = Math.pow(getRange(hero), 2);
        Player heroP = hero.getPlayer();

        for(Hero member : party) {
            Location memberLoc = member.getPlayer().getLocation();
            if(member.hasEffect("AncestralTotemIntellectEffect")) {
                if(memberLoc.getWorld() != totemLoc.getWorld()) {
                    AttributeIncreaseEffect oldEffect = (AttributeIncreaseEffect) member.getEffect("AncestralTotemIntellectEffect");
                    if(oldEffect.getApplier() == heroP) {
                        oldEffect.expire();
                    }
                }
                continue;
            }
            else if(memberLoc.getWorld() == totemLoc.getWorld() && memberLoc.distanceSquared(totemLoc) <= rangeSquared) {
                AttributeIncreaseEffect mEffect = new AttributeIncreaseEffect(this, "AncestralTotemIntellectEffect", heroP, totem.getEffect().getRemainingTime(), AttributeType.INTELLECT, (int)getIntellectIncrease(hero), getApplyText(), getExpireText());
                final Player memberP = member.getPlayer();
                new BukkitRunnable() {

                    private final Location location = memberP.getLocation();

                    private double time = 0;

                    @Override
                    public void run() {
                        if (time < 1.0) {
                            memberP.getLocation(location).add(0.7 * Math.sin(time * 16), time * 2.2, 0.7 * Math.cos(time * 16));
                            /* This is the new Particle API system for Spigot - the first few int = id, data, offsetX/Y/Z, speed, count, radius)
                             * offset controls how spread out the particles are
                             * id and data only work for two particles: ITEM_BREAK and TILE_BREAK
                             * */
                            //memberP.getWorld().spigot().playEffect(location, Effect.WATERDRIP, 0, 0, 0, 0, 0, 0.1f, 1, 16);
                            memberP.getWorld().spawnParticle(Particle.DRIP_WATER, location, 1, 0, 0, 0.1);
                        } else {
                            memberP.getLocation(location).add(0, 2.3, 0);
                            //memberP.getWorld().spigot().playEffect(location, Effect.TILE_BREAK, Material.WATER.getId(), 0, 0, 0, 0, 1f, 500, 16);
                            memberP.getWorld().spawnParticle(Particle.BLOCK_CRACK, location, 500, 0, 0, 0, 1, Bukkit.createBlockData(Material.WATER));
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
        Set<Hero> party = hero.hasParty() ? hero.getParty().getMembers() : new HashSet<>(Collections.singletonList(hero));
        Player heroP = hero.getPlayer();

        for(Hero member : party) {
            if(member.hasEffect("AncestralTotemIntellectEffect")) {
                AttributeIncreaseEffect oldEffect = (AttributeIncreaseEffect) member.getEffect("AncestralTotemIntellectEffect");
                if(oldEffect.getApplier() == heroP) {
                    oldEffect.expire();
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeroLeaveParty(HeroLeavePartyEvent event) {
        Hero hero = event.getHero();
        if(hero.hasEffect("AncestralTotemIntellectEffect")) {
            ((AttributeIncreaseEffect) hero.getEffect("AncestralTotemIntellectEffect")).expire();
        }
    }
    
    @Override
    public ConfigurationSection getSpecificDefaultConfig(ConfigurationSection node) {
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "$1 feels the Ancestral knowledge within them!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "$1's intelligence returns to normal.");
        node.set("intellect-increase", 5);
        node.set("intellect-increase-per-wisdom", 0.1);
        return node;
    }

    // Methods to grab config info that is specific to this skill
    public double getIntellectIncrease(Hero h) {
        return SkillConfigManager.getUseSetting(h, this, "intellect-increase", 5, false) + SkillConfigManager.getUseSetting(h, this, "intellect-increase-per-wisdom", 0.1, false) * h.getAttributeValue(AttributeType.WISDOM);
    }

    public String getApplyText() {
        return SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "$1 feels the Ancestral knowledge within them!");
    }

    public String getExpireText() {
        return SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "$1's intelligence returns to normal.");
    }

}