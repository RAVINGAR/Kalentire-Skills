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

public class SkillAetherOrb extends ActiveSkill {

	private static Color blueViolet = Color.fromRGB(138,43,226);
	private NMSHandler nmsHandler = NMSHandler.getInterface();

	public SkillAetherOrb(Heroes plugin) {
		super(plugin, "AetherOrb");
		setDescription("Call upon the forces of the aether and launch it forward in front of you. "
                + "Upon landing, an orb of Aether is formed at the location, dealing $1 damage every $2 second(s) for up to $3 second(s). "
                + "The orb can only hit up to $4 targets within $5 blocks every time it pulses.");
		setUsage("/skill aetherorb");
		setIdentifiers("skill aetherorb");
		setArgumentRange(0, 0);
		setTypes(SkillType.AREA_OF_EFFECT, SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_MAGICAL);
	}

	public String getDescription(Hero hero) {
		final double radius = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.RADIUS, false);
		long duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
		final long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 200, false);
		final double damageTick = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE_TICK, false);
        int maxTargets = SkillConfigManager.getUseSetting(hero, this, "max-targets-per-pulse", 4, false);

		return getDescription()
				.replace("$1", Util.decFormat.format(damageTick))
				.replace("$2", Util.decFormat.format((double) period / 1000))
				.replace("$3", Util.decFormat.format((double) duration / 1000))
                .replace("$4", maxTargets + "")
                .replace("$5", Util.decFormat.format(radius));
	}

	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection config = super.getDefaultConfig();
		config.set(SkillSetting.RADIUS.node(), 5.0);
		config.set("height", 4.0);
		config.set(SkillSetting.PERIOD.node(), 500);
		config.set(SkillSetting.DURATION.node(), 6000);
		config.set(SkillSetting.DAMAGE_TICK.node(), 50d);
		config.set(BasicMissile.PROJECTILE_VELOCITY_NODE, 20.0);
        config.set(BasicMissile.PROJECTILE_PIERCES_ON_HIT_NODE, true);
        config.set(BasicMissile.PROJECTILE_SIZE_NODE, 0.2);
        config.set(BasicMissile.PROJECTILE_GRAVITY_NODE, 12.25375);
        config.set(BasicMissile.PROJECTILE_DURATION_TICKS_NODE, 999999);
		config.set("max-targets-per-pulse", 4);
		config.set("visual-orb-radius", 1.0);
		return config;
	}

	public SkillResult use(final Hero hero, String[] args) {
		final Player player = hero.getPlayer();

		broadcastExecuteText(hero);
		AetherOrbMissile missile = new AetherOrbMissile(plugin, this, hero);
		missile.fireMissile();

		return SkillResult.NORMAL;
	}

	private class AetherOrbMissile extends BasicMissile {
		public AetherOrbMissile(Heroes plugin, Skill skill, Hero hero) {
			super(plugin, skill, hero, Particle.SPELL_WITCH);
		}

        @Override
		protected void onBlockHit(Block block, Vector hitPoint, BlockFace hitFace, Vector hitForce) {
			explodeIntoGroundEffect(block.getRelative(hitFace).getLocation());
		}

        @Override
        protected void onValidTargetFound(LivingEntity target, Vector origin, Vector passForce) {
            explodeIntoGroundEffect(target.getLocation());
        }

        private void explodeIntoGroundEffect(Location location) {
            long period = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.PERIOD, 500, false);
            long duration = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DURATION, 6000, false);

			AetherOrbEffect orbEffect = new AetherOrbEffect(skill, player, hero, period, duration, location.clone());
			hero.addEffect(orbEffect);
		}
	}

	private class AetherOrbEffect extends PeriodicExpirableEffect {
        private EffectManager effectManager;
        private final Location orbLocation;
		private double orbVisualRadius;
		private final double height;
		private double damageTick;
        private int maxTargetsPerPulse;
        private double radius;

        AetherOrbEffect(Skill skill, Player player, Hero hero, long period, long duration, Location location) {
		    super(skill, "ActiveAetherOrb", player, period, duration, null, null);

            this.height = SkillConfigManager.getUseSetting(hero, skill, "height", 4.0, false);
		    this.orbLocation = location.add(new Vector(0, height / 2.0, 0));
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            this.effectManager = new EffectManager(plugin);
            this.radius = SkillConfigManager.getScaledUseSettingDouble(hero, skill, SkillSetting.RADIUS, false);
            this.damageTick = SkillConfigManager.getScaledUseSettingDouble(hero, skill, SkillSetting.DAMAGE_TICK, false);
            this.maxTargetsPerPulse = SkillConfigManager.getUseSetting(hero, skill, "max-targets-per-pulse", 4, false);
            this.orbVisualRadius = SkillConfigManager.getUseSetting(hero, skill, "visual-orb-radius", 1.0, false);

            SphereEffect orbVisual = new SphereEffect(effectManager);
            orbVisual.setLocation(orbLocation);
            orbVisual.particle = Particle.SPELL_WITCH;
            orbVisual.particles = 25;
            orbVisual.radius = orbVisualRadius;
            orbVisual.type = EffectType.REPEATING;
            orbVisual.period = 1;
            orbVisual.iterations = (int) (getDuration() / 50);
            orbVisual.asynchronous = true;

            effectManager.start(orbVisual);
            orbLocation.getWorld().playSound(orbLocation, Sound.ENTITY_ILLUSION_ILLAGER_CAST_SPELL, 0.15f, 0.0001f);
        }

        @Override
        public void tickMonster(Monster monster) { }

        @Override
		public void tickHero(Hero hero) {
            int currentHitCount = 0;
            for (Entity entity : orbLocation.getWorld().getNearbyEntities(orbLocation, radius, height, radius)) {
                if (currentHitCount >= maxTargetsPerPulse)
                    break;
                if (!(entity instanceof LivingEntity))
                    continue;
                LivingEntity target = (LivingEntity) entity;
                if (!damageCheck(applier, target))
                    continue;

                currentHitCount++;

                addSpellTarget(target, hero);
                damageEntity(target, applier, damageTick, EntityDamageEvent.DamageCause.MAGIC, false);

                LineEffect lineVisual = new LineEffect(effectManager);
                lineVisual.particle = Particle.REDSTONE;
                lineVisual.color = blueViolet;
                lineVisual.setLocation(orbLocation);
                lineVisual.setTargetEntity(target);

                effectManager.start(lineVisual);
                orbLocation.getWorld().playSound(orbLocation, Sound.BLOCK_GLASS_BREAK, 0.3f, 2.0F);
            }
		}

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            if (this.effectManager != null)
                this.effectManager.dispose();
        }
    }
}
