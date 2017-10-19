package com.herocraftonline.heroes.characters.skill.unusedskills;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.SkillResult.ResultType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;

public class SkillGate extends ActiveSkill {

    public SkillGate(Heroes plugin) {
        super(plugin, "Gate");
        setDescription("You teleport yourself to the set location!");
        setUsage("/skill gate <location>");
        setArgumentRange(1, 1);
        setIdentifiers("skill gate");
        setTypes(SkillType.TELEPORTING, SkillType.SILENCEABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.NO_COMBAT_USE.node(), true);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        if (args[0].equalsIgnoreCase("list")) {
            for (String n : SkillConfigManager.getUseSettingKeys(hero, this, null)) {
                String retrievedNode = SkillConfigManager.getUseSetting(hero, this, n, (String) null);
                if (retrievedNode != null) {
                    Messaging.send(player, "$1 - $2", n, retrievedNode);
                }
            }
            return SkillResult.SKIP_POST_USAGE;
        }

        String gateInfo = SkillConfigManager.getUseSetting(hero, this, args[0].toLowerCase(), (String) null);
        if (gateInfo != null) {
            String[] splitArg = gateInfo.split(":");
            int levelRequirement = Integer.parseInt(splitArg[4]);
            World world = plugin.getServer().getWorld(splitArg[0]);
            if (world == null) {
                Messaging.send(player, "That teleport location no longer exists!");
                return SkillResult.INVALID_TARGET_NO_MSG;
            }

            if (hero.getHeroLevel(this) < levelRequirement) {
                return new SkillResult(ResultType.LOW_LEVEL, true, levelRequirement);
            }
            Location location = new Location(world, Double.parseDouble(splitArg[1]), Double.parseDouble(splitArg[2]), Double.parseDouble(splitArg[3]));
            player.teleport(location);
            broadcastExecuteText(hero);
            return SkillResult.NORMAL;
        } else {
            Messaging.send(player, "No gate location named $1", args[0]);
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
    }

    @Override
    public String getDescription(Hero hero) {
        // TODO Auto-generated method stub
        return null;
    }
}
