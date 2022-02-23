package com.herocraftonline.heroes.characters.skill.remastered;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.DisarmEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.GeometryUtil;
import com.herocraftonline.heroes.util.Util;
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

import java.util.ArrayList;
import java.util.List;

public class SkillBarrier extends ActiveSkill {

	private String applyText;
	private String expireText;

	public SkillBarrier(Heroes plugin) {
		super(plugin, "Barrier");
        setDescription("Create a protective barrier around yourself for $1 second(s). " +
                "The barrier allows you to retaliate against all incoming melee attacks, disarming them for $2 seconds," +
				" and dealing $3% of your weapon damage to them.");
		setUsage("/skill barrier");
        setIdentifiers("skill barrier");
		setArgumentRange(0, 0);
		setTypes(SkillType.ABILITY_PROPERTY_LIGHT, SkillType.DAMAGING, SkillType.BUFFING, SkillType.AGGRESSIVE);

		Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
	}

	@Override
	public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
		int disarmDuration = SkillConfigManager.getUseSetting(hero, this, "disarm-duration", 3000, false);
        double damageMultiplier = SkillConfigManager.getScaledUseSettingDouble(hero, this, "damage-multiplier", false);

		return getDescription()
				.replace("$1", Util.decFormat.format(duration / 1000.0))
				.replace("$2", Util.decFormat.format(disarmDuration / 1000.0))
				.replace("$3", Util.decFormat.format(damageMultiplier * 100));
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("damage-multiplier", 0.4);
        config.set("damage-multiplier-increase-per-intellect", 0.00875);
        config.set(SkillSetting.DURATION.node(), 5000);
        config.set(SkillSetting.DURATION_INCREASE_PER_INTELLECT.node(), 75);
        config.set("slow-amplifier", 35);
        config.set("disarm-duration", 3000);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has created a Barrier!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero%'s Barrier has faded.");
        return config;
	}

	@Override
	public void init() {
		super.init();

		applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% has created a Barrier!").replace("%hero%", "$1");
		expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero%'s Barrier has faded.").replace("%hero%", "$1");
	}

	@Override
	public SkillResult use(Hero hero, String[] args) {
		Player player = hero.getPlayer();

		broadcastExecuteText(hero);

        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);

		int disarmDuration = SkillConfigManager.getUseSetting(hero, this, "disarm-duration", 3000, false);
		int slowAmplifier = SkillConfigManager.getUseSetting(hero, this, "slow-amplifier", 3, false);

		hero.addEffect(new BarrierEffect(this, player, duration, slowAmplifier, disarmDuration));

        List<Location> circle = GeometryUtil.circle(player.getLocation(), 36, 1.5);
        for (int i = 0; i < circle.size(); i++) {
			//player.getWorld().spigot().playEffect(circle(player.getLocation(), 36, 1.5).get(i), org.bukkit.Effect.TILE_BREAK, Material.STONE.getId(), 0, 0.2F, 1.5F, 0.2F, 0, 4, 16);
            player.getWorld().spawnParticle(Particle.BLOCK_CRACK, circle.get(i), 4, 0.2, 1.5, 0.2, 0, Bukkit.createBlockData(Material.STONE));
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

					double damageMultiplier = SkillConfigManager.getScaledUseSettingDouble(defenderHero, skill, "damage-multiplier", false);

					Material item = defenderPlayer.getInventory().getItemInMainHand().getType();
					Double itemDamage = plugin.getDamageManager().getFlatItemDamage(defenderHero, item);


					double damage = itemDamage * damageMultiplier; // FIXME received null here, is this fix now using default damage?
					addSpellTarget(damagerPlayer, defenderHero);
					damageEntity(damagerPlayer, defenderPlayer, damage, DamageCause.ENTITY_ATTACK);

					damagerPlayer.getWorld().playSound(damagerPlayer.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8F, 1.0F);

					// Disarm checks
					Material heldItem = damagerPlayer.getInventory().getItemInMainHand().getType();
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