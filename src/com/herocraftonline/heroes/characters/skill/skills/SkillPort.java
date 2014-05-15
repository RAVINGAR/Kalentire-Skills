package com.herocraftonline.heroes.characters.skill.skills;

import java.util.ArrayList;
import java.util.List;

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
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;

public class SkillPort extends ActiveSkill {

	public SkillPort(Heroes plugin) {
		super(plugin, "Port");
		setDescription("You teleport yourself and party members within $1 blocks to the set location!");
		setUsage("/skill port <location>");
		setArgumentRange(1, 1);
		setIdentifiers("skill port");
        setTypes(SkillType.TELEPORTING, SkillType.AREA_OF_EFFECT, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE);
	}

	@Override
	public String getDescription(Hero hero) {
		int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 10, false);

		return getDescription().replace("$1", radius + "");
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.RADIUS.node(), Integer.valueOf(10));
        node.set(SkillSetting.NO_COMBAT_USE.node(), true);
        node.set(SkillSetting.DELAY.node(), Integer.valueOf(10000));

		return node;
	}

	@Override
	public SkillResult use(Hero hero, String[] args) {
		Player player = hero.getPlayer();

		List<String> keys = new ArrayList<String>(SkillConfigManager.getUseSettingKeys(hero, this, null));

		// Strip non-world keys
		for (SkillSetting setting : SkillSetting.values()) {
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
			boolean crossWorldEnabled = SkillConfigManager.getUseSetting(hero, this, "cross-world", false);
			if (world == null) {
				Messaging.send(player, "That teleport location no longer exists!");
				return SkillResult.INVALID_TARGET_NO_MSG;
			}
			else if (!world.equals(player.getWorld()) && !crossWorldEnabled) {
				Messaging.send(player, "You can't port to a location in another world!");
				return SkillResult.INVALID_TARGET_NO_MSG;
			}

			if (hero.getSkillLevel(this) < levelRequirement) {
				return new SkillResult(ResultType.LOW_LEVEL, true, levelRequirement);
			}

			broadcastExecuteText(hero);

            int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 10, false);
			int radiusSquared = radius * radius;
			Location loc = new Location(world, Double.parseDouble(splitArg[1]), Double.parseDouble(splitArg[2]), Double.parseDouble(splitArg[3]));

			if (!hero.hasParty()) {
				// Player doesn't have a party, just port him.
				player.teleport(loc);
				return SkillResult.NORMAL;
			}

			// Player has party. Port his party, if they are close enough.

			Location playerLocation = player.getLocation();
			for (Hero member : hero.getParty().getMembers()) {
				Player memberPlayer = member.getPlayer();

				if (loc.getWorld().equals(memberPlayer.getWorld())) {
					//Distance check the rest of the party
					if (memberPlayer.getLocation().distanceSquared(playerLocation) <= radiusSquared) {
						memberPlayer.teleport(loc);
					}
				}
			}
			hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.PORTAL_TRAVEL, 0.5F, 1.0F);
			return SkillResult.NORMAL;
		}
		else {
			Messaging.send(player, "No port location named $1", args[0]);
			return SkillResult.INVALID_TARGET_NO_MSG;
		}
	}
}