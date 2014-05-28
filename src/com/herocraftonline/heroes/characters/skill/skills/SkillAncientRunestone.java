package com.herocraftonline.heroes.characters.skill.skills;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.ResidencePermissions;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.SkillResult.ResultType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;
import com.herocraftonline.townships.HeroTowns;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

public class SkillAncientRunestone extends ActiveSkill {

    private ConcurrentHashMap<Player, List<ItemStack>> soulboundRunestones;

    private boolean herotowns = false;
    private HeroTowns ht;
    private boolean residence = false;
    private WorldGuardPlugin wgp;
    private boolean worldguard = false;

    public SkillAncientRunestone(Heroes plugin) {
        super(plugin, "AncientRunestone");
        setDescription("You imbue a redstone block with an Ancient Runestone. Ancient Runestones $1 and do not drop upon death.");
        setUsage("/skill ancientrunestone");
        setArgumentRange(0, 0);
        setIdentifiers("skill ancientrunestone");
        setTypes(SkillType.ITEM_MODIFYING, SkillType.SILENCEABLE);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillListener(this), plugin);

        soulboundRunestones = new ConcurrentHashMap<Player, List<ItemStack>>();

        try {
            //            if (Bukkit.getServer().getPluginManager().getPlugin("HeroTowns") != null) {
            //                herotowns = true;
            //                ht = (HeroTowns) this.plugin.getServer().getPluginManager().getPlugin("HeroTowns");
            //            }
            if (Bukkit.getServer().getPluginManager().getPlugin("Residence") != null) {
                residence = true;
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

    public String getDescription(Hero hero) {

        int maxUses = SkillConfigManager.getUseSetting(hero, this, "max-uses", -1, false);

        String maxUsesString = "";
        if (maxUses > -1)
            maxUsesString = "have a maximum use limit of " + maxUses;
        else
            maxUsesString = "have no usage limit";

        return getDescription().replace("$1", maxUsesString);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.NO_COMBAT_USE.node(), Boolean.valueOf(true));
        node.set(SkillSetting.DELAY.node(), 10000);
        node.set("max-uses", Integer.valueOf(-1));

        return node;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        ItemStack heldItem = player.getInventory().getItemInHand();

        // Check to make sure it is a redstone block
        ItemStack item = player.getItemInHand();
        if (item.getType().name() != "REDSTONE_BLOCK") {
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
            if (residence) {
                ClaimedResidence residence = Residence.getResidenceManager().getByLoc(location);
                if (residence != null) {
                    ResidencePermissions perm = residence.getPermissions();
                    if (!(perm.playerHas(player.getName(), "build", false))) {
                        Messaging.send(player, "You cannot imbue a Runestone within a residence you do not have access to!");
                        return SkillResult.FAIL;
                    }
                }
            }

            // Validate Herotowns
            if (herotowns) {
                if (!(ht.getGlobalRegionManager().canBuild(player, location))) {
                    Messaging.send(player, "You cannot imbue a Runestone within a town you do not have access to!");
                    return SkillResult.FAIL;
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

            // Set the Runestone name
            metaData.setDisplayName(ChatColor.GOLD + "Ancient Runestone");

            // Set the Lore with all Runestone information
            String locationInformation = ChatColor.AQUA + worldName + ": " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();

            int numUses = SkillConfigManager.getUseSetting(hero, this, "max-uses", -1, false);
            String runestoneUsesInformation = "";
            if (numUses > -1)           // -1 is unlimited
                runestoneUsesInformation = ChatColor.AQUA + "Uses: " + numUses + "/" + numUses;
            else
                runestoneUsesInformation = ChatColor.AQUA + "Uses: Unlimited";

            String imbuedByInformation = ChatColor.DARK_PURPLE + "Imbued by " + player.getName();
            List<String> newLore = Arrays.asList(locationInformation, runestoneUsesInformation, imbuedByInformation);
            metaData.setLore(newLore);

            // Seperate 1 block from their current stack (if they have one)
            int actualAmount = heldItem.getAmount();
            if (actualAmount > 1) {
                heldItem.setAmount(1);      // Remove the excess blocks.
            }

            // Set the new metaData to the item
            heldItem.setItemMeta(metaData);

            // Play Effects
            Util.playClientEffect(player, "enchantmenttable", new Vector(0, 0, 0), 1F, 10, true);
            player.getWorld().playSound(player.getLocation(), Sound.WITHER_IDLE, 0.5F, 1.0F);

            broadcastExecuteText(hero);

            if (actualAmount > 1) {
                // We need to return their excess blocks to them.
                PlayerInventory inventory = player.getInventory();

                HashMap<Integer, ItemStack> leftOvers = inventory.addItem(new ItemStack[] { new ItemStack(Material.REDSTONE_BLOCK, actualAmount - 1) });
                for (java.util.Map.Entry<Integer, ItemStack> entry : leftOvers.entrySet()) {
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

    public class SkillListener implements Listener {

        private Skill skill;

        public SkillListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onBlockPlace(BlockPlaceEvent event) {
            if (event.getItemInHand().getType() != Material.REDSTONE_BLOCK)
                return;

            ItemStack item = event.getItemInHand();
            ItemMeta metaData = item.getItemMeta();

            if (metaData == null || metaData.getDisplayName() == null)
                return;

            if (!(metaData.getDisplayName().contains("Ancient Runestone")))
                return;

            Messaging.send(event.getPlayer(), "You cannot place Runestone blocks!");
            event.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onPlayerQuit(PlayerQuitEvent event) {
            if (!(soulboundRunestones.containsKey(event.getPlayer())))
                return;

            Player player = event.getPlayer();
            Hero hero = plugin.getCharacterManager().getHero(player);

            List<ItemStack> runestoneDataPairs = soulboundRunestones.get(player);

            int i = 1;
            for (ItemStack item : runestoneDataPairs) {
                hero.setSkillSetting(skill, Integer.toString(i), item);
                i++;
            }

            // He's saved to the file now, don't need to keep him in the map anymore.
            soulboundRunestones.remove(player);

            // Save just in case - we're now removing this due to skillSettings being saved more efficiently.
            //plugin.getCharacterManager().saveHero(hero, true);
        }

        @EventHandler(priority = EventPriority.HIGH)
        public void onPlayerJoin(PlayerJoinEvent event) throws InvalidConfigurationException {

            Player player = event.getPlayer();
            Hero hero = plugin.getCharacterManager().getHero(player);
            ConfigurationSection skillSettings = hero.getSkillSettings("ancientrunestone");
            if (skillSettings == null)
                return;

            try {
                int i = 0;
                for (String key : skillSettings.getKeys(false)) {
                    ItemStack runeStone = (ItemStack) skillSettings.get(key);
                    if (runeStone != null) {
                        addRunestoneToSoulBoundList(player, runeStone);
                        skillSettings.set(key, null);
                        i++;
                    }
                }

                if (i > 0) {
                    // Save just in case
                    //plugin.getCharacterManager().saveHero(hero, true);
                }
            }
            catch (NumberFormatException e) {
                throw new InvalidConfigurationException("Expected a number.", e);
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onPlayerDeath(PlayerDeathEvent event) {

            if (event.getDrops().size() == 0)
                return;

            Player player = event.getEntity();

            for (ItemStack item : new HashSet<ItemStack>(event.getDrops())) {
                if (item == null)
                    continue;

                if (item.getType() != Material.REDSTONE_BLOCK)
                    continue;

                ItemMeta metaData = item.getItemMeta();

                if (metaData == null || metaData.getDisplayName() == null)
                    continue;

                if (!(metaData.getDisplayName().contains("Ancient Runestone")))
                    continue;

                // We have a runestone. Remove it from the drops, and get ready to place it back on the player
                event.getDrops().remove(item);

                addRunestoneToSoulBoundList(player, item);
            }
        }

        @SuppressWarnings("deprecation")
        @EventHandler(priority = EventPriority.MONITOR)
        private void onPlayerRespawn(PlayerRespawnEvent event) {
            if (!(soulboundRunestones.containsKey(event.getPlayer())))
                return;

            Player player = event.getPlayer();
            List<ItemStack> runestoneDataPairs = soulboundRunestones.get(player);

            // Loop through the items and give them back to the player.
            PlayerInventory inventory = player.getInventory();
            for (ItemStack item : runestoneDataPairs) {
                inventory.addItem(item);
            }
            player.updateInventory();

            soulboundRunestones.remove(player);
        }

        private void addRunestoneToSoulBoundList(Player player, ItemStack item) {
            // Add the runestone data to the list.
            if (soulboundRunestones.isEmpty()) {
                // Initialize our item data pairs
                List<ItemStack> runestoneDataPairs = new ArrayList<ItemStack>();

                // Add the paired data to the list
                runestoneDataPairs.add(item);

                // Pair the paired data to our player
                soulboundRunestones.put(player, runestoneDataPairs);
            }
            else {
                // Our main map is not empty. However, we might not have an entry for the current player.

                // Check if the player is on the map
                if (soulboundRunestones.containsKey(player)) {
                    // The player is on the map, and thus already has at least 1 runestone being tracked.
                    // Let's add this new runestone to the list.

                    List<ItemStack> runestoneDataPairs = soulboundRunestones.get(player);
                    runestoneDataPairs.add(item);
                }
                else {
                    // The player is not on the map.
                    // Let's add the runestone to the list.

                    List<ItemStack> runestoneDataPairs = new ArrayList<ItemStack>();
                    runestoneDataPairs.add(item);

                    // Pair the paired data to our player
                    soulboundRunestones.put(player, runestoneDataPairs);
                }
            }
        }
    }
}