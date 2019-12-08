package com.herocraftonline.heroes.characters.skill.reborn.other;

//TODO use SkillUseEvent and based on config options
// e.g. region restrictions (list of corner points), + their world name (see Port skill for working with server/world names)
// all skills boolean flag
// maybe specific skill names to restrict by the regions

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.SkillUseEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class SkillRestrictedSkills extends PassiveSkill {
    private boolean enabled;
    private boolean restrictByRegions;
    private boolean restrictAllSkills;
    private boolean restrictOnlyPlayersWithSkill;
    private String restrictedByRegionMessage;

    //TODO store regions, so it doesn't need to be processed on each skill use
    private Set<RestrictedRegion> restrictedRegions;

    public SkillRestrictedSkills(Heroes plugin) {
        super(plugin, "RestrictedSkills");
        setDescription("You are unable to use certain skills in certain conditions!");

        Bukkit.getPluginManager().registerEvents(new SkillListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.APPLY_TEXT.node(), "");
        node.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        node.set(SkillSetting.LEVEL.node(), 1);

        node.set("enabled", false);
        node.set("restrict-by-regions", false);
        node.set("restrict-all-skills", false);
        node.set("restrict-only-players-with-this-skill", false);
        node.set("restricted-skills", new String[0]);//FIXME: see if this works
        node.set("restricted-regions", new String[] { "#Examples:", "#server_name:world_name:x1,y1,z1:x2,y2,z2", "#world_name:x1,y1,z1:x2,y2,z2", "#For any y levels: world_name:x1,*,z1:x2,*,z2"});//FIXME: see if this works
        node.set("restricted-by-region-message", "Use of this skill is restricted by this region.");
        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public void init() {
        super.init();

        // may not be customisable in the class config and only skill config. Though I don't think it'll need to be.
        enabled = SkillConfigManager.getRaw(this, "enabled", false);
        restrictByRegions = SkillConfigManager.getRaw(this, "restrict-by-regions", false);
        restrictAllSkills = SkillConfigManager.getRaw(this, "restrict-all-skills", false);
        restrictOnlyPlayersWithSkill = SkillConfigManager.getRaw(this, "restrict-only-players-with-this-skill", false);

        restrictedByRegionMessage = SkillConfigManager.getRaw(this, "restricted-by-region-message", "Use of this skill is restricted by this region.");
    }

    public class SkillListener implements Listener {
        private final Skill skill;

        public SkillListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
        public void onSkillUse(SkillUseEvent event) {
            if (!enabled || (restrictOnlyPlayersWithSkill && !event.getHero().canUseSkill(skill)) )
                return;

            // TODO check if skill is in restricted list
            List<String> restrictedSkillNames = SkillConfigManager.getUseSettingStringList(event.getHero(), skill, "restricted-skills");
            if (!restrictAllSkills && !restrictedSkillNames.contains(event.getSkill().getName())) {
                return;
            }

            if (restrictByRegions && inRestrictedRegion(event.getHero())){
                event.getPlayer().sendMessage(restrictedByRegionMessage);
                event.setCancelled(true);
            }
        }

        private boolean inRestrictedRegion(final Hero hero){
            final Player player = hero.getPlayer();
            final Location heroLocation = player.getLocation();

            // get restricted regions
            List<String> restrictedRegions = SkillConfigManager.getUseSettingStringList(hero, skill, "restricted-regions");
            if (restrictedRegions == null || restrictedRegions.isEmpty()){
                return false;
            }

            for (String regionString : restrictedRegions){
                if (regionString.startsWith("#")){
                    continue; // ignore commented strings (such as our examples)
                }

                List<String> regionArgs = getRegionArgs(regionString);
                if (regionArgs.size() < 4){
                    Heroes.log(Level.WARNING, "Invalid RestrictedSkill region \"" + regionString + "\".");
                    continue;
                }

                // Check validity of server name and if on same server
                // Ignoring check if no server name is given
                String serverName = regionArgs.get(0);
                if (StringUtils.isNotEmpty(serverName)) {
                    // Check validity of server name and if on same server
                    if (!plugin.getServerNames().contains(serverName)) {
                        Heroes.log(Level.WARNING, "Invalid server name \"" + serverName
                                + "\" for  RestrictedSkill region \"" + regionString + "\".");
                        continue;
                    } else if (!serverName.equals(plugin.getServerName())){
                        continue; // on different server
                    }
                }

                //Check validity of world name and if on same world
                World world = plugin.getServer().getWorld(regionArgs.get(1));
                if (world == null){
                    Heroes.log(Level.WARNING, "Invalid world name \"" + regionArgs.get(1)
                            + "\" for  RestrictedSkill region \"" + regionString + "\".");
                    continue;
                } else if (!world.equals(player.getWorld())){
                    continue; // on different world
                }

                double[] point1 = getCoordinates(regionArgs.get(2));
                double[] point2 = getCoordinates(regionArgs.get(3));
                boolean ignoreY = isIgnoringY(regionArgs.get(2)) || isIgnoringY(regionArgs.get(3));
                if (point1 == null || point2 == null){
                    Heroes.log(Level.WARNING, "Atleast one of the region corner points is invalid for RestrictedSkill region \""
                            + regionString + "\".");
                    continue;
                }

                Location regionLocation1 = new Location(world, point1[0], point1[1], point1[2]);
                Location regionLocation2 = new Location(world, point2[0], point2[1], point2[2]);
                return locationInRegion(heroLocation, regionLocation1, regionLocation2, ignoreY);
            }
            return false;
        }

    }

    //server_name:world_name:x1,y1,z1:x2,y2,z2
    private static List<String> getRegionArgs(String regionInfo){
        List<String> portArgs = Lists.newArrayList(regionInfo.split(":"));
        if (portArgs.size() < 4) {
            portArgs.add(0, null); // skip server check
        }
        return portArgs;
    }

    @Nullable
    private static double[] getCoordinates(String xyzString){
        String[] xyzStrings = xyzString.split(",");
        if (xyzStrings.length < 3)
            return null;

        double x, y, z;
        try {
            x = Double.parseDouble(xyzStrings[0]);
            y = xyzStrings[1].equals("*") ? 0 : Double.parseDouble(xyzStrings[1]);
            z = Double.parseDouble(xyzStrings[2]);
        } catch (NumberFormatException e){
            return null;
        }

        return new double[]{x,y,z};
    }

    private static boolean isIgnoringY(String xyzString){
        String[] xyzStrings = xyzString.split(",");
        if (xyzStrings.length < 3)
            return false;

        return xyzStrings[1].equals("*");
    }

    private static boolean locationInRegion(Location location, Location regionLocation1, Location regionLocation2, boolean ignoreY){
        double minX = Math.min(regionLocation1.getX(), regionLocation2.getX());
        double minY = Math.min(regionLocation1.getY(), regionLocation2.getY());
        double minZ = Math.min(regionLocation1.getZ(), regionLocation2.getZ());

        double maxX = Math.max(regionLocation1.getX(), regionLocation2.getX());
        double maxY = Math.max(regionLocation1.getY(), regionLocation2.getY());
        double maxZ = Math.max(regionLocation1.getZ(), regionLocation2.getZ());

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        // Debug
//        Heroes.debugLog(Level.INFO, String.format("minX<= : %d, <=maxX: %d \nminY<= : %d, <=maxY: %d \nminZ<= : %d, <=maxZ: %d",
//                minX <= x ? 1:0, x <= maxX ? 1:0, minY <= y ? 1:0, y <= maxY ? 1:0, minZ <= z ? 1:0, z <= maxZ ? 1:0));

        return (minX <= x && x <= maxX) && (ignoreY || (minY <= y && y <= maxY)) && (minZ <= z && z <= maxZ);
    }

    public class RestrictedRegion {
        public @Nullable String serverName;
        public @Nullable String worldName;
        public double[] minXYZ;
        public double[] maxXYZ;
        public boolean ignoreY;

        public RestrictedRegion(String worldName, double[] minXYZ, double[] maxXYZ, boolean ignoreY){
            this(null, worldName, minXYZ, maxXYZ, ignoreY);
        }

        public RestrictedRegion(@Nullable String serverName, @Nullable String worldName, double[] minXYZ, double[] maxXYZ, boolean ignoreY){
            this.serverName = serverName;
            this.worldName = worldName;
            this.minXYZ = minXYZ;
            this.maxXYZ = maxXYZ;
            this.ignoreY = ignoreY;
        }
    }
}
