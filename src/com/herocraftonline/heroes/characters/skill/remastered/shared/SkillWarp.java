package com.herocraftonline.heroes.characters.skill.remastered.shared;

//oldsrc=http://pastie.org/private/hwkllkpsglhwd27qfhpqfg

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SkillWarp extends ActiveSkill {

    public SkillWarp(Heroes plugin) {
        super(plugin, "Warp");
        setDescription("Teleports you to a safe location in your current world.");
        setUsage("/skill warp");
        setIdentifiers("skill warp");
        setArgumentRange(0, 0);
        setTypes(SkillType.TELEPORTING, SkillType.SILENCEABLE);
    }

    public String getDescription(Hero hero) {
        return getDescription();
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("default-destination", "world");
        config.set("description", "a set location");
        return config;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        String defaultDestinationString = SkillConfigManager.getUseSetting(hero, this, "default-destination", "world");
        List<String> possibleDestinations = new ArrayList<String>(SkillConfigManager.getUseSettingKeys(hero, this, "destinations"));
        Location destination = null;
        World world = player.getWorld();
        for (String arg : possibleDestinations) {
            if (world.getName().equalsIgnoreCase(arg)) {
                String[] destArgs = SkillConfigManager.getUseSetting(hero, this, "destinations." + arg, "0,64,0").split(",");
                if (destArgs.length == 3) {
                    // This means the destination should be valid
                    destination = new Location(world, Double.parseDouble(destArgs[0]), Double.parseDouble(destArgs[1]), Double.parseDouble(destArgs[2]));
                    break;
                }
            } else {
                destination = null;
            }
        }
        if (destination == null) {
            String[] dArgs = SkillConfigManager.getUseSetting(hero, this, "destinations." + defaultDestinationString, "0,64,0").split(",");
            destination = new Location(plugin.getServer().getWorld(defaultDestinationString), Double.parseDouble(dArgs[0]), Double.parseDouble(dArgs[1]), Double.parseDouble(dArgs[2]));
        }

        try {
            broadcastExecuteText(hero);

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5F, 1.0F);

            player.teleport(destination);

            destination.getWorld().playSound(destination, Sound.ENTITY_WITHER_DEATH, 0.5F, 1.0F);

        } catch (Exception e) {
            player.sendMessage(ChatColor.GRAY + "SkillWarp has an invalid config.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        return SkillResult.NORMAL;
    }
}