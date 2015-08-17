package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkillLastLocation extends ActiveSkill {

	private Map<UUID, Marker> activeMarkers = new HashMap<>();

	public SkillLastLocation(Heroes plugin) {
		super(plugin, "LastLocation");
		setDescription("Saves your current location allowing you to teleport back at any time over the course of $1 seconds. "
				+ "When you do, you are healed for an amount based on how long you waited, for a max of $2 at full duration.");
		setUsage("/skill LastLocation");
		setIdentifiers("skill LastLocation");
		setTypes(SkillType.HEALING, SkillType.SILENCEABLE, SkillType.TELEPORTING, SkillType.UNINTERRUPTIBLE);
		setArgumentRange(0, 0);
	}

	@Override
	public String getDescription(Hero hero) {
		return getDescription();
	}

	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.DURATION.node(), 4d);

		return node;
	}

	@Override
	public SkillResult use(Hero hero, String[] strings) {
		Player player = hero.getPlayer();
		Marker marker = activeMarkers.get(player.getUniqueId());
		if (marker != null) {
			marker.activate(1000);
		}
		else {
			double duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4d, false);
			marker = new Marker(hero, player.getLocation(), duration);
			activeMarkers.put(player.getUniqueId(), marker);
		}

		return SkillResult.NORMAL;
	}

	private class Marker extends BukkitRunnable {

		final Hero hero;
		final Location location;
		final double duration;
		final long startTime;

		Marker(Hero hero, Location location, double duration) {
			this.hero = hero;
			this.location = location;
			this.duration = duration;
			startTime = hero.getPlayer().getWorld().getFullTime();
			runTaskLater(plugin, (long) (duration * 20));
		}

		public void activate(double maxHealAmount) {
			cancel();
			run();
			hero.getPlayer().teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);

			double healScale = (hero.getPlayer().getWorld().getFullTime() - startTime) / duration;
			if (healScale > 1)
				healScale = 1;

			hero.heal(maxHealAmount * healScale);
		}

		@Override
		public void run() {
			activeMarkers.remove(hero.getPlayer().getUniqueId());
		}
	}
}
