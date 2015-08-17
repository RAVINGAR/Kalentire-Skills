package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkillLastLocation extends ActiveSkill {

	private static final String HEAL_PERCENTAGE_NODE = "heal-percentage";

	private Map<UUID, Marker> activeMarkers = new HashMap<>();

	public SkillLastLocation(Heroes plugin) {
		super(plugin, "LastLocation");
		setDescription("Saves your current location allowing you to use the skill again to teleport back at any time over the course of $1 seconds. "
				+ "When you do, you are healed for up to $2% your max health ($3) scaled by how long you wait to teleport. "
				+ "If you choose not to teleport, you are healed for the full amount of $3 at the end of the duration");
		setUsage("/skill LastLocation");
		setIdentifiers("skill LastLocation");
		setTypes(SkillType.HEALING, SkillType.SILENCEABLE, SkillType.TELEPORTING, SkillType.UNINTERRUPTIBLE);
		setArgumentRange(0, 0);
	}

	@Override
	public String getDescription(Hero hero) {
		double duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4d, false);
		double healPercentage  = SkillConfigManager.getUseSetting(hero, this, HEAL_PERCENTAGE_NODE, 0.25d, false);
		double maxHealAmount = hero.getPlayer().getMaxHealth() * healPercentage;

		return getDescription().replace("$1", duration + "").replace("$2", healPercentage + "").replace("$3", maxHealAmount + "");
	}

	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.DURATION.node(), 4d);
		node.set(HEAL_PERCENTAGE_NODE, 0.25d);

		return node;
	}

	@Override
	public SkillResult use(Hero hero, String[] strings) {
		Player player = hero.getPlayer();
		Marker marker = activeMarkers.get(player.getUniqueId());

		if (marker != null) {
			marker.activate();
		}
		else {
			double duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4d, false);
			marker = new Marker(hero, player.getLocation(), duration);
			activeMarkers.put(player.getUniqueId(), marker);
		}

		return SkillResult.NORMAL;
	}

	private double getMaxHealAmount(Hero hero) {
		double healPercentage = SkillConfigManager.getUseSetting(hero, this, HEAL_PERCENTAGE_NODE, 0.25d, false);
		return hero.getPlayer().getMaxHealth() * healPercentage;
	}

	private class Marker extends BukkitRunnable {

		final Hero hero;
		final Location location;
		final long duration;
		final long startTime;

		Marker(Hero hero, Location location, double duration) {
			this.hero = hero;
			this.location = location;
			this.duration = (long) (duration * 20);
			startTime = hero.getPlayer().getWorld().getFullTime();
			runTaskLater(plugin, this.duration);
		}

		public void activate() {
			cancel();
			Player player = hero.getPlayer();
			player.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
			run(true);
		}

		@Override
		public void run() {
			run(false);
		}

		private void run(boolean teleported) {
			activeMarkers.remove(hero.getPlayer().getUniqueId());
			double healAmount = getMaxHealAmount(hero);

			if (teleported) {
				double healScale = (hero.getPlayer().getWorld().getFullTime() - startTime) / duration;
				if (healScale > 1) {
					healScale = 1;
				}
				healAmount *= healScale;
			}

			hero.heal(healAmount);
		}
	}
}
