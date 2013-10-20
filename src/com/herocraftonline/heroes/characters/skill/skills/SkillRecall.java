package com.herocraftonline.heroes.characters.skill.skills;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.townships.HeroTowns;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

public class SkillRecall extends ActiveSkill {

    private boolean herotowns = false;
    private HeroTowns ht;
    private WorldGuardPlugin wgp;
    private boolean worldguard = false;

    public SkillRecall(Heroes plugin) {
        super(plugin, "Recall");
        setDescription("You recall to your marked location. If you are holding a Runestone, you recall to its stored location instead.");
        setUsage("/skill recall");
        setArgumentRange(0, 0);
        setIdentifiers("skill recall");
        setTypes(SkillType.SILENCABLE, SkillType.TELEPORTING);

        try {
            //            if (Bukkit.getServer().getPluginManager().getPlugin("HeroTowns") != null) {
            //                herotowns = true;
            //                ht = (HeroTowns) this.plugin.getServer().getPluginManager().getPlugin("HeroTowns");
            //            }
            //        }
            if (Bukkit.getServer().getPluginManager().getPlugin("WorldGuard") != null) {
                worldguard = true;
                wgp = (WorldGuardPlugin) this.plugin.getServer().getPluginManager().getPlugin("WorldGuard");
            }
        }
        catch (Exception e) {
            Heroes.log(Level.SEVERE, "SkillRecall: Could not get Residence or HeroTowns plugins! Region checking may not work!");
        }
    }

    public String getDescription(Hero hero) {
        return getDescription();
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.NO_COMBAT_USE.node(), Boolean.valueOf(true));
        node.set(SkillSetting.DELAY.node(), 10000);
        node.set(SkillSetting.REAGENT.node(), Integer.valueOf(331));
        node.set(SkillSetting.REAGENT_COST.node(), Integer.valueOf(10));

        return node;
    }

    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        // RUNESTONE RECALL FUNCTIONALITY
        ItemStack heldItem = player.getItemInHand();
        if (heldItem.getType() == Material.REDSTONE_BLOCK) {

            // We have a possible Runestone object.

            // Read the meta data (if it has any)
            ItemMeta metaData = heldItem.getItemMeta();
            List<String> loreData = metaData.getLore();

            // Ensure that we actually have meta data
            if (loreData != null) {

                // Ensure that there is at least three rows of meta-data available on the block
                if (loreData.size() > 2) {

                    // Get the uses on the Runestone and ensure it is greater than 1
                    String usesString = loreData.get(1);
                    usesString = usesString.toLowerCase();

                    // Strip the usesString of the "Uses:" text.
                    int currentIndexLocation = usesString.indexOf(":", 0) + 2;  // Set the start point
                    int endIndexLocation = usesString.length();                 // Get the end point for grabbing remaining uses data
                    String currentUsesString = usesString.substring(currentIndexLocation, endIndexLocation);

                    // Strip the currentUsesString of the "max uses" text.
                    currentIndexLocation = 0;
                    endIndexLocation = currentUsesString.indexOf("/", 0);
                    if (endIndexLocation != -1) {
                        // There is a possible maximum uses located within the usesString
                        currentUsesString = currentUsesString.substring(currentIndexLocation, endIndexLocation);
                    }

                    if (validateUsesValue(currentUsesString, player)) {
                        // We have a valid value for "uses". It is either a number, or "unlimited"

                        int uses = -1;
                        if (!currentUsesString.equals("unlimited"))
                            uses = Integer.parseInt(currentUsesString);    // Grab the uses from the string

                        // If it's empty, tell them to recharge it.
                        if (uses == 0) {
                            Messaging.send(player, "Runestone is out of uses and needs to be recharged.");
                            return SkillResult.FAIL;
                        }

                        int maxUses = -1;
                        if (uses != -1) {
                            // We will need to set the max uses later, validate and store the value now.
                            currentIndexLocation = usesString.indexOf("/", 0);
                            if (currentIndexLocation != 0) {

                                // There is a possible maximum uses located within the usesString
                                currentIndexLocation++;
                                endIndexLocation = usesString.length();
                                String maxUsesString = usesString.substring(currentIndexLocation, endIndexLocation);

                                if (validateUsesValue(maxUsesString, player)) {
                                    if (!maxUsesString.equals("unlimited"))
                                        maxUses = Integer.parseInt(maxUsesString);    // Grab the uses from the string
                                }
                            }
                        }

                        // We have at least 1 use available--start grabbing location data from the Runestone.

                        String locationString = loreData.get(0);
                        locationString = locationString.toLowerCase();

                        // Strip the coloring codes from the string
                        currentIndexLocation = 2;
                        endIndexLocation = locationString.length();
                        locationString = locationString.substring(currentIndexLocation, endIndexLocation);

                        // Get the world data
                        currentIndexLocation = 0;                                                               // Set the start point
                        endIndexLocation = locationString.indexOf(":", currentIndexLocation);                   // Get the end point for grabbing world location data
                        String worldString = locationString.substring(currentIndexLocation, endIndexLocation);

                        // Get the x coord data
                        currentIndexLocation = endIndexLocation + 2;                                            // Set the start point
                        endIndexLocation = locationString.indexOf(",", currentIndexLocation);                   // Get the end point for grabbing x location data
                        String xString = locationString.substring(currentIndexLocation, endIndexLocation);

                        // Get the y coord data
                        currentIndexLocation = endIndexLocation + 2;                                            // Set the start point
                        endIndexLocation = locationString.indexOf(",", currentIndexLocation);                   // Get the end point for grabbing y location data
                        String yString = locationString.substring(currentIndexLocation, endIndexLocation);

                        // Get the z coord data 
                        currentIndexLocation = endIndexLocation + 2;                                            // Set the start point
                        endIndexLocation = locationString.length();                                             // Get the end point for grabbing z location data
                        String zString = locationString.substring(currentIndexLocation, endIndexLocation);

                        // VERIFY ALL LOCATION DATA
                        if (validateCoordinate(xString)
                                && validateCoordinate(yString)
                                && validateCoordinate(zString)
                                && validateWorldByName(worldString)) {

                            // We have validate location information, convert our strings to real data
                            World world = Bukkit.getServer().getWorld(worldString);
                            int x = Integer.parseInt(xString);
                            int y = Integer.parseInt(yString);
                            int z = Integer.parseInt(zString);

                            // Grab the players current location and store their pitch / yaw values.
                            Location currentLocation = player.getLocation();
                            float pitch = currentLocation.getPitch();
                            float yaw = currentLocation.getYaw();

                            // Validate world checks
                            List<String> disabledWorlds = new ArrayList<String>(SkillConfigManager.getUseSettingKeys(hero, this, "disable-worlds"));
                            for (String disabledWorld : disabledWorlds) {
                                if (disabledWorld.equalsIgnoreCase(player.getWorld().getName())) {
                                    Messaging.send(player, "Magic has blocked your recall in this world");
                                    return SkillResult.FAIL;
                                }
                            }

                            Location teleportLocation = (new Location(world, x, y, z, yaw, pitch));

                            // Validate Herotowns
                            if (herotowns) {
                                if (!ht.getGlobalRegionManager().canBuild(player, teleportLocation)) {
                                    Messaging.send(player, "You cannot Recall to a Town you have no access to!");
                                    return SkillResult.FAIL;
                                }
                            }
                            
                            // Validate WorldGuard
                            if (worldguard) {
                                if (!wgp.canBuild(player, teleportLocation)) {
                                    Messaging.send(player, "You cannot Recall to a Region you have no access to!");
                                    return SkillResult.FAIL;
                                }
                            }

                            // Remove 1 use from Runestone, but only if the runestone isn't unlimited.
                            if (uses != -1) {
                                if (maxUses != -1) {
                                    loreData.set(1, ChatColor.AQUA.toString() + "Uses: " + (uses - 1) + "/" + maxUses);
                                    metaData.setLore(loreData);
                                    heldItem.setItemMeta(metaData);
                                }
                            }

                            broadcastExecuteText(hero);

                            player.getWorld().playSound(player.getLocation(), Sound.WITHER_SPAWN, 0.5F, 1.0F);

                            // Teleport the player to the location
                            player.teleport(teleportLocation);

                            // Removed for now until I have time to properly test it.
                            //                            final Location finalTeleportLocation = teleportLocation;
                            //                            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                            //                                @Override
                            //                                public void run() {
                            //                                    if (!player.getLocation().equals(finalTeleportLocation))
                            //                                        player.teleport(finalTeleportLocation);
                            //                                }
                            //                            }, 5L);

                            teleportLocation.getWorld().playSound(teleportLocation, Sound.WITHER_SPAWN, 0.5F, 1.0F);

                            return SkillResult.NORMAL;
                        }
                        else {
                            Messaging.send(player, "Runestone Contains Invalid Location Data.", player.getDisplayName());
                            return SkillResult.FAIL;
                        }
                    }
                    else {
                        Messaging.send(player, "Not a Valid Runestone Object. Uses Value is not Valid.", player.getDisplayName());
                        return SkillResult.FAIL;
                    }
                }
                else {
                    Messaging.send(player, "Not a Valid Runestone Object. LoreData Size <= 0", player.getDisplayName());
                    return SkillResult.FAIL;
                }
            }
        }

        // If we make it this far, this is not a proper Runestone block.
        // Continue to normal recall functionality.

        // DEFAULT RECALL FUNCTIONALITY

        // Validate world checks
        List<String> disabledWorlds = new ArrayList<String>(SkillConfigManager.getUseSettingKeys(hero, this, "disabled-worlds"));
        for (String disabledWorld : disabledWorlds) {
            if (disabledWorld.equalsIgnoreCase(player.getWorld().getName())) {
                Messaging.send(player, "Magic has blocked your recall in this world");
                return SkillResult.FAIL;
            }
        }

        ConfigurationSection skillSettings = hero.getSkillSettings(this);
        World world = SkillMark.validateLocation(skillSettings, player);
        if (world == null) {
            return SkillResult.FAIL;
        }
        if (hero.hasEffectType(EffectType.ROOT) || hero.hasEffectType(EffectType.STUN)) {
            Messaging.send(player, "Teleport fizzled.");
            return SkillResult.FAIL;
        }

        double[] xyzyp = null;
        try {
            xyzyp = SkillMark.getStoredData(skillSettings);
        }
        catch (IllegalArgumentException e) {
            Messaging.send(player, "Your recall location is improperly set!", new Object[0]);
            return SkillResult.SKIP_POST_USAGE;
        }

        Location recallLocation = new Location(world, xyzyp[0], xyzyp[1], xyzyp[2], (float) xyzyp[3], (float) xyzyp[4]);

        // Validate Herotowns
        if (herotowns) {
            if (!ht.getGlobalRegionManager().canBuild(player, recallLocation)) {
                Messaging.send(player, "Can not Recall to a town you have no access to!");
                return SkillResult.FAIL;
            }
        }

        broadcastExecuteText(hero);

        player.getWorld().playSound(player.getLocation(), Sound.WITHER_SPAWN, 0.5F, 1.0F);

        Location teleportLocation = new Location(world, xyzyp[0], xyzyp[1], xyzyp[2], (float) xyzyp[3], (float) xyzyp[4]);

        player.teleport(teleportLocation);

        teleportLocation.getWorld().playSound(teleportLocation, Sound.WITHER_SPAWN, 0.5F, 1.0F);

        return SkillResult.NORMAL;
    }

    public boolean validateUsesValue(String uses, Player player) {
        if (uses.equals("unlimited")) {
            return true;
        }

        try {
            Integer.parseInt(uses);
            return true;
        }
        catch (Exception ex) {
            broadcast(player.getLocation(), "Tried to parse an invalid integar. Not valid.", player.getDisplayName());  // DEBUG
            return false;
        }
    }

    public boolean validateWorldByName(String worldName) {
        World world = Bukkit.getServer().getWorld(worldName);

        if (world == null)
            return false;
        else
            return true;
    }

    public boolean validateCoordinate(String coord) {
        try {
            Integer.parseInt(coord);
            return true;
        }
        catch (Exception ex) {
            return false;
        }
    }
}