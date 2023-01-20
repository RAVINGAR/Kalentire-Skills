package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.bukkit.protection.events.DisallowedPVPEvent;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SkillSoulEnlightened extends PassiveSkill {

    private static final String NO_PVP_REGIONS = "no-pvp-regions";

    private final WorldGuardPlugin worldGuard;

    public SkillSoulEnlightened(Heroes plugin) {
        super(plugin, "SoulEnlightened");
        setDescription("You are soul enlightened and have proven yourself worthy.  Those who wield these powers are at constant risk of attacks.");
        setArgumentRange(0, 0);
        Bukkit.getServer().getPluginManager().registerEvents(new AttackOnSightListener(this), plugin);
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
        private final Skill skill;

        AttackOnSightListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler()
        public void onDisallowedPVP(DisallowedPVPEvent event) {
            Hero defender = plugin.getCharacterManager().getHero(event.getDefender());
            if (!defender.canUseSkill(skill))
                return;

            List<String> noPvpRegions = SkillConfigManager.getUseSetting(defender, skill, NO_PVP_REGIONS, Collections.<String>emptyList());
            if (noPvpRegions.isEmpty())
                return;

            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();

            Location defenderLoc = event.getDefender().getLocation();
            com.sk89q.worldedit.util.Location wgDefenderLoc = BukkitAdapter.adapt(defenderLoc);

            for (ProtectedRegion region : query.getApplicableRegions(wgDefenderLoc)) {
                if (noPvpRegions.contains(region.getId()))
                    return;
            }

            Location attackerLoc = event.getAttacker().getLocation();
            com.sk89q.worldedit.util.Location wgAttackerLoc = BukkitAdapter.adapt(attackerLoc);

            for (ProtectedRegion region : query.getApplicableRegions(wgAttackerLoc)) {
                if (noPvpRegions.contains(region.getId()))
                    return;
            }

            event.setCancelled(true);
        }
    }
}