package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.totem.SkillBaseTotem;
import com.herocraftonline.heroes.characters.skill.skills.totem.Totem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

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

    @Override
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