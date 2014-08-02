package com.herocraftonline.heroes.characters.skill.skills;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
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

public class SkillRecall extends ActiveSkill implements Listener, PluginMessageListener {

    private boolean herotowns = false;
    private HeroTowns ht;
    private WorldGuardPlugin wgp;
    private boolean worldguard = false;
    private Set<String> pendingTeleport = new HashSet<>();
    private Map<String, Info<ConfigurationSection>> onJoinSkillSettings = new Hashtable<>();

    protected SkillRecall(Heroes plugin, String name) {
        super(plugin, name);
        setDescription("You recall to your marked location. If you are holding a Runestone, you recall to its stored location instead.");
        setUsage("/skill recall");
        setArgumentRange(0, 0);
        setIdentifiers("skill recall");
        setTypes(SkillType.SILENCEABLE, SkillType.TELEPORTING);

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

        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, Heroes.BUNGEE_CORD_CHANNEL, this);
    }

    public SkillRecall(Heroes plugin) {
        this(plugin, "Recall");
    }

    public String getDescription(Hero hero) {
        return getDescription();
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.NO_COMBAT_USE.node(), true);
        node.set(SkillSetting.DELAY.node(), 10000);
        node.set(SkillSetting.REAGENT.node(), 331);
        node.set(SkillSetting.REAGENT_COST.node(), 10);

        return node;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

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

                    if (isValidUses(currentUsesString, player)) {
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

                                if (isValidUses(maxUsesString, player)) {
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
                        if (isValidInteger(xString)
                                && isValidInteger(yString)
                                && isValidInteger(zString)
                                && isValidWorld(worldString)) {

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
                            if (isDisabledWorld(player.getWorld().getName(), SkillConfigManager.getUseSettingKeys(hero, this, "disable-worlds"))) {
                                Messaging.send(player, "Magic has blocked your recall in this world");
                                return SkillResult.FAIL;
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
                            Messaging.send(player, "Runestone Contains Invalid Location Data.", player.getName());
                            return SkillResult.FAIL;
                        }
                    }
                    else {
                        Messaging.send(player, "Not a Valid Runestone Object. Uses Value is not Valid.", player.getName());
                        return SkillResult.FAIL;
                    }
                }
                else {
                    Messaging.send(player, "Not a Valid Runestone Object. LoreData Size <= 0", player.getName());
                    return SkillResult.FAIL;
                }
            }
        }

        // If we make it this far, this is not a proper Runestone block.
        // Continue to normal recall functionality.

        // DEFAULT RECALL FUNCTIONALITY

        if (hero.hasEffectType(EffectType.ROOT) || hero.hasEffectType(EffectType.STUN)) {
            Messaging.send(player, "Teleport fizzled.");
            return SkillResult.FAIL;
        }

        ConfigurationSection skillSettings = hero.getSkillSettings(this);

        // Forward recall request to remote server
        if (isRemoteServerLocation(skillSettings)) {
            ByteArrayDataOutput recallRequest = ByteStreams.newDataOutput();
            recallRequest.writeUTF("Forward");
            recallRequest.writeUTF(skillSettings.getString("server"));
            recallRequest.writeUTF("RecallRequest");

            ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
            DataOutputStream msgout = new DataOutputStream(msgbytes);
            try {
                msgout.writeUTF(player.getName());
                msgout.writeUTF(skillSettings.getString("world"));
                msgout.writeUTF(skillSettings.getString("x"));
                msgout.writeUTF(skillSettings.getString("y"));
                msgout.writeUTF(skillSettings.getString("z"));
                msgout.writeUTF(skillSettings.getString("yaw"));
                msgout.writeUTF(skillSettings.getString("pitch"));
            }
            catch (IOException e) {
                Messaging.send(player, "Your recall location is improperly set!");
                return SkillResult.SKIP_POST_USAGE;
            }

            recallRequest.writeShort(msgbytes.toByteArray().length);
            recallRequest.write(msgbytes.toByteArray());

            pendingTeleport.add(player.getName());
            player.sendPluginMessage(plugin, Heroes.BUNGEE_CORD_CHANNEL, recallRequest.toByteArray());

            // Run this delayed task to check if the recall failed
            final String playerName = player.getName();
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {

                @Override
                public void run()
                {
                    if (pendingTeleport.remove(playerName)) {
                        Player player = Bukkit.getPlayer(playerName);
                        if (player != null) {
                            Messaging.send(player, "Teleport fizzled.");
                        }
                    }
                }
            }, 40);

            return SkillResult.NORMAL;
        }

        return doTeleport(hero, skillSettings, true);
    }

    private SkillResult doTeleport(Hero hero, ConfigurationSection skillSettings, boolean isDeparting)
    {
        Player player = hero.getPlayer();

        // Validate world checks
        if (isDisabledWorld(player.getWorld().getName(), SkillConfigManager.getUseSettingKeys(hero, this, "disabled-worlds"))) {
            Messaging.send(player, "Magic has blocked your recall in this world");
            return SkillResult.FAIL;
        }

        World world = SkillMark.getValidWorld(skillSettings, player.getName());
        if (world == null) {
            return SkillResult.FAIL;
        }

        double[] xyzyp;
        try {
            xyzyp = SkillMark.createLocationData(skillSettings);
        }
        catch (IllegalArgumentException e) {
            Messaging.send(player, "Your recall location is improperly set!");
            return SkillResult.SKIP_POST_USAGE;
        }

        Location teleportLocation = new Location(world, xyzyp[0], xyzyp[1], xyzyp[2], (float) xyzyp[3], (float) xyzyp[4]);

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

        if (isDeparting) {
            broadcastExecuteText(hero);
    
            player.getWorld().playSound(player.getLocation(), Sound.WITHER_SPAWN, 0.5F, 1.0F);
        }

        player.teleport(teleportLocation);

        teleportLocation.getWorld().playSound(teleportLocation, Sound.WITHER_SPAWN, 0.5F, 1.0F);

        return SkillResult.NORMAL;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!Heroes.BUNGEE_CORD_CHANNEL.equals(channel)) {
            return;
        }
    
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();
        
        short len = in.readShort();
        byte[] msgbytes = new byte[len];
        in.readFully(msgbytes);
        DataInputStream msgin = new DataInputStream(new ByteArrayInputStream(msgbytes));
    
        switch (subChannel) {
        case "RecallRequest":
            try {
                String playerName = msgin.readUTF();
                ConfigurationSection skillSettings = new MemoryConfiguration();
                skillSettings.set("world", msgin.readUTF());
                skillSettings.set("x", msgin.readUTF());
                skillSettings.set("y", msgin.readUTF());
                skillSettings.set("z", msgin.readUTF());
                skillSettings.set("yaw", msgin.readUTF());
                skillSettings.set("pitch", msgin.readUTF());

                // cache the location for onPlayerJoin
                onJoinSkillSettings.put(playerName, new Info<ConfigurationSection>(skillSettings));

                // send the player to this server
                ByteArrayDataOutput connectOther = ByteStreams.newDataOutput();
                connectOther.writeUTF("ConnectOther");
                connectOther.writeUTF(playerName);
                connectOther.writeUTF(plugin.getServerName());

                player.sendPluginMessage(plugin, Heroes.BUNGEE_CORD_CHANNEL, connectOther.toByteArray());
            }
            catch (IOException e) {
                Heroes.log(Level.SEVERE, "SkillRecall: Could not parse RecallRequest message from remote server");
            }
            break;
        case "RunestoneRequest":
            break;
        default:
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Hero hero = plugin.getCharacterManager().getHero(player);
        Info<ConfigurationSection> skillSettings = onJoinSkillSettings.remove(player.getName());
        if (skillSettings != null && skillSettings.isNotExpired()) {
            SkillResult result = doTeleport(hero, skillSettings.getInfo(), false);
            if (!SkillResult.NORMAL.equals(result)) {
                player.teleport(player.getWorld().getSpawnLocation());
                Messaging.send(player, "Teleport fizzled.");
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (pendingTeleport.remove(player.getName())) {
            broadcastExecuteText(plugin.getCharacterManager().getHero(player));
            player.getWorld().playSound(player.getLocation(), Sound.WITHER_SPAWN, 0.5F, 1.0F);
        }
    }

    private boolean isDisabledWorld(String world, Set<String> disabledWorlds) {
        boolean isDisabled = false;

        for (String disabledWorld : disabledWorlds) {
            if (disabledWorld.equalsIgnoreCase(world)) {
                isDisabled = true;
                break;
            }
        }

        return isDisabled;
    }

    private boolean isRemoteServerLocation(ConfigurationSection skillSettings) {
        
        return skillSettings != null && StringUtils.isNotEmpty(skillSettings.getString("server"))
                && !skillSettings.getString("server").equals(plugin.getServerName())
                && plugin.getServerNames().contains(skillSettings.getString("server"));
    }

	private boolean isValidUses(String uses, Player player) {
        if (uses.equals("unlimited")) {
            return true;
        }

        try {
            Integer.parseInt(uses);
            return true;
        }
        catch (Exception ex) {
            broadcast(player.getLocation(), "Tried to parse an invalid integar. Not valid.", player.getName());  // DEBUG
            return false;
        }
    }

    private boolean isValidWorld(String worldName) {
        World world = Bukkit.getServer().getWorld(worldName);

        return world != null;
    }

    private boolean isValidInteger(String i) {
        try {
            Integer.parseInt(i);
            return true;
        }
        catch (Exception ex) {
            return false;
        }
    }

    class Info<T>
    {
        private final static long TIMEOUT = 10 * 1000; // 10s
    
        private final long timeStamp;
        private final T info;
    
        public Info(final T info)
        {
            timeStamp = System.currentTimeMillis();
            this.info = info;
        }
    
        public T getInfo()
        {
            return info;
        }

        public boolean isExpired()
        {
            return System.currentTimeMillis() - timeStamp > TIMEOUT;
        }

        public boolean isNotExpired()
        {
            return !isExpired();
        }
    }
}