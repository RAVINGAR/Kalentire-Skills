package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.EffectType;
import de.slikey.effectlib.effect.CircleEffect;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

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

		broadcastExecuteText(hero);

		if (marker != null) {
			marker.activate();
			player.getWorld().playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 0.5f, 0.1f);
			player.getWorld().playSound(marker.location, Sound.ENDERMAN_TELEPORT, 0.5f, 0.1f);
			player.getWorld().spigot().playEffect(player.getLocation(), Effect.SMOKE, 0, 0, 0.4f, 0.4f, 0.4f, 1, 16, 32);
		}
		else {
			double duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4d, false);
			marker = new Marker(hero, duration);
			activeMarkers.put(player.getUniqueId(), marker);
			player.getWorld().playSound(player.getLocation(), Sound.ORB_PICKUP, 1, 0.001f);
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

		final EffectManager em;
		final LastLocationEffect effect;

		Marker(Hero hero, double duration) {
			this.hero = hero;
			this.location = hero.getPlayer().getLocation();
			this.duration = (long) (duration * 20);
			startTime = hero.getPlayer().getWorld().getFullTime();
			runTaskLater(plugin, this.duration);

			em = new EffectManager(plugin);
			effect = new LastLocationEffect(em);

			effect.setLocation(location.clone().add(0, 0.1, 0));
			effect.radius = 0.4f;
			//effect.enableRotation = false;
			effect.asynchronous = true;

			effect.particle = ParticleEffect.REDSTONE;
			effect.color = Color.WHITE;
			effect.particles = 16;
			effect.infinite();

			effect.start();
			em.disposeOnTermination();
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
				double healScale = (double) (hero.getPlayer().getWorld().getFullTime() - startTime) / duration;
				if (healScale > 1) {
					healScale = 1;
				}
				healAmount *= healScale;
			}

			hero.heal(healAmount);
			em.done(effect);
		}
	}

	public class LastLocationEffect extends de.slikey.effectlib.Effect {

		public double radius;

		public ParticleEffect particle;

		public double particles;

		public LastLocationEffect(EffectManager effectManager) {
			super(effectManager);
		}

		@Override
		public void onRun() {
			double inc = Math.PI * 2 / particles;
			for (double angle = 0; angle <= 360; angle += inc) {
				Vector v = new Vector(Math.cos(angle), 0, Math.sin(angle)).multiply(radius);
				display(particle, getLocation().clone().add(v));
			}
		}
	}
 }
