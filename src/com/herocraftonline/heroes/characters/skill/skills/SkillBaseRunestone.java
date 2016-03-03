package com.herocraftonline.heroes.characters.skill.skills;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import com.palmergames.bukkit.util.BukkitTools;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;


//import com.bekvon.bukkit.residence.Residence;
//import com.bekvon.bukkit.residence.protection.ClaimedResidence;
//import com.bekvon.bukkit.residence.protection.ResidencePermissions;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.SkillResult.ResultType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;
import com.herocraftonline.townships.HeroTowns;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

public abstract class SkillBaseRunestone extends ActiveSkill {

    private boolean herotowns = false;
    //private HeroTowns ht;
    //private boolean residence = false;
    private boolean towny = false;
    private WorldGuardPlugin wgp;
    private boolean worldguard = false;

    protected int defaultMaxUses;
    protected long defaultDelay;
    protected String displayName;
    protected ChatColor displayNameColor;

    protected SkillBaseRunestone(Heroes plugin, String name) {
        super(plugin, name);

        try {
            /*if (Bukkit.getServer().getPluginManager().getPlugin("HeroTowns") != null) {
                herotowns = true;
                ht = (HeroTowns) this.plugin.getServer().getPluginManager().getPlugin("HeroTowns");
            }*/
            /*if (Bukkit.getServer().getPluginManager().getPlugin("Residence") != null) {
                residence = true;
            }*/
            if (Bukkit.getServer().getPluginManager().getPlugin("Towny") != null) {
                towny = true;
            }
            if (Bukkit.getServer().getPluginManager().getPlugin("WorldGuard") != null) {
                worldguard = true;
                wgp = (WorldGuardPlugin) this.plugin.getServer().getPluginManager().getPlugin("WorldGuard");
            }
        }
        catch (Exception e) {
            Heroes.log(Level.SEVERE, "Could not get Residence or HeroTowns! Region checking may not work!");
        }
    }

    public SkillBaseRunestone(Heroes plugin) {
        this(plugin, "Runestone");
        setDescription("You imbue a redstone block with a Runestone. Runestones $1");
        setUsage("/skill runestone");
        setArgumentRange(0, 0);
        setIdentifiers("skill runestone");
        setTypes(SkillType.ITEM_MODIFYING, SkillType.SILENCEABLE);

        defaultMaxUses = 2;
        defaultDelay = 5000;
        displayName = "Runestone";
        displayNameColor = ChatColor.GREEN;

        new RunestoneListener();
    }

    @Override
    public String getDescription(Hero hero) {

        int maxUses = SkillConfigManager.getUseSetting(hero, this, "max-uses", defaultMaxUses, false);

        String maxUsesString = "";
        if (maxUses > -1)
            maxUsesString = "have a maximum use limit of " + maxUses + ".";
        else
            maxUsesString = "have no usage limit.";

        return getDescription().replace("$1", maxUsesString);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.NO_COMBAT_USE.node(), true);
        node.set(SkillSetting.DELAY.node(), defaultDelay);
        node.set("max-uses", defaultMaxUses);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        ItemStack heldItem = player.getInventory().getItemInHand();

        // Check to make sure it is a redstone block
        ItemStack item = player.getItemInHand();
        if (!item.getType().name().equals("REDSTONE_BLOCK")) {
            Messaging.send(player, "You must be holding a Redstone Block in order to imbue Runestones.");
            return new SkillResult(ResultType.MISSING_REAGENT, false);
        }

        // Read the meta data (if it has any)
        ItemMeta metaData = heldItem.getItemMeta();
        List<String> loreData = metaData.getLore();

        // Ensure that there is no meta-data already on the object
        if (loreData == null) {

            // Get their current location
            Location location = player.getLocation();

            // Validate Residences
            /*if (residence) {
                ClaimedResidence residence = Residence.getResidenceManager().getByLoc(location);
                if (residence != null) {
                    ResidencePermissions perm = residence.getPermissions();
                    if (!(perm.playerHas(player.getName(), "build", false))) {
                        Messaging.send(player, "You cannot imbue a Runestone within a residence you do not have access to!");
                        return SkillResult.FAIL;
                    }
                }
            }*/

            // Validate Herotowns
            if (herotowns) {
                /*if (!(ht.getGlobalRegionManager().canBuild(player, location))) {
                    Messaging.send(player, "You cannot imbue a Runestone within a town you do not have access to!");
                    return SkillResult.FAIL;
                }*/
            }

            // Validate Towny
            if(towny) {
                // Check if the block in question is a Town Block, don't want Towny perms to interfere if we're not in a town... just in case.
                TownBlock tBlock = TownyUniverse.getTownBlock(location);
                if(tBlock != null) {
                    // Make sure the Town Block actually belongs to a town. If there's no town, we don't care.
                    try {
                        tBlock.getTown();

                        // Need a Block to run towny build checks on. Naturally, the block they're standing on.
                        Block block = location.getBlock();
                        // Since we know the block is within a town, check if the player can build there. This *should* be actual perms, not circumstances like War.
                        boolean buildPerms = PlayerCacheUtil.getCachePermission(player, location, BukkitTools.getTypeId(block), BukkitTools.getData(block), TownyPermission.ActionType.BUILD);

                        // If the player can't build, no runestone
                        if (!buildPerms) {
                            Messaging.send(player, "You cannot imbue a Runestone in a Town you have no access to!");
                            return SkillResult.FAIL;
                        }
                    }
                    catch (NotRegisteredException e) {
                        // Ignore: No town here
                    }
                }
            }

            // Validate WorldGuard
            if (worldguard) {
                if (!wgp.canBuild(player, location)) {
                    Messaging.send(player, "You cannot imbue a Runestone in a Region you have no access to!");
                    return SkillResult.FAIL;
                }
            }

            // Set the first letter of the world name to be upper-case rather than lower case.
            String worldName = location.getWorld().getName();
            worldName = worldName.substring(0, 1).toUpperCase() + worldName.substring(1);
            if (StringUtils.isNotEmpty(plugin.getServerName())) {
                worldName = plugin.getServerName() + "," + worldName;
            }

            // Set the Runestone name
            metaData.setDisplayName(displayNameColor + displayName);

            // Set the Lore with all Runestone information
            String locationInformation = ChatColor.AQUA + worldName + ": " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();

            int numUses = SkillConfigManager.getUseSetting(hero, this, "max-uses", defaultMaxUses, false);
            String runestoneUsesInformation = "";
            if (numUses > -1) 			// -1 is unlimited
                runestoneUsesInformation = ChatColor.AQUA + "Uses: " + numUses + "/" + numUses;
            else
                runestoneUsesInformation = ChatColor.AQUA + "Uses: Unlimited";

            String imbuedByInformation = ChatColor.DARK_PURPLE + "Imbued by " + player.getName();
            List<String> newLore = Arrays.asList(locationInformation, runestoneUsesInformation, imbuedByInformation);
            metaData.setLore(newLore);

            // Seperate 1 block from their current stack (if they have one)
            int actualAmount = heldItem.getAmount();
            if (actualAmount > 1) {
                heldItem.setAmount(1);		// Remove the excess blocks.
            }

            // Set the new metaData to the item
            heldItem.setItemMeta(metaData);

            // Play Effects
            Util.playClientEffect(player, "enchantmenttable", new Vector(0, 0, 0), 1F, 10, true);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.5F, 1.0F);

            broadcastExecuteText(hero);

            if (actualAmount > 1) {
                // We need to return their excess blocks to them.
                PlayerInventory inventory = player.getInventory();

                HashMap<Integer, ItemStack> leftOvers = inventory.addItem(new ItemStack(Material.REDSTONE_BLOCK, actualAmount - 1));
                for (Entry<Integer, ItemStack> entry : leftOvers.entrySet()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), entry.getValue());
                    Messaging.send(player, "Items have been dropped at your feet!");
                }
            }

            return SkillResult.NORMAL;
        }
        else {
            Messaging.send(player, "You cannot imbue a Rune to that item!");
            return SkillResult.FAIL;
        }
    }

    protected class RunestoneListener implements Listener {

        public RunestoneListener() {
            Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onBlockPlace(BlockPlaceEvent event) {
            if (event.getItemInHand().getType() != Material.REDSTONE_BLOCK)
                return;

            ItemStack item = event.getItemInHand();
            ItemMeta metaData = item.getItemMeta();

            if (metaData == null || metaData.getDisplayName() == null)
                return;

            if (!(metaData.getDisplayName().contains(displayName)))
                return;

            Messaging.send(event.getPlayer(), "You cannot place Runestone blocks!");
            event.setCancelled(true);
        }
    }
}