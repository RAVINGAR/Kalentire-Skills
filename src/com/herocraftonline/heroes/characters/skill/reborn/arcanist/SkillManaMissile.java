package com.herocraftonline.heroes.characters.skill.reborn.arcanist;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillUseEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.tools.BasicMissile;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.Effect;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.EffectType;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.List;

public class SkillManaMissile extends PassiveSkill {

	private static Color blueViolet = Color.fromRGB(138,43,226);
	private static String cooldownEffectName = "ManaMissile-Cooldown";

	public SkillManaMissile(Heroes plugin) {
		super(plugin, "ManaMissile");
		setDescription("Conjure a bolt of mana that deals $1 damage.");
		setUsage("/skill manamissile");
		setIdentifiers("skill manamissile");
		setArgumentRange(0, 0);
		setTypes(SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_MAGICAL);

		Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
	}

	public String getDescription(Hero hero) {
		final double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50.0, false);
		return getDescription().replace("$1", Util.decFormat.format(damage));
	}

	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection config = super.getDefaultConfig();
		config.set(SkillSetting.DAMAGE.node(), 50.0);
		config.set("catalysts", Util.hoes);
        config.set("projectile-size", 0.25);
		config.set(SkillSetting.COOLDOWN.node(), 2000);
		config.set("projectile-velocity", 20.0);
		return config;
	}

	public class SkillHeroListener implements Listener {
		private Skill skill;

		public SkillHeroListener(Skill skill) {
			this.skill = skill;
		}

		@EventHandler(priority = EventPriority.MONITOR)
		public void onLeftClick(PlayerInteractEvent event) {
			if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK)
				return;

			Player player = event.getPlayer();
			Hero hero = plugin.getCharacterManager().getHero(player);
			if (!validateCanCast(hero))
				return;

			double projSize = SkillConfigManager.getUseSetting(hero, skill, "projectile-size", 0.25, false);
			double projVelocity = SkillConfigManager.getUseSetting(hero, skill, "projectile-velocity", 20.0, false);
			ManaProjectile missile = new ManaProjectile(plugin, skill, hero, projSize, projVelocity);
			missile.fireMissile();
			player.getWorld().playSound(player.getLocation(), Sound.ENTITY_VEX_HURT, 2F, 1F);

			int cooldown = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.COOLDOWN, 2000, false);
			hero.addEffect(new CooldownEffect(skill, player, cooldown));
		}

		private boolean validateCanCast(Hero hero) {
			if (!hero.canUseSkill(skill))
				return false;

			if (hero.hasEffect(cooldownEffectName)) {
				double remainingTime = ((CooldownEffect) hero.getEffect(cooldownEffectName)).getRemainingTime() / 1000.0;
				if (remainingTime > 0.0) {	// Sometimes we are below zero with this thing. Kinda weird.
					String formattedRemainingTime = Util.decFormatCDs.format(remainingTime);
					ActiveSkill.sendResultMessage(hero, skill, new SkillResult(SkillResult.ResultType.ON_COOLDOWN, true, skill.getName(), formattedRemainingTime));
					return false;
				}
			}

			Player player = hero.getPlayer();
			PlayerInventory playerInv = player.getInventory();
			ItemStack mainHand = NMSHandler.getInterface().getItemInMainHand(playerInv);

			List<String> allowedCatalysts = SkillConfigManager.getUseSetting(hero, skill, "catalysts", Util.hoes);
			if (mainHand == null || !allowedCatalysts.contains(mainHand.getType().name()))
				return false;

			double healthCost = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.HEALTH_COST, 0.0, false);
			int stamCost = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.STAMINA, 0, false);
			int manaCost = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.MANA, 0, false);

			final SkillUseEvent skillEvent = new SkillUseEvent(skill, player, hero, manaCost, healthCost, stamCost, null, null);
			plugin.getServer().getPluginManager().callEvent(skillEvent);
			if (skillEvent.isCancelled()) {
				ActiveSkill.sendResultMessage(hero, skill, SkillResult.CANCELLED);
				return false;
			}

			// Update manaCost with result of SkillUseEvent
			manaCost = skillEvent.getManaCost();
			if (manaCost > hero.getMana()) {
				ActiveSkill.sendResultMessage(hero, skill, SkillResult.LOW_MANA);
				return false;
			}

			// Update healthCost with results of SkillUseEvent
			healthCost = skillEvent.getHealthCost();
			if (healthCost > 0 && (hero.getPlayer().getHealth() <= healthCost)) {
				ActiveSkill.sendResultMessage(hero, skill, SkillResult.LOW_HEALTH);
				return false;
			}

			//Update staminaCost with results of SkilluseEvent
			stamCost = skillEvent.getStaminaCost();
			if (stamCost > 0 && (hero.getStamina() < stamCost)) {
				ActiveSkill.sendResultMessage(hero, skill, SkillResult.LOW_STAMINA);
				return false;
			}

			// Deduct health
			if (healthCost > 0) {
				player.setHealth(player.getHealth() - healthCost);
			}

			// Deduct mana
			if (manaCost > 0) {
				hero.setMana(hero.getMana() - manaCost);
				if (hero.isVerboseMana()) {
					hero.getPlayer().sendMessage(ChatComponents.Bars.mana(hero.getMana(), hero.getMaxMana(), true));
				}
			}

			// Deduct stamina
			if (stamCost > 0) {
				hero.setStamina(hero.getStamina() - stamCost);
				if (hero.isVerboseStamina()) {
					hero.getPlayer().sendMessage(ChatComponents.Bars.stamina(hero.getStamina(), hero.getMaxStamina(), true));
				}
			}

			return true;
		}
	}

	private class CooldownEffect extends ExpirableEffect {
		public CooldownEffect(Skill skill, Player applier, long duration) {
			super(skill, cooldownEffectName, applier, duration);
		}
	}

	private class ManaProjectile extends BasicMissile {
		public ManaProjectile(Plugin plugin, Skill skill, Hero hero, double projectileSize, double projVelocity) {
			super(plugin, skill, hero, projectileSize, projVelocity);

			setNoGravity();
			setRemainingLife(30);
			this.damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 50.0, false);
			this.visualEffect = new ManaMissileVisualEffect(this.effectManager);
		}

		@Override
		protected void onTick() {
			Location loc = getLocation();
			this.visualEffect.setLocation(loc);
//			if (this.getTicksLived() % 2 == 0) {
//				loc.getWorld().playSound(loc, Sound.ENTITY_VEX_HURT, 2F, 1F);
//			}
		}
	}

	private class ManaMissileVisualEffect extends Effect {
		private static final double halfPi = Math.PI / 2.0;

		private double step;
		public Particle particle = Particle.REDSTONE;
		public Color color = BasicMissile.DEFAULT_COLOR;
		private final double sizeMultiplier = 0.5;
		private double rotationSpeed = 4.0D;

		ManaMissileVisualEffect(EffectManager effectManager) {
			super(effectManager);

			this.type = EffectType.REPEATING;
			this.period = 1;
			this.iterations = 6000; // Arbitrary 5 minute duration. We will either dispose it early, or the extender will modify the effect themselves.
		}

		@Override
		public void onRun() {
			step += 1.0D;
			if (step > 20.0D) {
				step = 1.0D;
			}
			Location loc = getLocation();
			for (int j = 0; j < 2; j++) {
				for (double i = -Math.PI; i < Math.PI; i += halfPi) {
					Vector v = new Vector(Math.cos(i + step / rotationSpeed), Math.sin(i + step / rotationSpeed), 0.0D);
					v = rotate(v, loc).multiply(sizeMultiplier);

					display(particle, loc.add(v), color, 0.8F, 0);		// For some reason effect lib looks like shit with the exact same parameters. Makes no sense to me.

					// Doesn't work super great with redstone. You want FireworkSpark instead if you're gonna use this.
//					Particle.DustOptions data = new Particle.DustOptions(color, 1.0F);
//					loc.getWorld().spawnParticle(particle, loc, 0, v.getX(), v.getY(), v.getZ(), 0.08D, data, false);

//					this.particleOffsetX = (float) v.getX();
//					this.particleOffsetY = (float) v.getY();
//					this.particleOffsetZ = (float) v.getZ();
//					display(particle, loc, color, 0.8F, 0);		// For some reason effect lib looks like shit with the exact same parameters. Makes no sense to me.
				}
			}
		}

		private Vector rotate(Vector v, Location loc) {
			double yaw = loc.getYaw() / 180.0F * Math.PI;
			double pitch = loc.getPitch() / 180.0F * Math.PI;
			v = rotAxisX(v, pitch);
			v = rotAxisY(v, -yaw);
			return v;
		}

		public Vector rotAxisX(Vector v, double a) {
			double y = v.getY() * Math.cos(a) - v.getZ() * Math.sin(a);
			double z = v.getY() * Math.sin(a) + v.getZ() * Math.cos(a);
			return v.setY(y).setZ(z);
		}

		public Vector rotAxisY(Vector v, double b) {
			double x = v.getX() * Math.cos(b) + v.getZ() * Math.sin(b);
			double z = v.getX() * -Math.sin(b) + v.getZ() * Math.cos(b);
			return v.setX(x).setZ(z);
		}

		public Vector rotAxisZ(Vector v, double c) {
			double x = v.getX() * Math.cos(c) - v.getY() * Math.sin(c);
			double y = v.getX() * Math.sin(c) + v.getY() * Math.cos(c);
			return v.setX(x).setY(y);
		}
	}
}
