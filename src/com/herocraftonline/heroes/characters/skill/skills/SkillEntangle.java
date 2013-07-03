package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.RootEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;

public class SkillEntangle extends TargettedSkill {
	public VisualEffect fplayer = new VisualEffect();

	public SkillEntangle(Heroes plugin) {
		// Heroes stuff
		super(plugin, "Entangle");
		setDescription("Deals $1 damage and roots your target in place for $2 seconds.(Effect breaks when the target takes damage)");
		setUsage("/skill entangle");
		setIdentifiers("skill entangle");
		setTypes(SkillType.HARMFUL, SkillType.DEBUFF, SkillType.SILENCABLE, SkillType.EARTH, SkillType.MOVEMENT);
		setArgumentRange(0, 0);

		// Start up the listener for root skill usage
		Bukkit.getServer().getPluginManager().registerEvents(new RootListener(), plugin);
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.DAMAGE.node(), 1);
		node.set(SkillSetting.PERIOD.node(), 100);
		node.set(SkillSetting.DURATION.node(), 4000);
		node.set(SkillSetting.USE_TEXT.node(), "[§2Skill§7] %hero% used %skill% on %target%!");
		node.set(SkillSetting.APPLY_TEXT.node(), "[§2Skill§7] %target% has been entangled!");
		node.set(SkillSetting.EXPIRE_TEXT.node(), "[§2Skill§7] %target% has broken free from the entangle!");

		return node;
	}

	@Override
	public String getDescription(Hero hero) {
		double duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false) / 1000D;
		int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 1, false);

		return getDescription().replace("$1", damage + "").replace("$2", duration + "");
	}

	@Override
	public SkillResult use(Hero hero, LivingEntity target, String[] args) {

		//deal  damage
		int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 1, false);
		damageEntity(target, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.MAGIC);

		int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);
		int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 100, false);
		String applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "[§2Skill§7] %target% has been entangled!").replace("%target%", "$1");
		String expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "[§2Skill§7] %target% has broken free from the entangle!").replace("%target%", "$1");

		// Broadcast use text
		broadcastExecuteText(hero, target);

		// Play Sound
		Player player = hero.getPlayer();
		hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ZOMBIE_WOODBREAK, 0.8F, 1.0F);

		// Play Effect
		try {
			this.fplayer.playFirework(player.getWorld(), target.getLocation().add(0.0D, 1.5D, 0.0D), FireworkEffect.builder().flicker(true).trail(false).with(FireworkEffect.Type.BURST).withColor(Color.OLIVE).build());
		}
		catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		// Check to see if we're applying to a player
		if ((target instanceof Player)) {
			// Use new root for players

			// Create the root effect
			EntangleEffect EntangleEffect = new EntangleEffect(this, period, duration, hero.getPlayer(), applyText, expireText);

			// Add root effect to the target
			CharacterTemplate targetCT = this.plugin.getCharacterManager().getCharacter(target);
			targetCT.addEffect(EntangleEffect);
		}
		else {
			// Use old root for mobs

			// Create the root effect
			RootEffect rootEffect = new RootEffect(this, duration);

			// Add root effect to the target
			CharacterTemplate targetCT = this.plugin.getCharacterManager().getCharacter(target);
			targetCT.addEffect(rootEffect);
		}

		return SkillResult.NORMAL;
	}

	private class RootListener implements Listener {
		public RootListener() {
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onSkillDamage(SkillDamageEvent event) {

			// Pre-checks
			if (event.getDamage() == 0)
				return;

			if (!(event.getEntity() instanceof LivingEntity) || !(event.getDamager() instanceof Player))
				return;

			// Get our CT's
			//CharacterTemplate attackerCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getDamager());
			CharacterTemplate defenderCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());

			/*
			if (attackerCT.hasEffect("Entangle")) {
				attackerCT.removeEffect(attackerCT.getEffect("Entangle"));
			}
			else if (attackerCT.hasEffect("Root")) {
				attackerCT.removeEffect(attackerCT.getEffect("Root"));
			}
			*/

			if (defenderCT.hasEffect("Entangle")) {
				defenderCT.removeEffect(defenderCT.getEffect("Entangle"));
			}
			else if (defenderCT.hasEffect("Root")) {
				defenderCT.removeEffect(defenderCT.getEffect("Root"));
			}
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onEntityDamage(EntityDamageEvent event) {
			if (event.getDamage() == 0)
				return;

			// Ensure that the event is meant to happen
			if (!(event instanceof EntityDamageByEntityEvent))
				return;

			// If our target isn't a living entity, lets exit
			if (!(event.getEntity() instanceof LivingEntity))
				return;

			// Make sure that this is actually a left click attack
			EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
			if (subEvent.getCause() != DamageCause.ENTITY_ATTACK || !(subEvent.getDamager() instanceof LivingEntity))
				return;

			// Get our target's CT
			//CharacterTemplate attackerCT = plugin.getCharacterManager().getCharacter((LivingEntity) subEvent.getDamager());
			CharacterTemplate defenderCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());

			/*
			if (attackerCT.hasEffect("Entangle")) {
				attackerCT.removeEffect(attackerCT.getEffect("Entangle"));
			}
			else if (attackerCT.hasEffect("Root")) {
				attackerCT.removeEffect(attackerCT.getEffect("Root"));
			}
			*/

			if (defenderCT.hasEffect("Entangle")) {
				defenderCT.removeEffect(defenderCT.getEffect("Entangle"));
			}
			else if (defenderCT.hasEffect("Root")) {
				defenderCT.removeEffect(defenderCT.getEffect("Root"));
			}
		}
	}

	private class EntangleEffect extends PeriodicExpirableEffect {
		private final String applyText;
		private final String expireText;
		private final Player applier;

		private Location loc;

		public EntangleEffect(Skill skill, int period, int duration, Player applier, String applyText, String expireText) {
			super(skill, "Entangle", period, duration);
			this.applier = applier;
			this.applyText = applyText;
			this.expireText = expireText;
			this.types.add(EffectType.ROOT);
			this.types.add(EffectType.HARMFUL);
			this.types.add(EffectType.DISPELLABLE);
		}

		@Override
		public void applyToMonster(Monster monster) {
			super.applyToMonster(monster);
			broadcast(monster.getEntity().getLocation(), applyText, Messaging.getLivingEntityName(monster), applier.getDisplayName());
		}

		@Override
		public void applyToHero(Hero hero) {
			super.applyToHero(hero);
			final Player player = hero.getPlayer();
			loc = hero.getPlayer().getLocation();
			broadcast(player.getLocation(), applyText, player.getDisplayName());
		}

		@Override
		public void removeFromHero(Hero hero) {
			super.removeFromHero(hero);
			final Player player = hero.getPlayer();
			broadcast(player.getLocation(), expireText, player.getDisplayName());
		}

		@Override
		public void tickHero(Hero hero) {
			final Location location = hero.getPlayer().getLocation();
			if ((location.getX() != loc.getX()) || (location.getZ() != loc.getZ())) {

				// Retain the player's Y position and facing directions
				loc.setYaw(location.getYaw());
				loc.setPitch(location.getPitch());
				loc.setY(location.getY());

				// Teleport the Player back into place.
				hero.getPlayer().teleport(loc);
			}
		}

		@Override
		public void tickMonster(Monster monster) {
		}

	}
}