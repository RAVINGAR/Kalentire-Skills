package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillUndyingWill extends ActiveSkill {
	private String expireText;

	public SkillUndyingWill(Heroes plugin) {
		super(plugin, "UndyingWill");
		setDescription("You are overcome with an undying will to survive. You cannot be killed for the next $1 seconds.");
		setUsage("/skill undyingwill");
		setArgumentRange(0, 0);
		setIdentifiers("skill undyingwill");
		setTypes(SkillType.BUFF, SkillType.COUNTER, SkillType.PHYSICAL);

		Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(), plugin);
	}

	public String getDescription(Hero hero) {
		double duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, true) / 1000;

		return getDescription().replace("$1", duration + "");
	}

	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.DURATION.node(), 4000);
		node.set(SkillSetting.USE_TEXT.node(), "%hero% is overcome with an undying will!");
		node.set(SkillSetting.EXPIRE_TEXT.node(), "%hero%'s will returns to normal.");

		return node;
	}

	public void init() {
		super.init();
		expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%hero%'s will returns to normal.").replace("%hero%", "$1");
	}

	public SkillResult use(Hero hero, String[] args) {

		long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);
		hero.addEffect(new UndyingWillEffect(this, duration));
		hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENDERDRAGON_GROWL, 0.5F, 0.1F);

		broadcastExecuteText(hero);

		return SkillResult.NORMAL;
	}

	public class SkillHeroListener implements Listener {
		public SkillHeroListener() {
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onEntityDamage(EntityDamageEvent event) {

			// If our target isn't a Living Entity exit
			if (!(event.getEntity() instanceof LivingEntity)) {
				return;
			}

			Entity entity = event.getEntity();
			LivingEntity livingEntity = (LivingEntity) entity;

			if (entity instanceof Player) {

				Hero hero = plugin.getCharacterManager().getHero((Player) entity);

				// Don't let them go below 1HP.
				if (hero.hasEffect("UndyingWillEffect")) {
					// broadcast(hero.getPlayer().getLocation(), "Heroname: " + hero.getPlayer().getDisplayName() + ". Current HP: " + livingEntity.getHealth() + ". Damage: " + event.getDamage() + ".", hero.getPlayer().getDisplayName()); // DEBUG
					if (event.getDamage() > livingEntity.getHealth()) {
						if (livingEntity.getHealth() == 1)
							event.setDamage(0);
						else {
							int currentHealth = livingEntity.getHealth();
							event.setDamage(currentHealth - 1);
						}
					}
					// broadcast(hero.getPlayer().getLocation(), "Event Damage: " + event.getDamage() + "."); // DEBUG
				}
			}
		}
	}

	public class UndyingWillEffect extends ExpirableEffect {
		public UndyingWillEffect(Skill skill, long duration) {
			super(skill, "UndyingWillEffect", duration);

			this.types.add(EffectType.PHYSICAL);
		}

		public void removeFromHero(Hero hero) {
			super.removeFromHero(hero);
			Player player = hero.getPlayer();
			broadcast(player.getLocation(), expireText, player.getDisplayName());
		}
	}
}