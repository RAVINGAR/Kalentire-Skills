package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.BlockIterator;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;
import com.herocraftonline.heroes.util.Util;

public class SkillBlink extends ActiveSkill {

    public SkillBlink(Heroes plugin) {
        super(plugin, "Blink");
        setDescription("Teleports you up to $1 blocks away.");
        setUsage("/skill blink");
        setArgumentRange(0, 0);
        setIdentifiers("skill blink");
        setTypes(SkillType.SILENCABLE, SkillType.TELEPORT);
        
        Bukkit.getServer().getPluginManager().registerEvents(new SkillPlayerListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.MAX_DISTANCE.node(), 6);
        node.set("restrict-ender-pearl", true);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        Location loc = player.getLocation();
        if (loc.getBlockY() > loc.getWorld().getMaxHeight() || loc.getBlockY() < 1) {
            Messaging.send(player, "The void prevents you from blinking!");
            return SkillResult.FAIL;
        }
        int distance = SkillConfigManager.getUseSetting(hero, this, Setting.MAX_DISTANCE, 6, false);
        Block prev = null;
        Block b;
        BlockIterator iter = null;
        try {
            iter = new BlockIterator(player, distance);
        } catch (IllegalStateException e) {
            Messaging.send(player, "There was an error getting your blink location!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        while (iter.hasNext()) {
            b = iter.next();
            if (Util.transparentBlocks.contains(b.getType()) && (Util.transparentBlocks.contains(b.getRelative(BlockFace.UP).getType()) || Util.transparentBlocks.contains(b.getRelative(BlockFace.DOWN).getType()))) {
                prev = b;
            } else {
                break;
            }
        }
        if (prev != null) {
            Location teleport = prev.getLocation().clone();
            // Set the blink location yaw/pitch to that of the player
            teleport.setPitch(player.getLocation().getPitch());
            teleport.setYaw(player.getLocation().getYaw());
            player.teleport(teleport);
            return SkillResult.NORMAL;
        } else {
            Messaging.send(player, "No location to blink to.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int distance = SkillConfigManager.getUseSetting(hero, this, Setting.MAX_DISTANCE, 6, false);
        return getDescription().replace("$1", distance + "");
    }
    
    public class SkillPlayerListener implements Listener {

        private final Skill skill;
        
        public SkillPlayerListener(Skill skill) {
            this.skill = skill;
        }
        
        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onPlayerTeleport(PlayerTeleportEvent event) {
            Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
            if (!SkillConfigManager.getUseSetting(hero, skill, "restrict-ender-pearl", true)) {
                return;
            } else if (hero.getSkillLevel(skill) < 1 && event.getCause() == TeleportCause.ENDER_PEARL) {
                event.setCancelled(true);
            }
        }
    }
}