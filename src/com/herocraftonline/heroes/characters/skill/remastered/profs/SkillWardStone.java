package com.herocraftonline.heroes.characters.skill.remastered.profs;

import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.util.Util;
import com.herocraftonline.townships.Townships;
import com.herocraftonline.townships.regions.RegionCoords;
import com.herocraftonline.townships.users.TownshipsUser;
import com.herocraftonline.townships.users.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseRunestone;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.*;

/**
 * Created By MysticMight August 2021
 */

// A runestone used during war that on use, puts up a shield in the immediate area to prevent explosions for a short-ish duration, perhaps 15 minutes
public class SkillWardStone extends SkillBaseRunestone {
    private Map<RegionCoords, ProtectionInfo> regionCoordsProtectionInfoMap = new HashMap<>(10);

    public SkillWardStone(Heroes plugin) {
        super(plugin, "WardStone");
        setDescription("You imbue a redstone block with an WardStone. WardStones $1");
        setUsage("/skill wardstone");
        setArgumentRange(0, 0);
        setIdentifiers("skill wardstone");
        setTypes(SkillType.ITEM_MODIFYING, SkillType.SILENCEABLE);


        defaultMaxUses = 1;
        defaultDelay = 5000;
        displayName = "WardStone";
        displayNameColor = ChatColor.AQUA;

        Bukkit.getServer().getPluginManager().registerEvents(new WardStoneListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DURATION.node(), 15000);
        return config;
    }

    public boolean tryUseItem(Hero hero, List<String> runestoneLoreData) {
        final Player player = hero.getPlayer();

        // Only permit use in a town
        final RegionCoords regionCoords = new RegionCoords(player.getLocation());
        if (!Townships.regionManager.hasRegion(regionCoords)) {
            player.sendMessage(ChatColor.RED + "This location has no town chunk to protect from explosions.");
            return false;
        }

        // Only permit protection once, don't extend the duration
        if (regionCoordsProtectionInfoMap.containsKey(regionCoords)) {
            ProtectionInfo protectionInfo = regionCoordsProtectionInfoMap.get(regionCoords);
            final TownshipsUser townshipsUser = UserManager.fromOfflinePlayer(player);
            final TownshipsUser protectorUser = UserManager.fromOfflinePlayer(protectionInfo.getProtector());
            if (townshipsUser.getTown().isAlly(protectorUser.getTown())) {
                // Show time left for allies.
                String timeLeftString = Util.decFormatCDs.format(protectionInfo.getTimeLeftMillis() / 1000.0);
                player.sendMessage("This location is already protected from explosions, and has "
                        + timeLeftString + "s left.");
            } else {
                // FIXME Should we allow non-allies to add protection (or rather check it)?
                player.sendMessage("This location is already protected from explosions!");
            }
            return false;
        }

        // Note lore data for this runestone is unused, at the moment atleast
        //String locationString = runestoneLoreData.get(0);

        // Add protection to map
        int duration = SkillConfigManager.getUseSettingInt(hero, this, SkillSetting.DURATION, false);
        regionCoordsProtectionInfoMap.put(regionCoords, new ProtectionInfo(System.currentTimeMillis(), player, duration));
        return true; // used
    }

    // TODO Create task to remove protected regions as they expire... (Want to run this every 1s or 20ish ticks I suppose
    // see https://bukkit.fandom.com/wiki/Scheduler_Programming#Repeating_Example
    // and https://riptutorial.com/bukkit/example/19345/scheduler-repeating-task

    public class WardStoneListener extends RunestoneListener {
        private Skill skill;

        public WardStoneListener(Skill skill) {
            // note purposely overrides default (super) constructor which registers its events
            this.skill = skill;
        }

        // Note RunestoneListener contains a block check event for placing runestones (specifically for the skill extending)

        @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
        public void tntExplosion(EntityExplodeEvent event) {
            if (event.getEntityType() != EntityType.PRIMED_TNT)
                return;

            // First check whether TNT is in protected area, if so stop it exploding.
            RegionCoords regionCoords = new RegionCoords(event.getLocation());
            if (regionCoordsProtectionInfoMap.containsKey(regionCoords)) {
                ProtectionInfo protectionInfo = regionCoordsProtectionInfoMap.get(regionCoords);
                if (protectionInfo.getTimeLeftMillis() > 0) {
                    event.setCancelled(true);
                    return;
                }
            }

            // Alright Tnt will definitely explode, lets prevent damage to certain protected blocks (e.g. along chunk edges)
            Iterator<Block> iter = event.blockList().iterator();
            Set<RegionCoords> allow = new HashSet<>();
            Set<RegionCoords> remove = new HashSet<>();
            while (iter.hasNext()) {
                regionCoords = new RegionCoords(iter.next().getLocation());

                // Reduce double checking using these set checks (since RegionCoords is unique to a chunk of coordinates)
                if (remove.contains(regionCoords)) {
                    iter.remove(); // this region is already known to be protected, remove from list to explode
                    continue;
                }
                if (allow.contains(regionCoords))
                    continue; // Keep in of blocks list to explode

                if (regionCoordsProtectionInfoMap.containsKey(regionCoords)) {
                    ProtectionInfo protectionInfo = regionCoordsProtectionInfoMap.get(regionCoords);
                    if (protectionInfo.getTimeLeftMillis() > 0) {
                        remove.add(regionCoords);
                        iter.remove();
                        continue;
                    }
                }
                allow.add(regionCoords);
            }
        }
    }

    public class ProtectionInfo {
        private final long timeProtectedMilliseconds;
        private final Player protector;
        private final int duration;

        public ProtectionInfo(long timeProtectedMilliseconds, Player protector, int duration) {
            this.timeProtectedMilliseconds = timeProtectedMilliseconds;
            this.protector = protector;
            this.duration = duration;
        }

        public long getTimeProtectedMillis() {
            return timeProtectedMilliseconds;
        }

        public long getTimeLeftMillis() {
            return (timeProtectedMilliseconds + duration) - System.currentTimeMillis();
        }

        public Player getProtector() {
            return protector;
        }
    }
}