package com.herocraftonline.heroes.characters.skill.skills;

import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;
import com.herocraftonline.townships.HeroTowns;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

public class SkillMark extends ActiveSkill {

    private boolean herotowns = false;
    //private HeroTowns ht;
    private WorldGuardPlugin wgp;
    private boolean worldguard = false;
    protected String skillSettingsName;

    protected SkillMark(Heroes plugin, String name) {
        super(plugin, name);
        setDescription("You mark a location for use with recall.");
        setUsage("/skill mark <info|reset>");
        setArgumentRange(0, 1);
        setIdentifiers("skill mark");
        setTypes(SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_MAGICAL);
        skillSettingsName = "Recall";

        try {
            /*if (Bukkit.getServer().getPluginManager().getPlugin("HeroTowns") != null) {
                herotowns = true;
                ht = (HeroTowns) this.plugin.getServer().getPluginManager().getPlugin("HeroTowns");
            }*/

            if (Bukkit.getServer().getPluginManager().getPlugin("WorldGuard") != null) {
                worldguard = true;
                wgp = (WorldGuardPlugin) this.plugin.getServer().getPluginManager().getPlugin("WorldGuard");
            }
        }
        catch (Exception e) {
            Heroes.log(Level.SEVERE, "SkillRecall: Could not get Residence or HeroTowns plugins! Region checking may not work!");
        }
    }

    public SkillMark(Heroes plugin) {
        this(plugin, "Mark");
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.NO_COMBAT_USE.node(), true);
        node.set(SkillSetting.COOLDOWN.node(), 3180000);
        node.set(SkillSetting.DELAY.node(), 5000);
        node.set(SkillSetting.REAGENT.node(), 265);
        node.set(SkillSetting.REAGENT_COST.node(), 1);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        ConfigurationSection skillSettings = hero.getSkillSettings(skillSettingsName);

        if (args.length > 0 && args[0].equalsIgnoreCase("reset")) {
            clearStoredData(skillSettings);
            Messaging.send(player, "Your recall location has been cleared.");
            return SkillResult.SKIP_POST_USAGE;
        } else if (args.length > 0 ) {
            // Display the info about the current mark
            World world = getValidWorld(skillSettings, player.getName());
            if (world == null) {
                return SkillResult.FAIL;
            }
            double[] xyzyp;
            try {
                xyzyp = createLocationData(skillSettings);
            } catch (IllegalArgumentException e) {
                Messaging.send(player, "Your recall location is improperly set!");
                return SkillResult.SKIP_POST_USAGE;
            }
            if (StringUtils.isNotEmpty(skillSettings.getString("server"))) {
                Messaging.send(player, "Your recall is currently marked on $1,$2 at: $3, $4, $5", skillSettings.getString("server"), world.getName(), (int) xyzyp[0], (int) xyzyp[1], (int) xyzyp[2]);
            }
            else {
                Messaging.send(player, "Your recall is currently marked on $1 at: $2, $3, $4", world.getName(), (int) xyzyp[0], (int) xyzyp[1], (int) xyzyp[2]);
            }
            return SkillResult.SKIP_POST_USAGE;
        } else {
            // Save a new mark
            Location loc = player.getLocation();

            // Validate Herotowns
            if (herotowns) {
                /*if (!ht.getGlobalRegionManager().canBuild(player, loc)) {
                    Messaging.send(player, "You cannot Mark in a Town you have no access to!");
                    return SkillResult.FAIL;
                }*/
            }

            // Validate WorldGuard
            if (worldguard) {
                if (!wgp.canBuild(player, loc)) {
                    Messaging.send(player, "You cannot Mark in a Region you have no access to!");
                    return SkillResult.FAIL;
                }
            }

            if (plugin.getServerName() != null) {
                hero.setSkillSetting(skillSettingsName, "server", plugin.getServerName());
            }
            hero.setSkillSetting(skillSettingsName, "world", loc.getWorld().getName());
            hero.setSkillSetting(skillSettingsName, "x", loc.getX());
            hero.setSkillSetting(skillSettingsName, "y", loc.getY());
            hero.setSkillSetting(skillSettingsName, "z", loc.getZ());
            hero.setSkillSetting(skillSettingsName, "yaw", (double) loc.getYaw());
            hero.setSkillSetting(skillSettingsName, "pitch", (double) loc.getPitch());
            Object[] obj = new Object[] { loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ() };
            Messaging.send(player, "You have marked a new location on $1 at: $2, $3, $4", obj);

            //plugin.getCharacterManager().saveHero(hero, false); (remove this as its now being saved with skillsettings.
            hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.WITHER_SPAWN , 0.5F, 1.0F); 
            return SkillResult.NORMAL;
        }
    }

    @Override
    public boolean isWarmupRequired(String[] args)
    {
        return args != null ? !(args.length > 0) : true;
    }

    @Override
    public boolean isCoolDownRequired(String[] args)
    {
        return args != null ? !(args.length > 0) : true;
    }

    public static void clearStoredData(ConfigurationSection skillSettings) {
        skillSettings.set("server", null);
        skillSettings.set("world", null);
        skillSettings.set("x", null);
        skillSettings.set("y", null);
        skillSettings.set("z", null);
        skillSettings.set("yaw", null);
        skillSettings.set("pitch", null);
    }

    public static double[] createLocationData(ConfigurationSection skillSettings) throws IllegalArgumentException {
        double[] xyzyp = new double[5];
        Double temp;
        temp = Util.toDouble(skillSettings.get("x"));
        if (temp == null) {
            throw new IllegalArgumentException("Bad recall data.");
        }
        xyzyp[0] = temp;
        temp = Util.toDouble(skillSettings.get("y"));
        if (temp == null) {
            throw new IllegalArgumentException("Bad recall data.");
        }
        xyzyp[1] = temp;
        temp = Util.toDouble(skillSettings.get("z"));
        if (temp == null) {
            throw new IllegalArgumentException("Bad recall data.");
        }
        xyzyp[2] = temp;
        temp = Util.toDouble(skillSettings.get("yaw"));
        if (temp == null) {
            throw new IllegalArgumentException("Bad recall data.");
        }
        xyzyp[3] = temp;
        temp = Util.toDouble(skillSettings.get("pitch"));
        if (temp == null) {
            throw new IllegalArgumentException("Bad recall data.");
        }
        xyzyp[4] = temp;

        return xyzyp;
    }

    public static World getValidWorld(ConfigurationSection skillSetting, String playerName) {
        World world = null;

        if (skillSetting != null && StringUtils.isNotEmpty(skillSetting.getString("world"))) {
            world = Bukkit.getServer().getWorld(skillSetting.getString("world"));
        }

        Player player = Bukkit.getPlayer(playerName);
        if (world == null && player != null) {
            Messaging.send(player, "You have an invalid recall location marked!");
        }

        return world;
    }
}
