package com.herocraftonline.heroes.characters.skill.skills;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.ResidencePermissions;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

import com.herocraftonline.townships.HeroTowns;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;


public class SkillMinorRunestone extends ActiveSkill {

    private boolean herotowns = false;
    private HeroTowns ht;
    private boolean residence = false;
    private WorldGuardPlugin wgp;
    private boolean worldguard = false;
    
    public SkillMinorRunestone(Heroes plugin) {
        super(plugin, "MinorRunestone");
        setDescription("You imbue a redstone block with an Minor Runestone. Minor Runestones $1");
        setUsage("/skill minorrunestone");
        setArgumentRange(0, 0);
        setIdentifiers("skill minorrunestone");
        setTypes(SkillType.TELEPORT, SkillType.ITEM, SkillType.SILENCABLE);
        try {
            Residence res = (Residence) this.plugin.getServer().getPluginManager().getPlugin("Residence");
            if (res != null)
                residence = true;
            HeroTowns ht = (HeroTowns) this.plugin.getServer().getPluginManager().getPlugin("HeroTowns");
            if (ht != null)
                herotowns = true;
            wgp = (WorldGuardPlugin) this.plugin.getServer().getPluginManager().getPlugin("WorldGuard");
            if( wgp != null)
                worldguard = true;
        } catch (Exception e) {
            Heroes.log(Level.SEVERE, "Could not get Residence or HeroTowns! Region checking may not work!");
        }
    }

    public String getDescription(Hero hero) {

        int maxUses = SkillConfigManager.getUseSetting(hero, this, "max-uses", 3, false);

        String maxUsesString = "";
        if (maxUses > -1)
            maxUsesString = "have a maximum use limit of " + maxUses;
        else
            maxUsesString = "have no usage limit.";

        return getDescription().replace("$1", maxUsesString);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("max-uses", Integer.valueOf(3));

        return node;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        ItemStack heldItem = player.getInventory().getItemInHand();

        // Check to make sure it is a redstone block
        ItemStack item = player.getItemInHand();
        if (item.getType().name() != "REDSTONE_BLOCK") {
            return SkillResult.INVALID_TARGET;
        }

        // Get their current location
        Location location = player.getLocation();

        // Validate Residences
        if (residence) {
            ClaimedResidence residence = Residence.getResidenceManager().getByLoc(location);
            if (residence != null) {
                ResidencePermissions perm = residence.getPermissions();
                if (perm.playerHas(player.getName(), "build", false));
                else {
                    broadcast(player.getLocation()," Can not set a Runestone in a Residence you have no access to!");
                    return SkillResult.FAIL;
                }
            }
        }

        // Validate Herotowns
        if (herotowns) {
            if (ht.getGlobalRegionManager().canBuild(player, location));
            else {
                broadcast(player.getLocation(),"Can not set a Runestone in a Town you have no access to!");
                return SkillResult.FAIL;
            }
        }
        
        // Validate WorldGuard
        if(worldguard) {
            if(wgp.canBuild(player, player.getLocation()));
            else {
                broadcast(player.getLocation(), "Can not set a Runestone in a Region you have no access to!");
                return SkillResult.FAIL;
            }
        }

        // Set the first letter of the world name to be upper-case rather than
        // lower case.
        String worldName = location.getWorld().getName();
        worldName = worldName.substring(0, 1).toUpperCase() + worldName.substring(1);

        // Create the runestone metadata
        ItemMeta metaData = heldItem.getItemMeta();

        // Set the Runestone name
        metaData.setDisplayName("§2Minor Runestone");

        // Set the Lore with all Runestone information
        String locationInformation = "§b" + worldName + ": " + location.getBlockX() + ", " + location.getBlockY()
                + ", " + location.getBlockZ();

        int numUses = SkillConfigManager.getUseSetting(hero, this, "max-uses", 3, false);
        String runestoneUsesInformation = "";
        if (numUses > -1) // -1 is unlimited
            runestoneUsesInformation = "Uses: " + numUses;
        else
            runestoneUsesInformation = "Uses: Unlimited";

        String imbuedByInformation = "§5Imbued by " + player.getDisplayName();
        List<String> lore = Arrays.asList(locationInformation, runestoneUsesInformation, imbuedByInformation);
        metaData.setLore(lore);

        // Set the meta to the item
        heldItem.setItemMeta(metaData);

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }
}