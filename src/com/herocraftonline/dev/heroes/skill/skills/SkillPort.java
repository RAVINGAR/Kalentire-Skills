package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.config.ConfigurationNode;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.herocraftonline.dev.heroes.util.Setting;

public class SkillPort extends ActiveSkill {

    public SkillPort(Heroes plugin) {
        super(plugin, "Port");
        setDescription("Teleports you and your nearby party to the set location!");
        setUsage("/skill port <location>");
        setArgumentRange(1, 1);
        setIdentifiers("skill port");
        setTypes(SkillType.TELEPORT, SkillType.SILENCABLE);
    }

    @Override
    public ConfigurationNode getDefaultConfig() {
        ConfigurationNode node = super.getDefaultConfig();
        node.setProperty(Setting.RADIUS.node(), 10);
        return node;
    }

    @Override
    public boolean use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        if (args[0].equalsIgnoreCase("list")) {
            for (String n : getConfig().getKeys()) {
                String retrievedNode = getSetting(hero, n, (String) null);
                if (retrievedNode != null && retrievedNode.split(":").length == 5) {
                    Messaging.send(player, "$1 - $2", n, retrievedNode);
                }
            }
            return false;
        }

        if (getSetting(hero, args[0].toLowerCase(), (String) null) != null) {
            String[] splitArg = getSetting(hero, args[0].toLowerCase(), (String) null).split(":");
            int levelRequirement = Integer.parseInt(splitArg[4]);
            World world = plugin.getServer().getWorld(splitArg[0]);
            if (world == null) {
                Messaging.send(player, "That teleport location no longer exists!");
            }

            if (hero.getLevel() < levelRequirement) {
                Messaging.send(player, "Sorry, you need to be level $1 to use that!", levelRequirement);
                return false;
            }

            int range = (int) Math.pow(getSetting(hero, Setting.RADIUS.node(), 10, false), 2);
            Location loc = new Location(world, Double.parseDouble(splitArg[1]), Double.parseDouble(splitArg[2]), Double.parseDouble(splitArg[3]));
            broadcastExecuteText(hero);
            if (!hero.hasParty()) {
                player.teleport(loc);
                return true;
            }

            Location castLocation = player.getLocation();
            for (Hero pHero : hero.getParty().getMembers()) {
                if (!castLocation.getWorld().equals(player.getWorld())) {
                    continue;
                }
                double distance = castLocation.distanceSquared(pHero.getPlayer().getLocation());
                if (distance <= range) {
                    pHero.getPlayer().teleport(loc);
                }
            }

            return true;
        } else
            return false;
    }
}
