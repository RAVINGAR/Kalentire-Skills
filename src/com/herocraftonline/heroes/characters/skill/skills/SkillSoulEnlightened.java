package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.events.DisallowedPVPEvent;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.*;

public class SkillSoulEnlightened extends PassiveSkill {

    private static final String NO_PVP_REGIONS = "no-pvp-regions";

    private final WorldGuardPlugin worldGuard;

    public SkillSoulEnlightened(Heroes plugin) {
        super(plugin, "SoulEnlightened");
        setDescription("Your soul has been enlightened!");
        setArgumentRange(0, 0);
        Bukkit.getServer().getPluginManager().registerEvents(new AttackOnSightListener(), plugin);
        worldGuard = (WorldGuardPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(NO_PVP_REGIONS, new ArrayList<String>());

        return node;
    }

    public class AttackOnSightListener implements Listener {

        @EventHandler()
        public void onDisallowedPVP(DisallowedPVPEvent event) {
            Hero defender = plugin.getCharacterManager().getHero(event.getDefender());
            if (defender.canUseSkill(SkillSoulEnlightened.this)) {

                RegionManager manager = worldGuard.getRegionManager(defender.getPlayer().getLocation().getWorld());
                Set<ProtectedRegion> set = manager.getApplicableRegions(defender.getPlayer().getLocation()).getRegions();

                for (String noPvpRegion : SkillConfigManager.getUseSetting(defender, SkillSoulEnlightened.this, NO_PVP_REGIONS, Collections.<String>emptyList())) {
                   if (set.contains(noPvpRegion)) {
                       return;
                   }
                }

                event.setCancelled(true);
            }
        }
    }
}
