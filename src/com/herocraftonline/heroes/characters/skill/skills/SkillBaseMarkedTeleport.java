package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.ManaChangeEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import de.slikey.effectlib.Effect;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class SkillBaseMarkedTeleport extends TargettedSkill {

	private final Map<UUID, TeleportMarker> activeMarkers = new HashMap<>();

	public SkillBaseMarkedTeleport(Heroes plugin, String name) {
		super(plugin, name);
	}

	@Override
	public final SkillResult use(Hero hero, String[] strings) {
		Player player = hero.getPlayer();
		TeleportMarker teleportMarker = activeMarkers.get(player.getUniqueId());

		if (teleportMarker != null) {
			hero.setCooldown(getName(), System.currentTimeMillis() + getAppliedCooldown(hero));

			return SkillResult.NORMAL;
		} else {
			int manaCost = getManaCost(hero);
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

	protected long getAppliedCooldown(Hero hero) { return 0; }
	protected int getManaCost(Hero hero) { return 0; }

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
			disableMarker(hero);
		}

		@Override
		public void removeFromMonster(Monster monster) {
			super.removeFromMonster(monster);
			disableMarker(monster);
		}

		private void enableMarker(CharacterTemplate target) {
			TeleportMarker marker = new TeleportMarker(getApplier(), target, target.getEntity().getLocation(), preserveVelocity, preserveLookDirection, particle, color);
			activeMarkers.put(target.getEntity().getUniqueId(), marker);
		}

		private void disableMarker(CharacterTemplate target) {
			TeleportMarker marker = activeMarkers.remove(target.getEntity().getUniqueId());
			marker.effect.cancel();
		}
	}

	private final class TeleportMarker {

		private final Player caster;
		private final CharacterTemplate target;
		private final Location location;

		private final boolean preserveVelocity;
		private final boolean preserveLookDirection;

		private final RingEffect effect;

		public TeleportMarker(Player caster, CharacterTemplate target, Location location, boolean preserveVelocity, boolean preserveLookDirection, ParticleEffect particle, Color color) {
			this.caster = caster;
			this.target = target;
			this.location = location;

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
