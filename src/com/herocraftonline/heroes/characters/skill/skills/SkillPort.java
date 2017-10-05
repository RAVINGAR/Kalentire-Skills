package com.herocraftonline.heroes.characters.skill.skills;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.SkillResult.ResultType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Messaging;
import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.*;
import java.util.*;
import java.util.logging.Level;

public class SkillPort extends ActiveSkill implements Listener, PluginMessageListener {

    private Set<String> pendingPort = new HashSet<>();
    private Map<String, Info<String>> onJoinSkillSettings = new Hashtable<>();

	public SkillPort(Heroes plugin) {
		super(plugin, "Port");
		setDescription("You teleport yourself and party members within $1 blocks to the set location!");
		setUsage("/skill port <location>");
		setArgumentRange(1, 1);
		setIdentifiers("skill port");
        setTypes(SkillType.TELEPORTING, SkillType.AREA_OF_EFFECT, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE);

        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, Heroes.BUNGEE_CORD_CHANNEL, this);
	}

	@Override
	public String getDescription(Hero hero) {
		int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 10, false);

		return getDescription().replace("$1", radius + "");
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.RADIUS.node(), 10);
        node.set(SkillSetting.NO_COMBAT_USE.node(), true);
        node.set(SkillSetting.DELAY.node(), 10000);

		return node;
	}

	@Override
	public SkillResult use(Hero hero, String[] args) {
		Player player = hero.getPlayer();

		List<String> keys = new ArrayList<>(SkillConfigManager.getUseSettingKeys(hero, this, null));


        if (args.length < this.getMinArguments() || args.length > this.getMaxArguments()) {
            Messaging.send(player, "You must specify a location when using this skill! (use /skill port list)");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

		// Strip non-world keys
		for (SkillSetting setting : SkillSetting.values()) {
			keys.remove(setting.node());
		}
		keys.remove("cross-world");

		if (args[0].equalsIgnoreCase("list")) {
			for (String n : keys) {
				String retrievedNode = SkillConfigManager.getUseSetting(hero, this, n, (String) null);
				if (retrievedNode != null) {
					Messaging.send(player, "$1 - $2", n, retrievedNode);
				}
			}
			return SkillResult.SKIP_POST_USAGE;
		}

		String portInfo = SkillConfigManager.getUseSetting(hero, this, args[0].toLowerCase(), (String) null);
		if (portInfo != null) {
		    List<String> portArgs = getPortArgs(portInfo);

			int levelRequirement = Integer.parseInt(portArgs.get(5));
            if (hero.getHeroLevel(this) < levelRequirement) {
                return new SkillResult(ResultType.LOW_LEVEL, true, levelRequirement);
            }

            boolean crossWorldEnabled = SkillConfigManager.getUseSetting(hero, this, "cross-world", false);

            // handle bungee port
            String server = portArgs.get(0);
            if (StringUtils.isNotEmpty(server) && !server.equals(plugin.getServerName())) {
                if (plugin.getServerNames().contains(server)) {
                    if (crossWorldEnabled) {
                        ByteArrayDataOutput portRequest = ByteStreams.newDataOutput();
                        portRequest.writeUTF("Forward");
                        portRequest.writeUTF(server);
                        portRequest.writeUTF("PortRequest");

                        ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
                        DataOutputStream msgout = new DataOutputStream(msgbytes);
                        try {
                            msgout.writeUTF(portInfo);
                            Collection<String> playerNames = getPortMemberNames(hero, new Location(hero.getPlayer().getWorld(), Double.parseDouble(portArgs.get(2)),
                                    Double.parseDouble(portArgs.get(3)), Double.parseDouble(portArgs.get(4))));
                            msgout.writeUTF(Joiner.on(",").join(playerNames));
                        }
                        catch (IOException e) {
                            Messaging.send(player, "Port location is improperly set!");
                            return SkillResult.SKIP_POST_USAGE;
                        }

                        portRequest.writeShort(msgbytes.toByteArray().length);
                        portRequest.write(msgbytes.toByteArray());

                        pendingPort.add(player.getName());
                        player.sendPluginMessage(plugin, Heroes.BUNGEE_CORD_CHANNEL, portRequest.toByteArray());

                        // Run this delayed task to check if the port failed
                        final String playerName = player.getName();
                        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {

                            @Override
                            public void run()
                            {
                                if (pendingPort.remove(playerName)) {
                                    Player player = Bukkit.getPlayer(playerName);
                                    if (player != null) {
                                        Messaging.send(player, "Teleport fizzled.");
                                    }
                                }
                            }
                        }, 40);

                        return SkillResult.NORMAL;
                    }
                    else {
                        Messaging.send(player, "You can't port to a location in another world!");
                        return SkillResult.INVALID_TARGET_NO_MSG;
                    }
                }
                else {
                    Messaging.send(player, "That teleport location no longer exists!");
                    return SkillResult.INVALID_TARGET_NO_MSG;
                }
			}

            return doPort(hero, portInfo, true);
		}
		else {
			Messaging.send(player, "No port location named $1", args[0]);
			return SkillResult.INVALID_TARGET_NO_MSG;
		}
	}

	@Override
	public boolean isWarmupRequired(String[] args)
	{
	    return args == null || args.length < 1 || !"list".equalsIgnoreCase(args[0]);
	}

	@Override
	public boolean isCoolDownRequired(String[] args)
	{
        return args == null || args.length < 1 || !"list".equalsIgnoreCase(args[0]);
	}

    private SkillResult doPort(Hero hero, String portInfo, boolean isDeparting)
    {
        Player player = hero.getPlayer();
        List<String> portArgs = getPortArgs(portInfo);
        boolean crossWorldEnabled = SkillConfigManager.getUseSetting(hero, this, "cross-world", false);
        
        World world = plugin.getServer().getWorld(portArgs.get(1));
        if (world == null) {
        	Messaging.send(player, "That teleport location no longer exists!");
        	return SkillResult.INVALID_TARGET_NO_MSG;
        }
        else if (!world.equals(player.getWorld()) && !crossWorldEnabled) {
        	Messaging.send(player, "You can't port to a location in another world!");
        	return SkillResult.INVALID_TARGET_NO_MSG;
        }

        if (isDeparting) {
            broadcastExecuteText(hero);
            hero.getPlayer().getWorld().spigot().playEffect(player.getLocation(), Effect.MAGIC_CRIT, 0, 0, 0, 0.1F, 0, 0.5F, 50, 12);
        }

        Location portLocation = new Location(world, Double.parseDouble(portArgs.get(2)), Double.parseDouble(portArgs.get(3)), Double.parseDouble(portArgs.get(4)));
        Collection<Hero> portMembers = getPortMembers(hero, portLocation);

        for (Hero member : portMembers) {
            Player memberPlayer = member.getPlayer();
            memberPlayer.teleport(portLocation);
        }

        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), CompatSound.BLOCK_PORTAL_TRAVEL.value(), 0.5F, 1.0F);
        hero.getPlayer().getWorld().spigot().playEffect(player.getLocation(), Effect.MAGIC_CRIT, 0, 0, 0, 0.1F, 0, 0.5F, 50, 12);
        return SkillResult.NORMAL;
    }

    private List<String> getPortArgs(String portInfo)
    {
        List<String> portArgs = Lists.newArrayList(portInfo.split(":"));
        if (portArgs.size() < 6) {
            portArgs.add(0, plugin.getServerName());
        }
        return portArgs;
    }

    private Collection<String> getPortMemberNames(Hero hero, Location portLocation)
    {
        List<String> memberNames = new ArrayList<String>();
        for (Hero member : getPortMembers(hero, portLocation)) {
            memberNames.add(member.getPlayer().getName());
        }
        return memberNames;
    }

    private Collection<Hero> getPortMembers(Hero hero, Location portLocation)
    {
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 10, false);
        int radiusSquared = radius * radius;
        Location playerLocation = hero.getPlayer().getLocation();
        List<Hero> members = new ArrayList<>();

        if (hero.hasParty()) {
            // Player has party. Port his party, if they are close enough.
            for (Hero member : hero.getParty().getMembers()) {
                Player memberPlayer = member.getPlayer();

                if (portLocation.getWorld().equals(memberPlayer.getWorld())) {
                    // Distance check the rest of the party
                    if (memberPlayer.getLocation().distanceSquared(playerLocation) <= radiusSquared) {
                        members.add(member);
                    }
                }
            }
        }
        else {
            // Player doesn't have a party, just add him.
            members.add(hero);
        }

        return members;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message)
    {
        if (!Heroes.BUNGEE_CORD_CHANNEL.equals(channel)) {
            return;
        }
    
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();

        if ("PortRequest".equals(subChannel)) {
            short len = in.readShort();
            byte[] msgbytes = new byte[len];
            in.readFully(msgbytes);
            DataInputStream msgin = new DataInputStream(new ByteArrayInputStream(msgbytes));
        
            try {
                String portInfo = msgin.readUTF();
                String playerNames = msgin.readUTF();
                for (String playerName : Splitter.on(",").split(playerNames)) {
                    // cache the location for onPlayerJoin
                    onJoinSkillSettings.put(playerName, new Info<String>(portInfo));
    
                    // send the player to this server
                    ByteArrayDataOutput connectOther = ByteStreams.newDataOutput();
                    connectOther.writeUTF("ConnectOther");
                    connectOther.writeUTF(playerName);
                    connectOther.writeUTF(plugin.getServerName());
    
                    player.sendPluginMessage(plugin, Heroes.BUNGEE_CORD_CHANNEL, connectOther.toByteArray());
                }
            }
            catch (IOException e) {
                Heroes.log(Level.SEVERE, "SkillPort: Could not parse PortRequest message from remote server");
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Hero hero = plugin.getCharacterManager().getHero(player);
        Info<String> portInfo = onJoinSkillSettings.remove(player.getName());
        if (portInfo != null && portInfo.isNotExpired()) {
            SkillResult result = doPort(hero, portInfo.getInfo(), false);
            if (!SkillResult.NORMAL.equals(result)) {
                player.teleport(player.getWorld().getSpawnLocation());
                Messaging.send(player, "Teleport fizzled.");
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (pendingPort.remove(player.getName())) {
            broadcastExecuteText(plugin.getCharacterManager().getHero(player));
            player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_WITHER_SPAWN.value(), 0.5F, 1.0F);
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