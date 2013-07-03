package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillUseEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillSmoke extends ActiveSkill {

	private String applyText;
	private String expireText;

	public SkillSmoke(Heroes plugin) {
		super(plugin, "Smoke");
		setDescription("Vanish in a cloud of smoke! You will not be visible to other players for the next $1 seconds. Taking damage or using abilities will cause you to reappear.");
		setUsage("/skill smoke");
		setArgumentRange(0, 0);
		setIdentifiers("skill smoke");
		setNotes("Note: Taking damage removes the effect");
		setNotes("Note: Using skills removes the effect");
		setTypes(SkillType.ILLUSION, SkillType.BUFF, SkillType.COUNTER, SkillType.STEALTHY);

		Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(), plugin);
	}

	@Override
	public String getDescription(Hero hero) {
		int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
		return getDescription().replace("$1", duration + "");
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		final ConfigurationSection node = super.getDefaultConfig();
		node.set(SkillSetting.DURATION.node(), 20000);
		node.set(SkillSetting.APPLY_TEXT.node(), "§7[§2Skill§7] %hero% vanished in a cloud of smoke!");
		node.set(SkillSetting.EXPIRE_TEXT.node(), "§7[§2Skill§7] %hero% has reappeared!");
		return node;
	}

	@Override
	public void init() {
		super.init();

		applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "§7[§2Skill§7] %hero% vanished in a cloud of smoke!").replace("%hero%", "$1");
		expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "§7[§2Skill§7] %hero% has reappeared!").replace("%hero%", "$1");
	}

	@Override
	public SkillResult use(Hero hero, String[] args) {
		broadcastExecuteText(hero);

		long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 20000, false);
		Player player = hero.getPlayer();
		player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 4);
		hero.addEffect(new SmokeEffect(this, duration));

		return SkillResult.NORMAL;
	}

	public class SkillEntityListener implements Listener {

		public SkillEntityListener() {
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onSkillUse(SkillUseEvent event) {
			Hero hero = event.getHero();

			if (hero.hasEffect("SmokeEffect")) {
				hero.removeEffect(hero.getEffect("SmokeEffect"));
			}
		}

	}

	// Buff effect used to keep track of warmup time
	public class SmokeEffect extends ExpirableEffect {

		public SmokeEffect(Skill skill, long duration) {
			super(skill, "SmokeEffect", duration);

			this.types.add(EffectType.BENEFICIAL);
			this.types.add(EffectType.INVIS);
		}

		@Override
		public void applyToHero(Hero hero) {
			super.applyToHero(hero);

			Player player = hero.getPlayer();

			for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
				if (onlinePlayer.equals(player) || onlinePlayer.hasPermission("heroes.admin.seeinvis")) {
					continue;
				}
				onlinePlayer.hidePlayer(player);
			}

			if (applyText != null && applyText.length() > 0)
				broadcast(player.getLocation(), expireText, player.getDisplayName());
		}

		@Override
		public void removeFromHero(Hero hero) {
			super.removeFromHero(hero);

			Player player = hero.getPlayer();
			for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
				if (onlinePlayer.equals(player)) {
					continue;
				}
				onlinePlayer.showPlayer(player);
			}

			if (expireText != null && expireText.length() > 0)
				broadcast(player.getLocation(), expireText, player.getDisplayName());
		}
	}

}
