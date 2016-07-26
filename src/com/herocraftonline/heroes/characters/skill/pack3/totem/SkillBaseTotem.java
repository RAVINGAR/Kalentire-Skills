package com.herocraftonline.heroes.characters.skill.pack3.totem;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.skills.totem.*;
import com.herocraftonline.heroes.characters.skill.skills.totem.Totem;
import com.herocraftonline.heroes.characters.skill.skills.totem.TotemEffect;
import com.herocraftonline.heroes.util.Messaging;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;

public abstract class SkillBaseTotem extends ActiveSkill {

    public Material material = Material.BEDROCK;
    
    public static ArrayList<Totem> totems = new ArrayList<Totem>();

    // Register the events in TOtemListener
    static {
        Bukkit.getServer().getPluginManager().registerEvents(new com.herocraftonline.heroes.characters.skill.skills.totem.TotemListener(Heroes.getInstance()), Heroes.getInstance());
    }
    
    // Constructor
    public SkillBaseTotem(Heroes plugin, String name) {
        super(plugin, name);
    }

    // Static methods to check info about Totems!
    public static boolean isTotemBlock(Block block) { 
        for(Totem totem : totems) {
            if(totem.getBlocks().contains(block))
                return true;
        }

        return false;
    }
    
    public static boolean isTotemCrystal(EnderCrystal crystal) { 
        for(Totem totem : totems) {
            if(totem.getCrystal() == crystal)
                return true;
        }

        return false;
    }

    public static boolean totemExistsInRadius(Block center, int radius) { 
        Vector min = center.getLocation().subtract(radius, radius, radius).toVector();
        Vector max = center.getLocation().add(radius, radius, radius).toVector();

        for(Totem totem : totems) {
            if(totem.getBlock(0).getLocation().toVector().isInAABB(min, max))
                return true;
        }

        return false;
    }
    
    // The actual Skill code
    public abstract void usePower(Hero hero, Totem totem);

    public void totemDestroyed(Hero hero, Totem totem) {
        // Nothing to do here in this class...
    }
     
    @SuppressWarnings("deprecation")
    @Override
    public SkillResult use(Hero hero, String[] args) {

        Location loc = hero.getPlayer().getTargetBlock((HashSet<Byte>)null,  20).getLocation().add(0, 1, 0);
        Totem totem = new Totem(this, loc, getFireOnNaturalRemove(hero), getRange(hero));

        if(hero.hasEffect("TotemEffect")) {
            hero.getPlayer().sendMessage(ChatColor.RED + "You already have an active totem!");
            return SkillResult.CANCELLED;
        }
        
        if(!totem.canCreateTotem(material)) {
            hero.getPlayer().sendMessage(ChatColor.RED + "Invalid totem location!");
            return SkillResult.CANCELLED;
        }
        
        hero.addEffect(new TotemEffect(this, totem, hero.getPlayer(), getPeriod(hero), getDuration(hero)));
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }
    
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 60000);
        node.set(SkillSetting.DURATION_INCREASE_PER_WISDOM.node(), 100);
        node.set("duration-per-level", 100);
        node.set("range", 5);
        node.set("range-per-level", 0.1);
        node.set(SkillSetting.PERIOD.node(), 1000L);
        node.set("fireOnNaturalRemove", true);
        node.set(SkillSetting.USE_TEXT.node(), Messaging.getSkillDenoter() + "%hero% placed a(n) " + getName().replace("Totem", "") + " totem!");
        return getSpecificDefaultConfig(node);
    }

    public long getDuration(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 60000, false) + (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_WISDOM, 100, false) * hero.getAttributeValue(AttributeType.WISDOM)) + (SkillConfigManager.getUseSetting(hero, this, "duration-per-level", 100, false) * hero.getSkillLevel(this));
    }

    public double getRange(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, "range", 5, false) + SkillConfigManager.getUseSetting(hero, this, "range-per-level", 0.1, false) * hero.getSkillLevel(this);
    }
    
    public long getPeriod(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1000, false);
    }
    
    public boolean getFireOnNaturalRemove(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, "fireOnNaturalRemove", true);
    }

    public Material getMaterial() {
        return material;
    }
    
    public ConfigurationSection getSpecificDefaultConfig(ConfigurationSection node) {
        return node;
    }
}
