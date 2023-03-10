package com.herocraftonline.heroes.characters.skill.reborn;

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
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.townships.users.TownshipsUser;
import com.herocraftonline.townships.users.UserManager;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class SkillRecall extends ActiveSkill implements Listener {

    private final boolean towny = false;
    private WorldGuardPlugin wgp;
    private boolean worldguard = false;
    private boolean townships = false;

    protected String subChannel;

    protected SkillRecall(Heroes plugin, String name) {
        super(plugin, name);

        try {
            /*if (Bukkit.getServer().getPluginManager().getPlugin("Towny") != null) {
                towny = true;
            }*/

            if (Bukkit.getServer().getPluginManager().getPlugin("WorldGuard") != null) {
                worldguard = true;
                wgp = (WorldGuardPlugin) this.plugin.getServer().getPluginManager().getPlugin("WorldGuard");
            }
            if (Bukkit.getServer().getPluginManager().getPlugin("Townships") != null) {
                townships = true;
            }
        } catch (Exception e) {
            Heroes.log(Level.SEVERE, "SkillRecall: Could not get WorldGuard or Townships plugins! Region checking may not work!");
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public SkillRecall(Heroes plugin) {
        this(plugin, "Recall");

        setDescription("You recall to your marked location. If you are holding a Runestone, you recall to its stored location instead.");
        setUsage("/skill recall");
        setArgumentRange(0, 0);
        setIdentifiers("skill recall");
        setTypes(SkillType.SILENCEABLE, SkillType.TELEPORTING);

        subChannel = "RecallRequest";
    }

    public String getDescription(Hero hero) {
        return getDescription();
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("ignore-region-plugins", false);
        config.set(SkillSetting.NO_COMBAT_USE.node(), true);
        config.set(SkillSetting.DELAY.node(), 10000);
        config.set(SkillSetting.REAGENT.node(), "REDSTONE");
        config.set(SkillSetting.REAGENT_COST.node(), 10);
        return config;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        // RUNESTONE RECALL FUNCTIONALITY
        ItemStack heldItem = NMSHandler.getInterface().getItemInMainHand(player.getInventory());
        if (heldItem.getType() == Material.REDSTONE_BLOCK) {

            // We have a possible Runestone object.

            // Read the meta data (if it has any)
            ItemMeta metaData = heldItem.getItemMeta();
            List<String> loreData = metaData.getLore();

            // Ensure that we actually have meta data and at least three rows of available on the block
            if (loreData == null || loreData.isEmpty()) {
                player.sendMessage("Not a Valid Runestone Object. Uses Value is not Valid.");
                return SkillResult.FAIL;
            }

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

            if (!isValidUses(currentUsesString, player)) {
                player.sendMessage("Runestone Contains Invalid Location Data.");
                return SkillResult.FAIL;
            }

            // We have a valid value for "uses". It is either a number, or "unlimited"

            int uses = -1;
            if (!currentUsesString.equals("unlimited"))
                uses = Integer.parseInt(currentUsesString);    // Grab the uses from the string

            // If it's empty, tell them to recharge it.
            if (uses == 0) {
                player.sendMessage("Runestone is out of uses and needs to be recharged.");
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
            String serverString = plugin.getServerName();
            String[] worldParts = worldString.split(",");
            if (worldParts.length > 1) {
                worldString = worldParts[1];
                serverString = worldParts[0];
            }

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

            // Grab the players current location and store their pitch / yaw values.
            Location currentLocation = player.getLocation();
            float pitch = currentLocation.getPitch();
            float yaw = currentLocation.getYaw();

            ConfigurationSection skillSettings = new MemoryConfiguration();
            skillSettings.set("server", serverString);
            skillSettings.set("world", worldString);
            skillSettings.set("x", xString);
            skillSettings.set("y", yString);
            skillSettings.set("z", zString);
            skillSettings.set("yaw", yaw);
            skillSettings.set("pitch", pitch);
            skillSettings.set("pending-teleport", "runestone");

            // Remove 1 use from Runestone, but only if the runestone isn't unlimited.
            if (uses != -1) {
                if (maxUses != -1) {
                    loreData.set(1, ChatColor.AQUA + "Uses: " + (uses - 1) + "/" + maxUses);
                    metaData.setLore(loreData);
                    heldItem.setItemMeta(metaData);
                }
            }

            return isRemoteServerLocation(skillSettings) ? forwardTeleport(hero, skillSettings) : doTeleport(hero, skillSettings, true);
        }

        // If we make it this far, this is not a proper Runestone block.
        // Continue to normal recall functionality.

        // DEFAULT RECALL FUNCTIONALITY

        if (hero.hasEffectType(EffectType.ROOT) || hero.hasEffectType(EffectType.STUN)) {
            player.sendMessage("Teleport fizzled.");
            return SkillResult.FAIL;
        }

        ConfigurationSection skillSettings = hero.getSkillSettings(this);

        // If necessary, forward recall request to remote server
        return isRemoteServerLocation(skillSettings) ?
                forwardTeleport(hero, skillSettings) : doTeleport(hero, skillSettings, true);
    }

    private SkillResult forwardTeleport(final Hero hero, ConfigurationSection skillSettings) {
        ByteArrayDataOutput recallRequest = ByteStreams.newDataOutput();
        recallRequest.writeUTF("Connect");
        recallRequest.writeUTF(skillSettings.getString("server"));
        if ("runestone".equals(skillSettings.getString("pending-teleport"))) {
            hero.setSkillSetting(this, "pending-teleport", skillSettings.getString("pending-teleport"));
            hero.setSkillSetting(this, "rs-server", skillSettings.getString("server"));
            hero.setSkillSetting(this, "rs-world", skillSettings.getString("world"));
            hero.setSkillSetting(this, "rs-x", skillSettings.getString("x"));
            hero.setSkillSetting(this, "rs-y", skillSettings.getString("y"));
            hero.setSkillSetting(this, "rs-z", skillSettings.getString("z"));
            hero.setSkillSetting(this, "rs-yaw", skillSettings.getString("yaw"));
            hero.setSkillSetting(this, "rs-pitch", skillSettings.getString("pitch"));
        } else {
            hero.setSkillSetting(this, "pending-teleport", "recall");
        }

        Player player = hero.getPlayer();
        player.sendPluginMessage(plugin, Heroes.BUNGEE_CORD_CHANNEL, recallRequest.toByteArray());

        // Run this delayed task to check if the recall failed
        final String playerName = player.getName();
        final SkillRecall thisSkill = this;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player player1 = Bukkit.getPlayerExact(playerName);
            if (player1 != null) {
                Hero hero1 = plugin.getCharacterManager().getHero(player1);
                ConfigurationSection skillSettings1 = plugin.getCharacterManager().getHero(player1).getSkillSettings(thisSkill);
                if (skillSettings1 != null && ("runestone".equals(skillSettings1.getString("pending-teleport")) ||
                        "recall".equals(skillSettings1.getString("pending-teleport")))) {
                    hero1.setSkillSetting(thisSkill, "pending-teleport", "none");
                    player1.sendMessage("Teleport fizzled.");
                }
            }
        }, 40);

        return SkillResult.NORMAL;
    }

    private SkillResult doTeleport(Hero hero, ConfigurationSection skillSettings, boolean isDeparting) {
        final Player player = hero.getPlayer();

        // Validate world checks
        if (isDisabledWorld(player.getWorld().getName(), SkillConfigManager.getUseSettingKeys(hero, this, "disabled-worlds"))) {
            player.sendMessage("Magic has blocked your recall in this world");
            return SkillResult.FAIL;
        }

        World world = SkillMark.getValidWorld(skillSettings, player.getName());
        if (world == null) {
            return SkillResult.FAIL;
        }

        double[] xyzyp;
        try {
            xyzyp = SkillMark.createLocationData(skillSettings);
        } catch (IllegalArgumentException e) {
            player.sendMessage("Your recall location is improperly set!");
            return SkillResult.SKIP_POST_USAGE;
        }

        boolean ignoreRegionPlugins = skillSettings.getBoolean("ignore-region-plugins");
        Location teleportLocation = new Location(world, xyzyp[0], xyzyp[1], xyzyp[2], (float) xyzyp[3], (float) xyzyp[4]);

        // Validate Towny
        if (towny && !ignoreRegionPlugins) {
           /* // Check if the block in question is a Town Block, don't want Towny perms to interfere if we're not in a town... just in case.
            TownBlock tBlock = TownyUniverse.getTownBlock(teleportLocation);
            if(tBlock != null) {
                // Make sure the Town Block actually belongs to a town. If there's no town, we don't care.
                try {
                    tBlock.getTown();

                    // There is a town, but we need a block to check build perms. The teleport location will do.
                    Block block = teleportLocation.getBlock();
                    // Since we know the block is within a town, check if the player can build there. This *should* be actual perms, not circumstances like War.
                    boolean buildPerms = PlayerCacheUtil.getCachePermission(player, teleportLocation, BukkitTools.getTypeId(block), BukkitTools.getData(block), TownyPermission.ActionType.BUILD);

                    // If the player can't build, no recall
                    if (!buildPerms) {
                        player.sendMessage("You cannot Recall to a Town you have no access to!");
                        return SkillResult.FAIL;
                    }
                }
                catch (NotRegisteredException e) {
                    // Ignore: No town here
                }
            }*/
        }

        // Validate Townships
        if (townships && !ignoreRegionPlugins) {
            TownshipsUser user = UserManager.fromOfflinePlayer(player);
            if (!user.canBuild(teleportLocation)) {
                player.sendMessage("You cannot Recall to a Region you have no access to!");
                return SkillResult.FAIL;
            }
        }

        // Validate WorldGuard
        if (worldguard && !ignoreRegionPlugins) {
            LocalPlayer wgPlayer = wgp.wrapPlayer(player);
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            com.sk89q.worldedit.util.Location wgTeleportLoc = BukkitAdapter.adapt(teleportLocation);
            RegionQuery query = container.createQuery();
            if (!query.testState(wgTeleportLoc, wgPlayer, Flags.BUILD)) {
                player.sendMessage("You cannot Recall to a Region you have no access to!");
                return SkillResult.FAIL;
            }
        }

        if (isDeparting) {
            broadcastExecuteText(hero);

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5F, 1.0F);
            //hero.getPlayer().getWorld().spigot().playEffect(player.getLocation(), Effect.COLOURED_DUST, 0, 0, 0.2F, 1.0F, 0.2F, 0.0F, 50, 12);
            hero.getPlayer().getWorld().spawnParticle(Particle.REDSTONE, player.getLocation(), 50, 0.2, 1, 0.2, 0, new Particle.DustOptions(Color.ORANGE, 1));
        }

        // Removed for now until I have time to properly test it.
        // final Location finalTeleportLocation = teleportLocation;
        // Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
        //     @Override
        //     public void run() {
        //         if (!player.getLocation().equals(finalTeleportLocation))
        //             player.teleport(finalTeleportLocation);
        //     }
        // }, 5L);

        player.teleport(teleportLocation);

        teleportLocation.getWorld().playSound(teleportLocation, Sound.ENTITY_WITHER_SPAWN, 0.5F, 1.0F);
        //teleportLocation.getWorld().spigot().playEffect(teleportLocation, Effect.COLOURED_DUST, 0, 0, 0.2F, 1.0F, 0.2F, 0.0F, 50, 12);
        hero.getPlayer().getWorld().spawnParticle(Particle.REDSTONE, player.getLocation(), 50, 0.2, 1, 0.2, 0, new Particle.DustOptions(Color.ORANGE, 1));

        return SkillResult.NORMAL;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Hero hero = plugin.getCharacterManager().getHero(player);
        ConfigurationSection skillSettings = hero.getSkillSettings(this);
        if (skillSettings != null) {
            ConfigurationSection teleportSettings = null;
            if ("recall".equals(skillSettings.getString("pending-teleport"))) {
                hero.setSkillSetting(this, "pending-teleport", "none");
                teleportSettings = skillSettings;
            } else if ("runestone".equals(skillSettings.getString("pending-teleport"))) {
                hero.setSkillSetting(this, "pending-teleport", "none");
                teleportSettings = new MemoryConfiguration();
                teleportSettings.set("server", skillSettings.getString("rs-server"));
                teleportSettings.set("world", skillSettings.getString("rs-world"));
                teleportSettings.set("x", skillSettings.getString("rs-x"));
                teleportSettings.set("y", skillSettings.getString("rs-y"));
                teleportSettings.set("z", skillSettings.getString("rs-z"));
                teleportSettings.set("yaw", skillSettings.getString("rs-yaw"));
                teleportSettings.set("pitch", skillSettings.getString("rs-pitch"));
            }
            if (teleportSettings != null) {
                SkillResult result = doTeleport(hero, teleportSettings, false);
                if (!SkillResult.NORMAL.equals(result)) {
                    player.teleport(player.getWorld().getSpawnLocation());
                    player.sendMessage("Teleport fizzled.");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Hero hero = plugin.getCharacterManager().getHero(player);
        ConfigurationSection skillSettings = hero.getSkillSettings(this);
        if (skillSettings != null && ("runestone".equals(skillSettings.getString("pending-teleport")) ||
                "recall".equals(skillSettings.getString("pending-teleport")))) {
            broadcastExecuteText(hero);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5F, 1.0F);
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
        if(skillSettings == null) {
            return false;
        }
        String server = skillSettings.getString("server");
        return server != null && !server.isEmpty() && !server.equals(plugin.getServerName()) && plugin.getServerNames().contains(server);
    }

    private boolean isValidUses(String uses, Player player) {
        if (uses.equals("unlimited")) {
            return true;
        }

        try {
            Integer.parseInt(uses);
            return true;
        } catch (Exception ex) {
            broadcast(player.getLocation(), "Tried to parse an invalid integar. Not valid.");  // DEBUG
            return false;
        }
    }

    static class Info<T> {
        private final static long TIMEOUT = 10 * 1000; // 10s

        private final long timeStamp;
        private final T info;

        public Info(final T info) {
            timeStamp = System.currentTimeMillis();
            this.info = info;
        }

        public T getInfo() {
            return info;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timeStamp > TIMEOUT;
        }

        public boolean isNotExpired() {
            return !isExpired();
        }
    }
}