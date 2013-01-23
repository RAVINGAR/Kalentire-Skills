package com.herocraftonline.heroes.characters.skill.skills;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.SkillResult.ResultType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;

public class SkillPort extends ActiveSkill {

    public SkillPort(Heroes plugin) {
        super(plugin, "Port");
        setDescription("You teleport yourself and party members within $1 blocks to the set location!");
        setUsage("/skill port <location>");
        setArgumentRange(1, 1);
        setIdentifiers("skill port");
        setTypes(SkillType.TELEPORT, SkillType.SILENCABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.RADIUS.node(), 10);
        node.set(Setting.NO_COMBAT_USE.node(), true);
        node.set("cross-world", false);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        List<String> keys = new ArrayList<String>(SkillConfigManager.getUseSettingKeys(hero, this, null));
        // Strip non-world keys
        for (Setting setting : Setting.values()) {
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
            String[] splitArg = portInfo.split(":");
            int levelRequirement = Integer.parseInt(splitArg[4]);
            World world = plugin.getServer().getWorld(splitArg[0]);
            if (world == null) {
                Messaging.send(player, "That teleport location no longer exists!");
                return SkillResult.INVALID_TARGET_NO_MSG;
            } else if (!world.equals(player.getWorld()) && !SkillConfigManager.getUseSetting(hero, this, "cross-world", false)) {
                Messaging.send(player, "You can't port to a location in another world!");
                return SkillResult.INVALID_TARGET_NO_MSG;
            }
            if (hero.getSkillLevel(this) < levelRequirement) {
                return new SkillResult(ResultType.LOW_LEVEL, true, levelRequirement);
            }
            int radiusInc = (int) SkillConfigManager.getUseSetting(hero, this, Setting.RADIUS_INCREASE, 0.0, false) * hero.getSkillLevel(this);
            int range = (int) Math.pow(SkillConfigManager.getUseSetting(hero, this, Setting.RADIUS, 10, false) + radiusInc, 2);
            Location loc = new Location(world, Double.parseDouble(splitArg[1]), Double.parseDouble(splitArg[2]), Double.parseDouble(splitArg[3]));
            broadcastExecuteText(hero);
            if (!hero.hasParty()) {
                player.teleport(loc);
                return SkillResult.NORMAL;
            }

            Location castLocation = player.getLocation().clone();
            for (Hero pHero : hero.getParty().getMembers()) {
                if (!castLocation.getWorld().equals(player.getWorld())) {
                    continue;
                }
                // Always teleport the caster
                if (pHero.equals(hero)) {
                    pHero.getPlayer().teleport(loc);
                    continue;
                }
                //Distance check the rest of the party
                double distance = castLocation.distanceSquared(pHero.getPlayer().getLocation());
                if (distance <= range) {
                    pHero.getPlayer().teleport(loc);
                }
            }
            hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.PORTAL_TRAVEL , 0.5F, 1.0F);
            return SkillResult.NORMAL;
        } else {
            Messaging.send(player, "No port location named $1", args[0]);
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int radius = SkillConfigManager.getUseSetting(hero, this, Setting.RADIUS, 10, false);
        radius += (int) SkillConfigManager.getUseSetting(hero, this, Setting.RADIUS_INCREASE, 0.0, false) * hero.getSkillLevel(this);
        return getDescription().replace("$1", radius + "");
    }
}
