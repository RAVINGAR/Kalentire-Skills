package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.herocraftonline.dev.heroes.util.Util;

public class SkillTrack extends ActiveSkill {

    public SkillTrack(Heroes plugin) {
        super(plugin, "Track");
        setDescription("Locates a player");
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

}
