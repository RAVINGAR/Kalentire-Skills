package com.herocraftonline.heroes.characters.skill.skills;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.VisualEffect;

public class SkillFirestorm extends ActiveSkill {

	public VisualEffect fplayer = new VisualEffect();		// Firework effect

	public SkillFirestorm(Heroes plugin) {
		super(plugin, "Firestorm");
		setIdentifiers("skill firestorm");
		setUsage("/skill firestorm");
		setArgumentRange(0, 0);
		setDescription("Summons a fire stormt in the area around your feet, dealing $1 damage to all targets within a $2 block radius.");
	}

	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.DAMAGE.node(), 300);
		node.set(SkillSetting.RADIUS.node(), 10);
		node.set(SkillSetting.DELAY.node(), 5000);
		node.set(SkillSetting.USE_TEXT.node(), "§7[§2Skill§7] %hero% has unleashed a powerful §lTempest!");
		node.set("effect-height", 4);

		return node;
	}

	@Override
	public String getDescription(Hero hero) {

		int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 300, false);
		int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 10, false);

		return getDescription().replace("$1", damage + "").replace("$1", radius + "");
	}

	@Override
	public SkillResult use(final Hero hero, String[] args) {

		final Player player = hero.getPlayer();

		// Get config settings
		final double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 300, false);
		final int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 10, false);
		final int height = SkillConfigManager.getUseSetting(hero, this, "effect-height", 5, false);

		broadcastExecuteText(hero);

		// Create a cicle of firework locations, based on skill radius.
		List<Location> fireworkLocations = circle(player, player.getLocation(), radius, 1, true, false, height);
		int fireworksSize = fireworkLocations.size();
		long ticksPerFirework = (int) (100.00 / ((double) fireworksSize));

		// Play the firework effects in a sequence
		for (int i = 0; i < fireworksSize; i++) {
			final Location fLoc = fireworkLocations.get(i);
			Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
				@Override
				public void run() {
					try {
						fplayer.playFirework(fLoc.getWorld(), fLoc, FireworkEffect.builder().flicker(false).trail(false).withColor(Color.RED).with(Type.BALL).build());
					}
					catch (IllegalArgumentException e) {
						e.printStackTrace();
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}

			}, ticksPerFirework * i);
		}

		// Save player location for the center of the blast
		final Location centerLocation = player.getLocation();

		// Damage all entities near the center after the fireworks finish playing
		Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
			@Override
			public void run() {
				for (Entity entity : getNearbyEntities(centerLocation, radius, radius, radius)) {
					// Check to see if the entity can be damaged
					if (!(entity instanceof LivingEntity) || !damageCheck(player, (LivingEntity) entity))
						continue;

					// Damage the target
					addSpellTarget((LivingEntity) entity, hero);
					damageEntity((LivingEntity) entity, player, damage, DamageCause.MAGIC);
					//player.getWorld().strikeLightningEffect(entity.getLocation());
				}
			}

		}, ticksPerFirework * fireworksSize);

		// Finish
		return SkillResult.NORMAL;
	}

	protected List<Entity> getNearbyEntities(Location targetLocation, int radiusX, int radiusY, int radiusZ) {
		List<Entity> entities = new ArrayList<Entity>();

		for (Entity entity : targetLocation.getWorld().getEntities()) {
			if (isInBorder(targetLocation, entity.getLocation(), radiusX, radiusY, radiusZ)) {
				entities.add(entity);
			}
		}
		return entities;
	}

	public boolean isInBorder(Location center, Location targetLocation, int radiusX, int radiusY, int radiusZ) {
		int x1 = center.getBlockX();
		int y1 = center.getBlockY();
		int z1 = center.getBlockZ();

		int x2 = targetLocation.getBlockX();
		int y2 = targetLocation.getBlockY();
		int z2 = targetLocation.getBlockZ();

		if (x2 >= (x1 + radiusX) || x2 <= (x1 - radiusX) || y2 >= (y1 + radiusY) || y2 <= (y1 - radiusY) || z2 >= (z1 + radiusZ) || z2 <= (z1 - radiusZ))
			return false;

		return true;
	}

	protected List<Location> circle(Player player, Location loc, Integer r, Integer h, boolean hollow, boolean sphere, int plus_y) {
		List<Location> circleblocks = new ArrayList<Location>();
		int cx = loc.getBlockX();
		int cy = loc.getBlockY();
		int cz = loc.getBlockZ();
		for (int x = cx - r; x <= cx + r; x++)
			for (int z = cz - r; z <= cz + r; z++)
				for (int y = (sphere ? cy - r : cy); y < (sphere ? cy + r : cy + h); y++) {
					double dist = (cx - x) * (cx - x) + (cz - z) * (cz - z) + (sphere ? (cy - y) * (cy - y) : 0);
					if (dist < r * r && !(hollow && dist < (r - 1) * (r - 1))) {
						Location l = new Location(loc.getWorld(), x, y + plus_y, z);
						circleblocks.add(l);
					}
				}

		return circleblocks;
	}
}