package com.herocraftonline.dev.heroes.skill.skills;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.herocraftonline.dev.heroes.util.Util;

public class SkillForage extends ActiveSkill{
 
    public SkillForage(Heroes plugin) {
        super(plugin, "Forage");
        setDescription("Forages for food.");
        setUsage("/skill forage");
        setArgumentRange(0, 0);
        setIdentifiers("skill forage");
        setTypes(SkillType.ITEM, SkillType.EARTH, SkillType.KNOWLEDGE);
    }
    
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("forest.items",  Arrays.asList(new String[] {"APPLE"}));
        node.set("forest.chance", .01);
        node.set("forest.max-found", 3);
        node.set("plains.items",  Arrays.asList(new String[] {"WHEAT", "MELON"}));
        node.set("plains.chance", .01);
        node.set("plains.max-found", 3);
        node.set("water.items",  Arrays.asList(new String[] {"RAW_FISH"}));
        node.set("water.chance", .01);
        node.set("water.max-found", 3);
        node.set("swamp.items", Arrays.asList(new String[] {"RED_MUSHROOM", "BROWN_MUSHROOM", "RAW_FISH", "VINE"}));
        node.set("swamp.chance", .01);
        node.set("swamp.max-found", 4);
        node.set("desert.items", Arrays.asList(new String[] {"CACTUS", "SUGAR_CANE"}));
        node.set("desert.chance", .005);
        node.set("desert.max-found", 2);
        node.set("hell.items", Arrays.asList(new String[] {"ROTTEN_FLESH" }));
        node.set("hell.chance", .005);
        node.set("hell.max-found", 1);
        node.set("sky.items", Arrays.asList(new String[] {"VINE"}));
        node.set("sky.chance", .01);
        node.set("sky.max-found", 3);
        node.set("ice.items", Arrays.asList(new String[] {"RAW_FISH"}));
        node.set("ice.chance", 0.005D);
        node.set("ice.max-found", 1);
        node.set("mushroom.items", Arrays.asList(new String[] {"RED_MUSHROOM", "BROWN_MUSHROOM", "HUGE_MUSHROOM_1", "HUGE_MUSHROOM_2"}));
        node.set("mushroom.chance", 0.1);
        node.set("mushroom.max-found", 2);
        node.set("default.items", new ArrayList<String>());
        return node;
    }
    
    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        Location loc = player.getLocation();
        Biome biome = player.getWorld().getBiome(loc.getBlockX(), loc.getBlockZ());

        double chance = 0;
        int maxFinds = 0;
        //Get the list of foragable stuff here
        List<String> materialNames = new ArrayList<String>();
        switch (biome) {
        default: 
            materialNames.addAll(getSetting(hero, "default.items", new ArrayList<String>()));
        case TAIGA :
        case TUNDRA :
        case ICE_DESERT :
        case FROZEN_OCEAN :
        case FROZEN_RIVER :
        case ICE_PLAINS :
        case ICE_MOUNTAINS :
            materialNames.addAll(getSetting(hero, "ice.items", new ArrayList<String>()));
            chance = getSetting(hero, "ice.chance", .01, false) * hero.getLevel(this);
            maxFinds = getSetting(hero, "ice.max-found", 3, false);
            break;
        case FOREST :
        case RAINFOREST :
        case SEASONAL_FOREST :
        case EXTREME_HILLS :
            materialNames.addAll(getSetting(hero, "forest.items", Arrays.asList(new String[] {"APPLE", "MELON"})));
            chance = getSetting(hero, "forest.chance", .01, false) * hero.getLevel(this);
            maxFinds = getSetting(hero, "forest.max-found", 3, false);
            break;
        case SWAMPLAND :
            materialNames.addAll(getSetting(hero, "swamp.items", Arrays.asList(new String[] {"RED_MUSHROOM", "BROWN_MUSHROOM", "RAW_FISH", "VINE"})));
            chance = getSetting(hero, "swamp.chance", .01, false) * hero.getLevel(this);
            maxFinds = getSetting(hero, "swamp.max-found", 4, false);
            break;
        case SAVANNA :
        case SHRUBLAND :
        case PLAINS :
            materialNames.addAll(getSetting(hero, "plains.items", Arrays.asList(new String[] {"WHEAT"})));
            chance = getSetting(hero, "plains.chance", .01, false) * hero.getLevel(this);
            maxFinds = getSetting(hero, "plains.max-found", 3, false);
            break;
        case DESERT :
            materialNames.addAll(getSetting(hero, "desert.items", Arrays.asList(new String[] {"CACTUS"})));
            chance = getSetting(hero, "desert.chance", .005, false) * hero.getLevel(this);
            maxFinds = getSetting(hero, "desert.max-found", 2, false);
            break;
        case OCEAN :
        case RIVER :
            materialNames.addAll(getSetting(hero, "water.items", Arrays.asList(new String[] {"RAW_FISH"})));
            chance = getSetting(hero, "water.chance", .01, false) * hero.getLevel(this);
            maxFinds = getSetting(hero, "water.max-found", 3, false);
            break;
        case MUSHROOM_SHORE :
        case MUSHROOM_ISLAND :
        	materialNames.addAll(getSetting(hero, "mushroom.items", Arrays.asList(new String[] {"RED_MUSHROOM", "BROWN_MUSHROOM", "HUGE_MUSHROOM_1", "HUGE_MUSHROOM_2"})));
        	chance = getSetting(hero, "mushroom.chance", .01, false) * hero.getLevel(this);
        	maxFinds = getSetting(hero, "mushroom.max-found", 2, false);
        case HELL :
            materialNames.addAll(getSetting(hero, "hell.items", Arrays.asList(new String[] {"ROTTEN_FLESH"})));
            chance = getSetting(hero, "hell.chance", .005, false) * hero.getLevel(this);
            maxFinds = getSetting(hero, "hell.max-found", 1, false);
            break;
        case SKY :
            materialNames.addAll(getSetting(hero, "sky.items", Arrays.asList(new String[] {"VINE"})));
            chance = getSetting(hero, "sky.chance", .01, false) * hero.getLevel(this);
            maxFinds = getSetting(hero, "sky.max-found", 3, false);
            break;
        }
        
        List<Material> materials = new ArrayList<Material>();
        for (String name : materialNames) {
            try {
                materials.add(Material.valueOf(name));
            } catch (IllegalArgumentException e) {
                continue;
            }
        }
        
        if (materials.isEmpty() || Util.rand.nextDouble() >= chance || maxFinds <= 0) {
            Messaging.send(player, "You found nothing while foraging.");
            return SkillResult.NORMAL;
        } 
        
        int numItems = Util.rand.nextInt(maxFinds) + 1;
        for (int i = 0; i < numItems; i++) {
            ItemStack item = new ItemStack(materials.get(Util.rand.nextInt(materials.size())), 1);
            
            Map<Integer, ItemStack> leftOvers = player.getInventory().addItem(item);
            // Drop any leftovers we couldn't add to the players inventory
            if (!leftOvers.isEmpty()) {
                for (ItemStack leftOver : leftOvers.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftOver);
                }
                Messaging.send(player, "Items have been dropped at your feet!");
            }
        }
        Util.syncInventory(player, plugin);
        broadcastExecuteText(hero);
        return SkillResult.NORMAL; 
    }
}