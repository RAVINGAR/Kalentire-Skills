package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseRunestone;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class SkillAncientRunestone extends SkillBaseRunestone {

    private final ConcurrentHashMap<Player, List<ItemStack>> soulboundRunestones;

    public SkillAncientRunestone(final Heroes plugin) {
        super(plugin, "AncientRunestone");
        setDescription("You imbue a redstone block with an Ancient Runestone.");
        setUsage("/skill ancientrunestone");
        setArgumentRange(0, 0);
        setIdentifiers("skill ancientrunestone");
        setTypes(SkillType.ITEM_MODIFYING, SkillType.SILENCEABLE);

        defaultMaxUses = -1;
        defaultDelay = 10000;
        displayName = "Ancient Runestone";
        displayNameColor = ChatColor.GOLD;

        soulboundRunestones = new ConcurrentHashMap<>();

        new AncientRunestoneListener(this);
    }

    protected class AncientRunestoneListener extends RunestoneListener implements Listener {

        private final Skill skill;

        public AncientRunestoneListener(final Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onPlayerQuit(final PlayerQuitEvent event) {
            if (!(soulboundRunestones.containsKey(event.getPlayer()))) {
                return;
            }

            final Player player = event.getPlayer();
            final Hero hero = plugin.getCharacterManager().getHero(player);

            final List<ItemStack> runestoneDataPairs = soulboundRunestones.get(player);

            int i = 1;
            for (final ItemStack item : runestoneDataPairs) {
                hero.setSkillSetting(skill, Integer.toString(i), item);
                i++;
            }

            // He's saved to the file now, don't need to keep him in the map anymore.
            soulboundRunestones.remove(player);

            // Save just in case - we're now removing this due to skillSettings being saved more efficiently.
            //plugin.getCharacterManager().saveHero(hero, true);
        }

        @EventHandler(priority = EventPriority.HIGH)
        public void onPlayerJoin(final PlayerJoinEvent event) throws InvalidConfigurationException {

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                final Player player = event.getPlayer();
                final Hero hero = plugin.getCharacterManager().getHero(player);
                final ConfigurationSection skillSettings = hero.getSkillSettings("ancientrunestone");
                if (skillSettings == null) {
                    return;
                }

                try {
                    int i = 0;
                    for (final String key : skillSettings.getKeys(false)) {
                        final ItemStack runeStone = (ItemStack) skillSettings.get(key);
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
                } catch (final NumberFormatException e) {
                    Heroes.log(Level.WARNING, "AncientRunestone Configuraiton Error; expected a number.");
                }
            }, 30L);

        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onPlayerDeath(final PlayerDeathEvent event) {

            if (event.getDrops().size() == 0) {
                return;
            }

            final Player player = event.getEntity();

            for (final ItemStack item : new HashSet<>(event.getDrops())) {
                if (item == null) {
                    continue;
                }

                if (item.getType() != Material.REDSTONE_BLOCK) {
                    continue;
                }

                final ItemMeta metaData = item.getItemMeta();

                if (metaData == null || metaData.getDisplayName() == null) {
                    continue;
                }

                if (!(metaData.getDisplayName().contains("Ancient Runestone"))) {
                    continue;
                }

                // We have a runestone. Remove it from the drops, and get ready to place it back on the player
                event.getDrops().remove(item);

                addRunestoneToSoulBoundList(player, item);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        private void onPlayerRespawn(final PlayerRespawnEvent event) {
            if (!(soulboundRunestones.containsKey(event.getPlayer()))) {
                return;
            }

            final Player player = event.getPlayer();
            final List<ItemStack> runestoneDataPairs = soulboundRunestones.get(player);

            // Loop through the items and give them back to the player.
            final PlayerInventory inventory = player.getInventory();
            for (final ItemStack item : runestoneDataPairs) {
                inventory.addItem(item);
            }
            player.updateInventory();

            soulboundRunestones.remove(player);
        }

        private void addRunestoneToSoulBoundList(final Player player, final ItemStack item) {
            // Add the runestone data to the list.
            if (soulboundRunestones.isEmpty()) {
                // Initialize our item data pairs
                final List<ItemStack> runestoneDataPairs = new ArrayList<>();

                // Add the paired data to the list
                runestoneDataPairs.add(item);

                // Pair the paired data to our player
                soulboundRunestones.put(player, runestoneDataPairs);
            } else {
                // Our main map is not empty. However, we might not have an entry for the current player.

                // Check if the player is on the map
                if (soulboundRunestones.containsKey(player)) {
                    // The player is on the map, and thus already has at least 1 runestone being tracked.
                    // Let's add this new runestone to the list.

                    final List<ItemStack> runestoneDataPairs = soulboundRunestones.get(player);
                    runestoneDataPairs.add(item);
                } else {
                    // The player is not on the map.
                    // Let's add the runestone to the list.

                    final List<ItemStack> runestoneDataPairs = new ArrayList<>();
                    runestoneDataPairs.add(item);

                    // Pair the paired data to our player
                    soulboundRunestones.put(player, runestoneDataPairs);
                }
            }
        }
    }
}