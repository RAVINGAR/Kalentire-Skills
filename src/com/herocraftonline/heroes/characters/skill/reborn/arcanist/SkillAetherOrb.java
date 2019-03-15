package com.herocraftonline.heroes.characters.skill.reborn.arcanist;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.tools.BasicMissile;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.EffectType;
import de.slikey.effectlib.effect.LineEffect;
import de.slikey.effectlib.effect.SphereEffect;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SkillAetherOrb extends ActiveSkill {

	private static Color blueViolet = Color.fromRGB(138,43,226);
	private NMSHandler nmsHandler = NMSHandler.getInterface();

	public SkillAetherOrb(Heroes plugin) {
		super(plugin, "AetherOrb");
		setDescription("Call upon the forces of the aether and launch it in front of you. "
                + "Upon landing, an orb of Aether is formed at the location, dealing $1 damage every $2 second(s) for up to $3 second(s). "
                + "The orb can only hit up to $5 targets within $4 blocks every time it pulses.");
		setUsage("/skill aetherorb");
		setIdentifiers("skill aetherorb");
		setArgumentRange(0, 0);
		setTypes(SkillType.AREA_OF_EFFECT, SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_MAGICAL);

		setToggleableEffectName(this.getName());
	}

	public String getDescription(Hero hero) {
		final int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 4, false);
		long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);
		final long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 200, false);
		final double damageTick = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 50d, false);
        int maxTargets = SkillConfigManager.getUseSetting(hero, this, "max-targets-per-pulse", 4, false);

		return getDescription()
				.replace("$1", Util.decFormat.format(damageTick))
				.replace("$2", Util.decFormat.format((double) period / 1000))
				.replace("$3", Util.decFormat.format((double) duration / 1000))
				.replace("$4", Util.decFormat.format(radius))
                .replace("$5", maxTargets + "");
	}

	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();
		node.set(SkillSetting.RADIUS.node(), 6);
		node.set("height", 4);
		node.set(SkillSetting.DURATION.node(), 6000);
		node.set(SkillSetting.PERIOD.node(), 500);
		node.set(SkillSetting.DAMAGE_TICK.node(), 50d);
		node.set("max-targets-per-pulse", 4);
		node.set("projectile-velocity", 20.0);
		node.set("visual-orb-radius", 1.0);
		return node;
	}

	public SkillResult use(final Hero hero, String[] args) {
		final Player player = hero.getPlayer();

		double projVelocity = SkillConfigManager.getUseSetting(hero, this, "projectile-velocity", 20.0, false);
		AetherOrbMissile missile = new AetherOrbMissile(plugin, this, hero, 0.2, projVelocity);
		missile.fireMissile();

		broadcastExecuteText(hero);

		return SkillResult.NORMAL;
	}

	private class AetherOrbMissile extends BasicMissile {

		public AetherOrbMissile(Plugin plugin, Skill skill, Hero hero, double projectileSize, double projVelocity) {
			super(plugin, skill, hero, projectileSize, Particle.SPELL_WITCH, projVelocity);
		}

		@Override
		protected void onBlockHit(Block block, Vector hitPoint, BlockFace hitFace, Vector hitForce) {
			explodeIntoGroundEffect(block.getRelative(hitFace).getLocation());
		}

		@Override
		protected void onEntityHit(Entity entity, Vector hitOrigin, Vector hitForce) {
			explodeIntoGroundEffect(entity.getLocation());
		}

		private void explodeIntoGroundEffect(Location location) {
            long period = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.PERIOD, 500, false);
            long duration = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DURATION, 6000, false);

			AetherOrbEffect orbEffect = new AetherOrbEffect(skill, player, hero, period, duration, location.clone());
			hero.addEffect(orbEffect);
		}
	}

	private class AetherOrbEffect extends PeriodicExpirableEffect {
        private final Location orbLocation;
		private final int height;

        private EffectManager effectManager;

		private double damageTick;
        private int maxTargetsPerPulse;
        private int radius;
		private double visualOrbRadius;

        AetherOrbEffect(Skill skill, Player player, Hero hero, long period, long duration, Location location) {
		    super(skill, "ActiveAetherOrb", player, period, duration, null, null);

            this.height = SkillConfigManager.getUseSetting(hero, skill, "height", 4, false);
		    this.orbLocation = location.add(new Vector(0, height / 2, 0));
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            this.effectManager = new EffectManager(plugin);
            this.radius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS, 6, false);
            this.damageTick = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_TICK, 50d, false);
            this.maxTargetsPerPulse = SkillConfigManager.getUseSetting(hero, skill, "max-targets-per-pulse", 4, false);
            this.visualOrbRadius = SkillConfigManager.getUseSetting(hero, skill, "visual-orb-radius", 1.0, false);

            SphereEffect visualEffect = new SphereEffect(effectManager);
            visualEffect.particle = Particle.SPELL_WITCH;
            visualEffect.color = blueViolet;	// Don't think this works for this particle type but no biggy
            visualEffect.radius = visualOrbRadius;

            visualEffect.type = EffectType.INSTANT;
            visualEffect.period = 1;
            visualEffect.iterations = (int) (getDuration() / 50);
            visualEffect.asynchronous = true;
            visualEffect.setLocation(orbLocation);

            visualEffect.start();
            orbLocation.getWorld().playSound(orbLocation, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.15f, 0.0001f);
        }

        @Override
        public void tickMonster(Monster monster) { }

        @Override
		public void tickHero(Hero hero) {
            int currentHitCount = 0;
            for (Entity entity : getEntitiesInChunks(orbLocation, radius)) {
                if (currentHitCount >= maxTargetsPerPulse)
                    break;

                if (!(entity instanceof LivingEntity))
                    continue;

                Location targetLocation = entity.getLocation();
                double targetY = targetLocation.getY();
                if (!(targetLocation.distanceSquared(targetLocation) <= radius * radius) || !(targetY <= targetLocation.getY() + height) || !(targetY >= targetLocation.getY() - height))
                    continue;

                LivingEntity target = (LivingEntity) entity;
                if (!damageCheck(applier, target))
                    return;

                addSpellTarget(target, hero);
                damageEntity(target, applier, damageTick, EntityDamageEvent.DamageCause.MAGIC, false);

                LineEffect lineVisual = new LineEffect(effectManager);
                lineVisual.particle = Particle.REDSTONE;
                lineVisual.color = blueViolet;

                lineVisual.setLocation(orbLocation);
                lineVisual.setTargetEntity(target);

                currentHitCount++;

                lineVisual.start();
                orbLocation.getWorld().playSound(orbLocation, Sound.BLOCK_GLASS_BREAK, 0.3f, 2.0F);
            }
		}

        private Set<Entity> getEntitiesInChunks(Location location, int radius) {
            Set<Entity> entities = new HashSet<Entity>();

            int chunkRadius = (int) (radius + 16) / 16;
            Chunk origin = location.getChunk();
            for (int x = -chunkRadius; x <= chunkRadius; x++) {
                for (int z = -chunkRadius; z <= chunkRadius; z++) {
                    Collections.addAll(entities, origin.getWorld().getChunkAt(origin.getX() + x, origin.getZ() + z).getEntities());
                }
            }
            return entities;
        }
	}
}
