package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;
import com.herocraftonline.heroes.util.Util;

public class SkillBladegrasp extends ActiveSkill {

	private String applyText;
	private String expireText;
	private String parryText;
	private String parrySkillText;

	public SkillBladegrasp(Heroes plugin) {
		super(plugin, "Bladegrasp");
		setDescription("You have a $1% chance to block incoming damage for $2 seconds.");
		setUsage("/skill bladegrasp");
		setArgumentRange(0, 0);
		setIdentifiers("skill bladegrasp", "skill bgrasp");
		setTypes(SkillType.PHYSICAL, SkillType.BUFF);
		Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();
		node.set(Setting.DURATION.node(), 5000);
		node.set(Setting.APPLY_TEXT.node(), "%hero% tightened his grip!");
		node.set(Setting.EXPIRE_TEXT.node(), "%hero% loosened his grip!");
		node.set("parry-text", "%hero% parried an attack!");
		node.set("parry-skill-text", "%hero% has parried %target%'s %skill%.");
		node.set(Setting.CHANCE_LEVEL.node(), .02);
		return node;
	}

	@Override
	public void init() {
		super.init();
		applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "%hero% tightened his grip!").replace("%hero%", "$1");
		expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "%hero% loosened his grip!").replace("%hero%", "$1");
		parryText = SkillConfigManager.getRaw(this, "parry-text", "%hero% parried an attack!").replace("%hero%", "$1");
		parrySkillText = SkillConfigManager.getRaw(this, "parry-skill-text", "%hero% has parried %target%'s %skill%.").replace("$1","%hero$").replace("$2","%target%").replace("$3","%skill");
	}

	@Override
	public SkillResult use(Hero hero, String[] args) {
		broadcastExecuteText(hero);
		int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
		hero.addEffect(new BladegraspEffect(this, duration));

		return SkillResult.NORMAL;
	}

	public class BladegraspEffect extends ExpirableEffect {

		public BladegraspEffect(Skill skill, long duration) {
			super(skill, "Bladegrasp", duration);
			this.types.add(EffectType.PHYSICAL);
			this.types.add(EffectType.BENEFICIAL);
		}

		@Override
		public void apply(Hero hero) {
			super.apply(hero);
			Player player = hero.getPlayer();
			broadcast(player.getLocation(), applyText, player.getDisplayName());
		}

		@Override
		public void remove(Hero hero) {
			super.remove(hero);
			Player player = hero.getPlayer();
			broadcast(player.getLocation(), expireText, player.getDisplayName());
		}

	}

	public class SkillEntityListener implements Listener {

		private Skill skill;
		
		SkillEntityListener(Skill skill) {
			this.skill = skill;
		}
		
		@EventHandler()
		public void onWeaponDamage(WeaponDamageEvent event) {
			// Ignore cancelled damage events & 0 damage events for Spam Control
			if (event.getDamage() == 0 || event.isCancelled() || !(event.getEntity() instanceof Player)) {
				return;
			}

			Player player = (Player) event.getEntity();
			Hero hero = plugin.getHeroManager().getHero(player);
			if (hero.hasEffect(getName())) {
				double parryChance = SkillConfigManager.getUseSetting(hero, skill, Setting.CHANCE_LEVEL, .02, false) * hero.getSkillLevel(skill);
				if (Util.rand.nextDouble() > parryChance)
					return;

				event.setCancelled(true);
				String message = Messaging.parameterizeMessage(parryText, player.getDisplayName());
				Messaging.send(player, message);
				if (event.getDamager() instanceof Player) {
					Messaging.send((Player) event.getDamager(), message);
				}
			}
		}

		@EventHandler()
		public void onSkillDamage(SkillDamageEvent event) {
			// Ignore cancelled damage events & 0 damage events for Spam Control
			if (event.getDamage() == 0 || event.isCancelled() || !event.getSkill().isType(SkillType.PHYSICAL) || !(event.getEntity() instanceof Player)) {
				return;
			}
			Player player = (Player) event.getEntity();
			Hero hero = plugin.getHeroManager().getHero(player);
			if (hero.hasEffect(getName())) {
				double parryChance = SkillConfigManager.getUseSetting(hero, skill, Setting.CHANCE_LEVEL, .02, false) * hero.getSkillLevel(event.getSkill());
				if (Util.rand.nextDouble() > parryChance)
					return;

				event.setCancelled(true);
				String message = Messaging.parameterizeMessage(parrySkillText, player.getDisplayName(), event.getDamager().getPlayer().getDisplayName(), event.getSkill().getName());
				Messaging.send(player, message);
				Messaging.send(event.getDamager().getPlayer(), message);
				
			}
		}
	}

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
        double chance = SkillConfigManager.getUseSetting(hero, this, Setting.CHANCE_LEVEL, .02, false);
        int level = hero.getSkillLevel(this);
        if (level < 1)
            level = 1;
        return getDescription().replace("$1", Util.stringDouble(chance * level * 100)).replace("$2", duration / 1000 + "");
    }
}
