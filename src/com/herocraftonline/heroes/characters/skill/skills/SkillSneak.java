package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SneakEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;

public class SkillSneak extends ActiveSkill {

	private boolean damageCancels;
	private boolean attackCancels;

	public SkillSneak(Heroes plugin) {
		super(plugin, "Sneak");
		setDescription("You crouch into the shadows.");
		setUsage("/skill stealth");
		setArgumentRange(0, 0);
		setIdentifiers("skill sneak");
		setTypes(SkillType.BUFF, SkillType.PHYSICAL, SkillType.STEALTHY);
		Bukkit.getServer().getPluginManager().registerEvents(new SkillEventListener(), plugin);
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		final ConfigurationSection node = super.getDefaultConfig();
		node.set(SkillSetting.DURATION.node(), Integer.valueOf(600000)); // 10 minutes in milliseconds
		node.set("damage-cancels", true);
		node.set("attacking-cancels", true);
		node.set("refresh-interval", Integer.valueOf(5000)); // in milliseconds
		return node;
	}

	@Override
	public void init() {
		super.init();
		damageCancels = SkillConfigManager.getRaw(this, "damage-cancels", true);
		attackCancels = SkillConfigManager.getRaw(this, "attacking-cancels", true);
	}

	@Override
	public SkillResult use(Hero hero, String[] args) {
		if (hero.hasEffect("Sneak")) {
			hero.removeEffect(hero.getEffect("Sneak"));
			return SkillResult.REMOVED_EFFECT;
		}
		else {
			Messaging.send(hero.getPlayer(), "You are now sneaking");

			final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 600000, false);
			final int period = SkillConfigManager.getUseSetting(hero, this, "refresh-interval", 5000, true);

			if (hero.getPlayer().isSneaking())
				hero.addEffect(new SneakEffect(this, period, duration, true));
			else
				hero.addEffect(new SneakEffect(this, period, duration, false));
		}
		return SkillResult.NORMAL;
	}

	public class SkillEventListener implements Listener {

		@EventHandler(priority = EventPriority.MONITOR)
		public void onEntityDamage(EntityDamageEvent event) {
			if (event.isCancelled() || !damageCancels || (event.getDamage() == 0)) {
				return;
			}

			Player player = null;
			if (event.getEntity() instanceof Player) {
				player = (Player) event.getEntity();
				final Hero hero = plugin.getCharacterManager().getHero(player);
				if (hero.hasEffect("Sneak")) {
					player.setSneaking(false);
					hero.removeEffect(hero.getEffect("Sneak"));
				}
			}

			player = null;
			if (attackCancels && (event instanceof EntityDamageByEntityEvent)) {
				final EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
				if (subEvent.getDamager() instanceof Player) {
					player = (Player) subEvent.getDamager();
				}
				else if (subEvent.getDamager() instanceof Projectile) {
					if (((Projectile) subEvent.getDamager()).getShooter() instanceof Player) {
						player = (Player) ((Projectile) subEvent.getDamager()).getShooter();
					}
				}

				if (player != null) {
					final Hero hero = plugin.getCharacterManager().getHero(player);
					if (hero.hasEffect("Sneak")) {
						player.setSneaking(false);
						hero.removeEffect(hero.getEffect("Sneak"));
					}
				}
			}
		}

		@EventHandler(priority = EventPriority.HIGHEST)
		public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
			final Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
			if (hero.hasEffect("Sneak")) {
				SneakEffect sEffect = (SneakEffect) hero.getEffect("Sneak");

				// Messaging.send(hero.getPlayer(), "Sneak Toggle Event. Switching to sneak == " + event.isSneaking());	// DEBUG
				if (!event.isSneaking()) {
					// Messaging.send(hero.getPlayer(), "Player is leaving sneak. Setting vanilla to false.");	// DEBUG
					sEffect.setVanillaSneaking(false);
					event.setCancelled(true);
				}
				else {
					// Messaging.send(hero.getPlayer(), "Player is entering sneak. Setting vanilla to true.");	// DEBUG
					sEffect.setVanillaSneaking(true);
				}
			}
		}
	}

	@Override
	public String getDescription(Hero hero) {
		return getDescription();
	}
}
