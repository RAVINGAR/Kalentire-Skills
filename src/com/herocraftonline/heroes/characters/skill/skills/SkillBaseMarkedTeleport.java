package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.ManaChangeEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import de.slikey.effectlib.Effect;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class SkillBaseMarkedTeleport extends TargettedSkill {

	protected static final String MANA_ACTIVATION_NODE = "mana-activation";

	protected static final String PRESERVE_VELOCITY_NODE = "preserve-velocity";
	protected static final String PRESERVE_LOOK_DIRECTION_NODE = "preserve-look-direction";

	private final Map<UUID, Marker> activeMarkers = new HashMap<>();

	public SkillBaseMarkedTeleport(Heroes plugin, String name) {
		super(plugin, name);
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.MAX_DISTANCE.node(), 8d);
		node.set(SkillSetting.DURATION.node(), 6000);

		return node;
	}

	@Override
	public SkillResult use(Hero hero, LivingEntity target, String[] strings) {
		Player player = hero.getPlayer();
		Marker marker = activeMarkers.get(player.getUniqueId());

		if (marker != null) {
			// Test if returning SkillResult.NORMAL does the cooldown stuff
			//hero.setCooldown(getName(), System.currentTimeMillis() + SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 10000, false));

			marker.activate();

			return SkillResult.NORMAL;
		} else {
			int manaCost = SkillConfigManager.getUseSetting(hero, this, MANA_ACTIVATION_NODE, 0, false);

			if (manaCost <= hero.getMana()) {

				ManaChangeEvent event = new ManaChangeEvent(hero, hero.getMana(), hero.getMana() - manaCost);
				Bukkit.getPluginManager().callEvent(event);
				if (!event.isCancelled()) {
					hero.setMana(event.getFinalMana());
				}



			} else {
				return SkillResult.LOW_MANA;
			}

			return SkillResult.INVALID_TARGET_NO_MSG;
		}
	}

	public class MarkedTeleportEffect extends ExpirableEffect {

		private final ParticleEffect particle;
		private final Color color;
		private final boolean preserveVelocity;
		private final boolean preserveLookDirection;

		public MarkedTeleportEffect(Player applier, long duration, boolean preserveVelocity, boolean preserveLookDirection, ParticleEffect particle, Color color) {
			super(SkillBaseMarkedTeleport.this, SkillBaseMarkedTeleport.this.getName(), applier, duration);
			this.preserveVelocity = preserveVelocity;
			this.preserveLookDirection = preserveLookDirection;
			this.particle = particle;
			this.color = color;
		}

		public MarkedTeleportEffect(Player applier, long duration, boolean preserveVelocity, boolean preserveLookDirection, ParticleEffect particle) {
			this(applier, duration, preserveVelocity, preserveLookDirection, particle, Color.WHITE);
		}

		public MarkedTeleportEffect(Player applier, long duration, boolean preserveVelocity, boolean preserveLookDirection) {
			this(applier, duration, preserveVelocity, preserveLookDirection, ParticleEffect.REDSTONE);
		}

		@Override
		public void applyToHero(Hero hero) {
			super.removeFromHero(hero);
			enableMarker(hero);
		}

		@Override
		public void applyToMonster(Monster monster) {
			super.applyToMonster(monster);
			enableMarker(monster);
		}

		@Override
		public void removeFromHero(Hero hero) {
			super.removeFromHero(hero);
			disableMarker();
		}

		@Override
		public void removeFromMonster(Monster monster) {
			super.removeFromMonster(monster);
			disableMarker();
		}

		private void enableMarker(CharacterTemplate target) {
			Marker marker = new Marker(target, preserveVelocity, preserveLookDirection, particle, color);
			activeMarkers.put(getApplier().getUniqueId(), marker);
		}

		private void disableMarker() {
			Marker marker = activeMarkers.remove(getApplier().getUniqueId());
			marker.effect.cancel();
		}
	}

	private final class Marker {

		private final CharacterTemplate target;
		private final Location location;

		private final boolean preserveVelocity;
		private final boolean preserveLookDirection;

		private final RingEffect effect;

		public Marker(CharacterTemplate target, boolean preserveVelocity, boolean preserveLookDirection, ParticleEffect particle, Color color) {
			this.target = target;
			this.location = target.getEntity().getLocation();

			this.preserveVelocity = preserveVelocity;
			this.preserveLookDirection = preserveLookDirection;

			EffectManager em = new EffectManager(plugin);
			effect = new RingEffect(em);

			effect.setLocation(location.clone().add(0, 0.1, 0));
			effect.particle = particle;
			effect.color = color;

			effect.start();
			em.disposeOnTermination();
		}

		public void activate() {
			Vector currentVelocity = target.getEntity().getVelocity();

			if (preserveLookDirection) {
				location.setDirection(target.getEntity().getLocation().getDirection());
			}

			target.getEntity().teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);

			if (preserveVelocity) {
				target.getEntity().setVelocity(currentVelocity);
			}

			target.removeEffect(target.getEffect(getName()));
		}

		private class RingEffect extends Effect {

			public double radius = 0.5;
			public ParticleEffect particle = ParticleEffect.FLAME;
			public double particles = 16;

			public RingEffect(EffectManager effectManager) {
				super(effectManager);
				infinite();
				asynchronous = true;
			}

			@Override
			public void onRun() {

			}
		}
	}
}
