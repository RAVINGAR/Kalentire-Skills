package com.herocraftonline.heroes.characters.skill.pack8;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.DisarmEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;

public class SkillBarrier extends ActiveSkill {

	private String applyText;
	private String expireText;

	public SkillBarrier(Heroes plugin) {
		super(plugin, "Barrier");
		setDescription("Create a protective barrier around yourself for $1 seconds. The barrier allows you to retaliate against all incoming melee attacks, disarming them for $2 seconds, and dealing $3% of your weapon damage to them.");
		setUsage("/skill barrier");
		setArgumentRange(0, 0);
		setIdentifiers("skill barrier");
		setTypes(SkillType.ABILITY_PROPERTY_LIGHT, SkillType.DAMAGING, SkillType.BUFFING, SkillType.AGGRESSIVE);

		Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
	}

	@Override
	public String getDescription(Hero hero) {
		int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);

		int disarmDuration = SkillConfigManager.getUseSetting(hero, this, "disarm-duration", 3000, false);

		double damageMultiplier = SkillConfigManager.getUseSetting(hero, this, "damage-multiplier", 0.4, false);
		double damageMultiplierIncrease = SkillConfigManager.getUseSetting(hero, this, "damage-multiplier-increase-per-intellect", 0.00875, false);
		damageMultiplier += hero.getAttributeValue(AttributeType.INTELLECT) * damageMultiplierIncrease;

		String formattedDuration = Util.decFormat.format(duration / 1000.0);
		String formattedDisarmDuration = Util.decFormat.format(disarmDuration / 1000.0);
		String formattedDamageMultiplier = Util.decFormat.format(damageMultiplier * 100);

		return getDescription().replace("$1", formattedDuration).replace("$2", formattedDisarmDuration).replace("$3", formattedDamageMultiplier);
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set("damage-multiplier", 0.4);
		node.set("damage-multiplier-increase-per-intellect", 0.00875);
		node.set(SkillSetting.DURATION.node(), 5000);
		node.set(SkillSetting.DURATION_INCREASE_PER_INTELLECT.node(), 75);
		node.set("slow-amplifier", 35);
		node.set("disarm-duration", 3000);
		node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has created a Barrier!");
		node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero%'s Barrier has faded.");

		return node;
	}

	@Override
	public void init() {
		super.init();

		applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% has created a Barrier!").replace("%hero%", "$1");
		expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero%'s Barrier has faded.").replace("%hero%", "$1");
	}

	public ArrayList<Location> circle(Location centerPoint, int particleAmount, double circleRadius)
	{
		World world = centerPoint.getWorld();

		double increment = (2 * Math.PI) / particleAmount;

		ArrayList<Location> locations = new ArrayList<Location>();

		for (int i = 0; i < particleAmount; i++)
		{
			double angle = i * increment;
			double x = centerPoint.getX() + (circleRadius * Math.cos(angle));
			double z = centerPoint.getZ() + (circleRadius * Math.sin(angle));
			locations.add(new Location(world, x, centerPoint.getY(), z));
		}
		return locations;
	}

	@Override
	public SkillResult use(Hero hero, String[] args) {
		Player player = hero.getPlayer();

		broadcastExecuteText(hero);

		int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
		int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_INTELLECT, 75, false);
		duration += hero.getAttributeValue(AttributeType.INTELLECT) * durationIncrease;

		int disarmDuration = SkillConfigManager.getUseSetting(hero, this, "disarm-duration", 3000, false);
		int slowAmplifier = SkillConfigManager.getUseSetting(hero, this, "slow-amplifier", 3, false);

		hero.addEffect(new BarrierEffect(this, player, duration, slowAmplifier, disarmDuration));

        List<Location> circle = circle(player.getLocation(), 36, 1.5);
		for (int i = 0; i < circle.size(); i++)
		{
			//player.getWorld().spigot().playEffect(circle(player.getLocation(), 36, 1.5).get(i), org.bukkit.Effect.TILE_BREAK, Material.STONE.getId(), 0, 0.2F, 1.5F, 0.2F, 0, 4, 16);
            player.getWorld().spawnParticle(Particle.BLOCK_CRACK, circle.get(i), 4, 0.2, 1.5, 0.2, 0);
		}

		player.getWorld().playEffect(player.getLocation(), org.bukkit.Effect.SMOKE, 3);
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.7F, 2.0F);

		return SkillResult.NORMAL;
	}

	public class SkillEntityListener implements Listener {

		private Skill skill;

		SkillEntityListener(Skill skill) {
			this.skill = skill;
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onWeaponDamage(WeaponDamageEvent event) {
			if (event.getDamage() == 0)
				return;

			// Handle outgoing
			if (event.getDamager() instanceof Hero && event.getEntity() instanceof Player && !(event.getAttackerEntity() instanceof Projectile)) {

				Player defenderPlayer = (Player) event.getEntity();
				Hero defenderHero = plugin.getCharacterManager().getHero(defenderPlayer);
				Player damagerPlayer = ((Hero) event.getDamager()).getPlayer();
				Hero damagerHero = plugin.getCharacterManager().getHero(damagerPlayer);

				if ((defenderPlayer.getNoDamageTicks() > 10) || defenderPlayer.isDead() || defenderPlayer.getHealth() <= 0)
					return;

				// Check if they are under the effects of Barrier
				if (defenderHero.hasEffect("Barrier")) {
					BarrierEffect bgEffect = (BarrierEffect) defenderHero.getEffect("Barrier");

					if (!(damageCheck(defenderPlayer, (LivingEntity) damagerPlayer) && damageCheck(damagerPlayer, (LivingEntity) defenderPlayer)))
						return;

					for (Effect effect : defenderHero.getEffects()) {
						if (effect.isType(EffectType.STUN) || effect.isType(EffectType.DISABLE)) {
							defenderHero.removeEffect(bgEffect);
							return;
						}
					}

					if (damagerHero.hasEffect("Barrier"))
						return;

					// Cancel the attack
					event.setCancelled(true);

					// Make them have invuln ticks so attackers dont get machine-gunned from attacking the buffed player.
					defenderPlayer.setNoDamageTicks(defenderPlayer.getMaximumNoDamageTicks());

					double damageMultiplier = SkillConfigManager.getUseSetting(defenderHero, skill, "damage-multiplier", 0.4, false);
					double damageMultiplierIncrease = SkillConfigManager.getUseSetting(defenderHero, skill, "damage-multiplier-increase-per-intellect", 0.00875, false);
					damageMultiplier += defenderHero.getAttributeValue(AttributeType.INTELLECT) * damageMultiplierIncrease;

					Material item = NMSHandler.getInterface().getItemInMainHand(defenderPlayer.getInventory()).getType();
					double damage = plugin.getDamageManager().getHighestItemDamage(defenderHero, item) * damageMultiplier;
					addSpellTarget(damagerPlayer, defenderHero);
					damageEntity(damagerPlayer, defenderPlayer, damage, DamageCause.ENTITY_ATTACK);

					damagerPlayer.getWorld().playSound(damagerPlayer.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8F, 1.0F);

					// Disarm checks
					Material heldItem = NMSHandler.getInterface().getItemInMainHand(damagerPlayer.getInventory()).getType();
					if (!Util.isWeapon(heldItem) && !Util.isAwkwardWeapon(heldItem)) {
						return;
					}
					if (damagerHero.hasEffectType(EffectType.DISARM)) {
						return;
					}

					// Disarm attacker
					long disarmDuration = bgEffect.getDisarmDuration();
					damagerHero.addEffect(new DisarmEffect(skill, defenderPlayer, disarmDuration));

					//damagerPlayer.getWorld().playSound(damagerPlayer.getLocation(), Sound.HURT, 0.8F, 0.5F);
				}
			}
		}
	}

	public class BarrierEffect extends ExpirableEffect {

		private long disarmDuration;

		public BarrierEffect(Skill skill, Player applier, long duration, int slowAmplifier, long disarmDuration) {
			super(skill, "Barrier", applier, duration, applyText, expireText);

			types.add(EffectType.PHYSICAL);
			types.add(EffectType.BENEFICIAL);

			this.disarmDuration = disarmDuration;

			int tickDuration = (int) ((duration / 1000) * 20);
			addPotionEffect(new PotionEffect(PotionEffectType.SLOW, tickDuration, slowAmplifier), false);
			//addMobEffect(8, tickDuration, 254, false);
		}

		public long getDisarmDuration() {
			return disarmDuration;
		}

		public void setDisarmDuration(long disarmDuration) {
			this.disarmDuration = disarmDuration;
		}
	}
}
