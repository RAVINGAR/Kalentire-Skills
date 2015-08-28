package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.ManaChangeEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import de.slikey.effectlib.Effect;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class SkillBaseMarkedTeleport extends TargettedSkill {

	protected static final String MANA_ACTIVATION_NODE = "mana-activation";

	protected static final String PRESERVE_LOOK_DIRECTION_NODE = "preserve-look-direction";
	protected static final String PRESERVE_VELOCITY_NODE = "preserve-velocity";

	private final Map<UUID, Marker> activeMarkers = new HashMap<>();

	private final EffectType[] markerEffectTypes;
	private final ParticleEffect particle;
	private final Color[] colors;

	public SkillBaseMarkedTeleport(Heroes plugin, String name,
	                               EffectType[] markerEffectTypes, ParticleEffect particle, Color[] colors) {
		super(plugin, name);
		this.markerEffectTypes = markerEffectTypes;
		this.particle = particle;
		this.colors = colors;
	}

	public SkillBaseMarkedTeleport(Heroes plugin, String name, EffectType[] markerEffectTypes, ParticleEffect particle) {
		this(plugin, name, markerEffectTypes, particle, new Color[0]);
	}

	public SkillBaseMarkedTeleport(Heroes plugin, String name, ParticleEffect particle) {
		this(plugin, name, new EffectType[0], particle, new Color[0]);
	}

	public SkillBaseMarkedTeleport(Heroes plugin, String name, ParticleEffect particle, Color[] colors) {
		this(plugin, name, new EffectType[0], particle, colors);
	}

	@Override
	public final SkillResult use(Hero hero, String[] args) {
		Marker marker = activeMarkers.get(hero.getPlayer().getUniqueId());
		if (marker != null) {
			marker.activate();
			return SkillResult.INVALID_TARGET_NO_MSG;
		} else {
			return super.use(hero, args);
		}
	}

	@Override
	public final SkillResult use(Hero hero, LivingEntity target, String[] strings) {
		Player player = hero.getPlayer();

		broadcastExecuteText(hero, target);

		int manaCost = SkillConfigManager.getUseSetting(hero, this, MANA_ACTIVATION_NODE, 0, false);

		if (manaCost <= hero.getMana()) {

			ManaChangeEvent event = new ManaChangeEvent(hero, hero.getMana(), hero.getMana() - manaCost);
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled()) {
				hero.setMana(event.getFinalMana());
			}

			CharacterTemplate targetCt = plugin.getCharacterManager().getCharacter(target);

			MarkedTeleportEffect effect = new MarkedTeleportEffect(player,
					SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false),
					SkillConfigManager.getUseSetting(hero, this, PRESERVE_LOOK_DIRECTION_NODE, true),
					SkillConfigManager.getUseSetting(hero, this, PRESERVE_VELOCITY_NODE, true));

			Collections.addAll(effect.types, markerEffectTypes);

			targetCt.addEffect(effect);
		} else {
			return SkillResult.LOW_MANA;
		}

		return SkillResult.INVALID_TARGET_NO_MSG;
	}

	protected void onMarkerCreate(Marker marker) { }
	protected void onMarkerRemove(Marker marker) { }
	protected void onMarkerActivate(Marker marker, long activateTime) { }

	private void applyCooldown(Hero hero) {
		hero.setCooldown(getName(), System.currentTimeMillis() + SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 0, false));
	}

	private final class MarkedTeleportEffect extends ExpirableEffect {

		private final boolean preserveLookDirection;
		private final boolean preserveVelocity;

		public MarkedTeleportEffect(Player applier, long duration, boolean preserveLookDirection, boolean preserveVelocity) {
			super(SkillBaseMarkedTeleport.this, SkillBaseMarkedTeleport.this.getName(), applier, duration);
			this.preserveLookDirection = preserveLookDirection;
			this.preserveVelocity = preserveVelocity;

			types.add(EffectType.DISPELLABLE);
			types.add(EffectType.MAGIC);
			types.add(EffectType.SUMMON);
		}

		@Override
		public void applyToHero(Hero hero) {
			super.applyToHero(hero);
			createMarker(hero);
		}

		@Override
		public void applyToMonster(Monster monster) {
			super.applyToMonster(monster);
			createMarker(monster);
		}

		@Override
		public void removeFromHero(Hero hero) {
			super.removeFromHero(hero);
			removeMarker();
		}

		@Override
		public void removeFromMonster(Monster monster) {
			super.removeFromMonster(monster);
			removeMarker();
		}

		private void createMarker(CharacterTemplate target) {
			Marker marker = new Marker(plugin.getCharacterManager().getHero(getApplier()), target, preserveVelocity, preserveLookDirection);
			activeMarkers.put(getApplier().getUniqueId(), marker);

			onMarkerCreate(marker);
		}

		private void removeMarker() {
			Marker marker = activeMarkers.remove(getApplier().getUniqueId());
			applyCooldown(marker.hero);
			marker.effect.cancel();

			onMarkerRemove(marker);
		}
	}

	protected final class Marker {

		private final Hero hero;
		private final CharacterTemplate target;
		private final Location location;

		private final boolean preserveVelocity;
		private final boolean preserveLookDirection;

		private final RingEffect effect;

		private final long createTime = System.currentTimeMillis();
		private boolean activated = false;

		private Marker(Hero hero, CharacterTemplate target, boolean preserveVelocity, boolean preserveLookDirection) {
			this.hero = hero;
			this.target = target;
			this.location = target.getEntity().getLocation();

			this.preserveVelocity = preserveVelocity;
			this.preserveLookDirection = preserveLookDirection;

			EffectManager em = new EffectManager(plugin);
			effect = new RingEffect(em);

			effect.setLocation(location.clone().add(0, 0.1, 0));

			effect.start();
			em.disposeOnTermination();
		}

		public Hero getHero() {
			return hero;
		}

		public CharacterTemplate getTarget() {
			return target;
		}

		public Location getLocation() {
			return location.clone();
		}

		public boolean isVelocityPreserved() {
			return preserveVelocity;
		}

		public boolean isLookDirectionPreserved() {
			return preserveLookDirection;
		}

		public long getCreateTime() {
			return createTime;
		}

		public boolean isActivated() {
			return activated;
		}

		private void activate() {
			if (!isActivated()) {
				Vector currentVelocity = target.getEntity().getVelocity();

				if (preserveLookDirection) {
					location.setDirection(target.getEntity().getLocation().getDirection());
				}

				target.getEntity().teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);

				if (preserveVelocity) {
					target.getEntity().setVelocity(currentVelocity);
				}

				activated = true;
				target.removeEffect(target.getEffect(getName()));

				onMarkerActivate(this, System.currentTimeMillis());
			}
		}

		private final class RingEffect extends Effect {

			public double radius = 0.5;
			public double particles = 16;

			private int colorIndex = 0;

			public RingEffect(EffectManager effectManager) {
				super(effectManager);
				infinite();
				asynchronous = true;
				color = Color.WHITE;
			}

			@Override
			public void onRun() {
				double inc = Math.PI * 2 / particles;

				for (double angle = 0; angle <= 360; angle += inc) {
					if (colors.length > 0) {
						if (colorIndex >= colors.length) {
							colorIndex = 0;
						}

						color = colors[colorIndex++];
					}

					Vector v = new Vector(Math.cos(angle), 0, Math.sin(angle)).multiply(radius);
					display(particle, getLocation().clone().add(v));
				}
			}
		}
	}
}
