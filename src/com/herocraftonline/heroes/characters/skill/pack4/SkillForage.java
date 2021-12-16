package com.herocraftonline.heroes.characters.skill.pack4;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.util.Properties;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class SkillForage extends ActiveSkill {

    private static final int SUBVERSION = 16;

    public SkillForage(Heroes plugin) {
        super(plugin, "Forage");
        setDescription("You forage for food.");
        setUsage("/skill forage");
        setArgumentRange(0, 0);
        setIdentifiers("skill forage");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("forest.items", Arrays.asList("APPLE"));
        node.set("forest.chance", .01);
        node.set("forest.max-found", 3);
        node.set("plains.items", Arrays.asList("WHEAT", "MELON"));
        node.set("plains.chance", .01);
        node.set("plains.max-found", 3);
        node.set("water.items", Arrays.asList("RAW_FISH"));
        node.set("water.chance", .01);
        node.set("water.max-found", 3);
        node.set("swamp.items", Arrays.asList("RED_MUSHROOM", "BROWN_MUSHROOM", "RAW_FISH", "VINE"));
        node.set("swamp.chance", .01);
        node.set("swamp.max-found", 4);
        node.set("desert.items", Arrays.asList("CACTUS", "SUGAR_CANE"));
        node.set("desert.chance", .005);
        node.set("desert.max-found", 2);
        node.set("hell.items", Arrays.asList("ROTTEN_FLESH"));
        node.set("hell.chance", .005);
        node.set("hell.max-found", 1);
        node.set("sky.items", Arrays.asList("VINE"));
        node.set("sky.chance", .01);
        node.set("sky.max-found", 3);
        node.set("ice.items", Arrays.asList("RAW_FISH"));
        node.set("ice.chance", 0.005D);
        node.set("ice.max-found", 1);
        node.set("mushroom.items", Arrays.asList("RED_MUSHROOM", "BROWN_MUSHROOM", "HUGE_MUSHROOM_1", "HUGE_MUSHROOM_2"));
        node.set("mushroom.chance", 0.1);
        node.set("mushroom.max-found", 2);
        node.set("default.items", new ArrayList<>());
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
        List<String> materialNames = new ArrayList<>();

        //FIXME Someone with more knowledge of the skill should address this switch statement
        switch (biome) {
        case TAIGA:
        case TAIGA_HILLS:
        case FROZEN_OCEAN:
        case FROZEN_RIVER:
        case ICE_SPIKES:
        case SNOWY_MOUNTAINS:
        //case ICE_FLATS:
        //case ICE_MOUNTAINS:
            materialNames.addAll(SkillConfigManager.getUseSetting(hero, this, "ice.items", new ArrayList<String>()));
            chance = SkillConfigManager.getUseSetting(hero, this, "ice.chance", .01, false) * hero.getHeroLevel(this);
            maxFinds = SkillConfigManager.getUseSetting(hero, this, "ice.max-found", 3, false);
            break;
        case FOREST:
        case BIRCH_FOREST_HILLS:
        case DARK_FOREST_HILLS:
        case WOODED_HILLS:
        case JUNGLE_HILLS:
        case BAMBOO_JUNGLE_HILLS:
        case SNOWY_TAIGA_HILLS:
        case GIANT_SPRUCE_TAIGA_HILLS:
        case GIANT_TREE_TAIGA_HILLS:
        case TALL_BIRCH_HILLS:
        //case FOREST_HILLS:
        //case EXTREME_HILLS:
        //case SMALLER_EXTREME_HILLS:
            materialNames.addAll(SkillConfigManager.getUseSetting(hero, this, "forest.items", Arrays.asList(new String[] { "APPLE", "MELON" })));
            chance = SkillConfigManager.getUseSetting(hero, this, "forest.chance", .01, false) * hero.getHeroLevel(this);
            maxFinds = SkillConfigManager.getUseSetting(hero, this, "forest.max-found", 3, false);
            break;
        case SWAMP:
        case SWAMP_HILLS:
            materialNames.addAll(SkillConfigManager.getUseSetting(hero, this, "swamp.items", Arrays.asList(new String[] { "RED_MUSHROOM", "BROWN_MUSHROOM", "RAW_FISH", "VINE" })));
            chance = SkillConfigManager.getUseSetting(hero, this, "swamp.chance", .01, false) * hero.getHeroLevel(this);
            maxFinds = SkillConfigManager.getUseSetting(hero, this, "swamp.max-found", 4, false);
            break;
        case PLAINS:
        case SUNFLOWER_PLAINS:
            materialNames.addAll(SkillConfigManager.getUseSetting(hero, this, "plains.items", Arrays.asList(new String[] { "WHEAT" })));
            chance = SkillConfigManager.getUseSetting(hero, this, "plains.chance", .01, false) * hero.getHeroLevel(this);
            maxFinds = SkillConfigManager.getUseSetting(hero, this, "plains.max-found", 3, false);
            break;
        case DESERT:
        case DESERT_HILLS:
        case DESERT_LAKES:
            materialNames.addAll(SkillConfigManager.getUseSetting(hero, this, "desert.items", Arrays.asList(new String[] { "CACTUS" })));
            chance = SkillConfigManager.getUseSetting(hero, this, "desert.chance", .005, false) * hero.getHeroLevel(this);
            maxFinds = SkillConfigManager.getUseSetting(hero, this, "desert.max-found", 2, false);
            break;
        case OCEAN:
        case RIVER:
        case BEACH:
        case SNOWY_BEACH:
            materialNames.addAll(SkillConfigManager.getUseSetting(hero, this, "water.items", Arrays.asList(new String[] { "RAW_FISH" })));
            chance = SkillConfigManager.getUseSetting(hero, this, "water.chance", .01, false) * hero.getHeroLevel(this);
            maxFinds = SkillConfigManager.getUseSetting(hero, this, "water.max-found", 3, false);
            break;
        case MUSHROOM_FIELDS:
        case MUSHROOM_FIELD_SHORE:
            materialNames.addAll(SkillConfigManager.getUseSetting(hero, this, "mushroom.items", Arrays.asList(new String[] { "RED_MUSHROOM", "BROWN_MUSHROOM", "HUGE_MUSHROOM_1", "HUGE_MUSHROOM_2" })));
            chance = SkillConfigManager.getUseSetting(hero, this, "mushroom.chance", .01, false) * hero.getHeroLevel(this);
            maxFinds = SkillConfigManager.getUseSetting(hero, this, "mushroom.max-found", 2, false);
            break;
        case THE_END:
        case SMALL_END_ISLANDS:
        case END_MIDLANDS:
        case END_HIGHLANDS:
        case END_BARRENS:
            materialNames.addAll(SkillConfigManager.getUseSetting(hero, this, "sky.items", Arrays.asList(new String[] { "VINE" })));
            chance = SkillConfigManager.getUseSetting(hero, this, "sky.chance", .01, false) * hero.getHeroLevel(this);
            maxFinds = SkillConfigManager.getUseSetting(hero, this, "sky.max-found", 3, false);
            break;
        default:
            switch (biome.name()) {
                case "NETHER", "NETHER_WASTES", "SOUL_SAND_VALLEY", "CRIMSON_FOREST", "WARPED_FOREST", "BASALT_DELTAS" -> {
                    materialNames.addAll(SkillConfigManager.getUseSetting(hero, this, "hell.items", Arrays.asList(new String[]{"ROTTEN_FLESH"})));
                    chance = SkillConfigManager.getUseSetting(hero, this, "hell.chance", .005, false) * hero.getHeroLevel(this);
                    maxFinds = SkillConfigManager.getUseSetting(hero, this, "hell.max-found", 1, false);
                }
            }

            materialNames.addAll(SkillConfigManager.getUseSetting(hero, this, "default.items", new ArrayList<String>()));
        }

        List<Material> materials = new ArrayList<>();
        for (String name : materialNames) {
            try {
                materials.add(Material.valueOf(name));
            }
            catch (IllegalArgumentException ignored) {
            }
        }

        if (materials.isEmpty() || Util.nextRand() >= chance || maxFinds <= 0) {
            player.sendMessage("You found nothing while foraging.");
            return SkillResult.NORMAL;
        }

        int numItems = Util.nextInt(maxFinds) + 1;
        for (int i = 0; i < numItems; i++) {
            ItemStack item = new ItemStack(materials.get(Util.nextInt(materials.size())), 1);

            Map<Integer, ItemStack> leftOvers = player.getInventory().addItem(item);
            // Drop any leftovers we couldn't add to the players inventory
            if (!leftOvers.isEmpty()) {
                for (ItemStack leftOver : leftOvers.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftOver);
                }
                player.sendMessage("Items have been dropped at your feet!");
            }
        }
        Util.syncInventory(player, plugin);
        player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 3);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENTITY_WOLF_HOWL, 0.5F, 1.0F);
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
}