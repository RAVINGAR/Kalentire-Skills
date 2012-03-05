package com.herocraftonline.heroes.skill.skills;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.hero.Hero;
import com.herocraftonline.heroes.skill.ActiveSkill;
import com.herocraftonline.heroes.skill.SkillConfigManager;
import com.herocraftonline.heroes.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillTrack extends ActiveSkill {

    public SkillTrack(Heroes plugin) {
        super(plugin, "Track");
        setDescription("You are able to track down another player's location.");
        setUsage("/skill track <player>");
        setArgumentRange(1, 1);
        setIdentifiers("skill track");
        setTypes(SkillType.EARTH, SkillType.KNOWLEDGE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("randomness", 50);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null)
        	return SkillResult.INVALID_TARGET;

        Location location = target.getLocation();
        int randomness = SkillConfigManager.getUseSetting(hero, this, "randomness", 50, true);
        int x = location.getBlockX() + Util.rand.nextInt(randomness);
        int y = location.getBlockY() + Util.rand.nextInt(randomness / 10);
        int z = location.getBlockZ() + Util.rand.nextInt(randomness);

        Messaging.send(player, "Tracked $1: $2,$3,$4", target.getName(), x, y, z);
        player.setCompassTarget(location);
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

}
