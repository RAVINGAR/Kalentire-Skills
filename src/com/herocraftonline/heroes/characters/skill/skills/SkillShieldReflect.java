package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
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
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;

public class SkillShieldReflect extends ActiveSkill {
	private String applyText;
	private String expireText;

	public SkillShieldReflect(Heroes plugin) {
		super(plugin, "ShieldReflect");
		setDescription("Your shield reflects $1% of the damage you take back at your attackers for $2 seconds.");
		setUsage("/skill shieldreflect");
		setArgumentRange(0, 0);
		setIdentifiers("skill shieldreflect", "skill shieldref", "skill shieldr", "skill sreflect", "skill sref");
		setTypes(SkillType.FORCE, SkillType.SILENCABLE, SkillType.BUFF, SkillType.PHYSICAL);
		Bukkit.getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();
		node.set(SkillSetting.DURATION.node(), Integer.valueOf(3000));
		node.set("reflected-amount", Integer.valueOf(2));
		node.set(SkillSetting.APPLY_TEXT.node(), "%hero% holds up their shield and is now reflecting incoming attacks!");
		node.set(SkillSetting.EXPIRE_TEXT.node(), "%hero% is no longer reflecting attacks!");
		return node;
	}

	@Override
	public String getDescription(Hero hero) {
		double reflectedAmount = SkillConfigManager.getUseSetting(hero, this, "reflected-amount", 1, false) * 100;
		double duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false) / 1000;
		return getDescription().replace("$1", reflectedAmount + "").replace("$2", duration + "");
	}

	@Override
	public void init() {
		super.init();
		this.applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%hero% holds up their shield and is now reflecting incoming attacks!").replace("%hero%", "$1");
		this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%hero% is no longer reflecting attacks!").replace("%hero%", "$1");
	}

	@Override
	public SkillResult use(Hero hero, String[] args) {
		int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
		Player player = hero.getPlayer();

		switch (player.getItemInHand().getType()) {
		case IRON_DOOR:
		case WOOD_DOOR:
		case TRAP_DOOR:
			player.getWorld().playEffect(player.getLocation(), Effect.MOBSPAWNER_FLAMES, 3);
			hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ZOMBIE_METAL, 0.8F, 1.0F);
			broadcastExecuteText(hero);
			hero.addEffect(new ShieldReflectEffect(this, duration));
			return SkillResult.NORMAL;
		default:
			Messaging.send(player, "You must have a shield equipped to use this skill");
			return SkillResult.FAIL;
		}
	}

	public class SkillEntityListener implements Listener {
		private final Skill skill;

		public SkillEntityListener(Skill skill) {
			this.skill = skill;
		}

		@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
		public void onSkillDamage(SkillDamageEvent event) {
			if (!(event.getEntity() instanceof LivingEntity))
				return;

			LivingEntity defenderLE = (LivingEntity) event.getEntity();
			CharacterTemplate defenderCT = plugin.getCharacterManager().getCharacter(defenderLE);

			if (!(defenderCT instanceof Player))
				return;

			Player defenderPlayer = (Player) defenderCT;
			Hero defenderHero = plugin.getCharacterManager().getHero(defenderPlayer);
			if (defenderHero.hasEffect("ShieldReflect")) {
				CharacterTemplate attackerCT = event.getDamager();
				if ((attackerCT instanceof Player)) {
					Player attackerPlayer = (Player) attackerCT;
					if (plugin.getCharacterManager().getHero(attackerPlayer).hasEffect(getName())) {
						event.setCancelled(true);
						return;
					}
				}

				switch (defenderPlayer.getItemInHand().getType()) {
				case IRON_DOOR:
				case WOOD_DOOR:
				case TRAP_DOOR:
					double damage = event.getDamage() * SkillConfigManager.getUseSetting(defenderHero, skill, "reflected-amount", 2, false);
					plugin.getDamageManager().addSpellTarget(attackerCT.getEntity(), defenderHero, skill);
					Skill.damageEntity(attackerCT.getEntity(), defenderLE, damage, DamageCause.MAGIC);
				default:
					return;
				}
			}
		}

		@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
		public void onEntityDamage(EntityDamageEvent event) {
			if (!(event instanceof EntityDamageByEntityEvent)) {
				return;
			}

			EntityDamageByEntityEvent edbe = (EntityDamageByEntityEvent) event;
			Entity defender = edbe.getEntity();
			Entity attacker = edbe.getDamager();
			if (((attacker instanceof LivingEntity)) && ((defender instanceof Player))) {
				Player defenderPlayer = (Player) defender;
				Hero defenderHero = plugin.getCharacterManager().getHero(defenderPlayer);
				if (defenderHero.hasEffect("ShieldReflect")) {
					if ((attacker instanceof Player)) {
						Player attackerPlayer = (Player) attacker;
						if (plugin.getCharacterManager().getHero(attackerPlayer).hasEffect(getName())) {
							event.setCancelled(true);
							return;
						}
					}

					switch (defenderHero.getPlayer().getItemInHand().getType()) {
					case IRON_DOOR:
					case WOOD_DOOR:
					case TRAP_DOOR:
						double damage = event.getDamage() * SkillConfigManager.getUseSetting(defenderHero, skill, "reflected-amount", 2, false);
						plugin.getDamageManager().addSpellTarget(attacker, defenderHero, skill);
						Skill.damageEntity((LivingEntity) attacker, (LivingEntity) defender, damage, DamageCause.MAGIC);
					default:
						return;
					}
				}
			}
		}
	}

	public class ShieldReflectEffect extends ExpirableEffect {
		public ShieldReflectEffect(Skill skill, long duration) {
			super(skill, "ShieldReflect", duration);
			this.types.add(EffectType.DISPELLABLE);
			this.types.add(EffectType.BENEFICIAL);
		}

		@Override
		public void applyToHero(Hero hero) {
			super.applyToHero(hero);
			Player player = hero.getPlayer();
			broadcast(player.getLocation(), SkillShieldReflect.this.applyText, new Object[] { player.getDisplayName() });
		}

		@Override
		public void removeFromHero(Hero hero) {
			super.removeFromHero(hero);
			Player player = hero.getPlayer();
			broadcast(player.getLocation(), SkillShieldReflect.this.expireText, new Object[] { player.getDisplayName() });
		}
	}
}
