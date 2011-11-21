package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.util.config.ConfigurationNode;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.HeroesEventListener;
import com.herocraftonline.dev.heroes.api.SkillDamageEvent;
import com.herocraftonline.dev.heroes.api.WeaponDamageEvent;
import com.herocraftonline.dev.heroes.effects.EffectType;
import com.herocraftonline.dev.heroes.effects.ExpirableEffect;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.herocraftonline.dev.heroes.util.Setting;
import com.herocraftonline.dev.heroes.util.Util;

public class SkillBladegrasp extends ActiveSkill {

	private String applyText;
	private String expireText;
	private String parryText;
	private String parrySkillText;

	public SkillBladegrasp(Heroes plugin) {
		super(plugin, "Bladegrasp");
		setDescription("Blocks incoming melee damage");
		setUsage("/skill bladegrasp");
		setArgumentRange(0, 0);
		setIdentifiers("skill bladegrasp", "skill bgrasp");
		setTypes(SkillType.PHYSICAL, SkillType.BUFF);

		registerEvent(Type.CUSTOM_EVENT, new SkillEntityListener(this), Priority.Normal);
	}

	@Override
	public ConfigurationNode getDefaultConfig() {
		ConfigurationNode node = super.getDefaultConfig();
		node.setProperty(Setting.DURATION.node(), 5000);
		node.setProperty(Setting.APPLY_TEXT.node(), "%hero% tightened his grip!");
		node.setProperty(Setting.EXPIRE_TEXT.node(), "%hero% loosened his grip!");
		node.setProperty("parry-text", "%hero% parried an attack!");
		node.setProperty("parry-skill-text", "%hero% has parried %target%'s %skill%.");
		node.setProperty(Setting.CHANCE_LEVEL.node(), .02);
		return node;
	}

	@Override
	public void init() {
		super.init();
		applyText = getSetting(null, Setting.APPLY_TEXT.node(), "%hero% tightened his grip!").replace("%hero%", "$1");
		expireText = getSetting(null, Setting.EXPIRE_TEXT.node(), "%hero% loosened his grip!").replace("%hero%", "$1");
		parryText = getSetting(null, "parry-text", "%hero% parried an attack!").replace("%hero%", "$1");
		parrySkillText = getSetting(null, "parry-skill-text", "%hero% has parried %target%'s %skill%.").replace("$1","%hero$").replace("$2","%target%").replace("$3","%skill");
	}

	@Override
	public SkillResult use(Hero hero, String[] args) {
		broadcastExecuteText(hero);
		int duration = getSetting(hero, Setting.DURATION.node(), 5000, false);
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

	public class SkillEntityListener extends HeroesEventListener {

		private Skill skill;
		
		SkillEntityListener(Skill skill) {
			this.skill = skill;
		}
		
		@Override
		public void onWeaponDamage(WeaponDamageEvent event) {
			Heroes.debug.startTask("HeroesSkillListener");
			// Ignore cancelled damage events & 0 damage events for Spam Control
			if (event.getDamage() == 0 || event.isCancelled() || !(event.getEntity() instanceof Player)) {
				Heroes.debug.stopTask("HeroesSkillListener");
				return;
			}

			Player player = (Player) event.getEntity();
			Hero hero = plugin.getHeroManager().getHero(player);
			if (hero.hasEffect(getName())) {
				double parryChance = getSetting(hero, "chance-per-level", .02, false) * hero.getLevel(skill);
				if (Util.rand.nextDouble() > parryChance)
					return;

				event.setCancelled(true);
				String message = Messaging.parameterizeMessage(parryText, player.getDisplayName());
				Messaging.send(player, message);
				if (event.getDamager() instanceof Player) {
					Messaging.send((Player) event.getDamager(), message);
				}
			}

			Heroes.debug.stopTask("HeroesSkillListener");
		}

		@Override
		public void onSkillDamage(SkillDamageEvent event) {
			Heroes.debug.startTask("HeroesSkillListener");
			// Ignore cancelled damage events & 0 damage events for Spam Control
			if (event.getDamage() == 0 || event.isCancelled() || !event.getSkill().isType(SkillType.PHYSICAL) || !(event.getEntity() instanceof Player)) {
				Heroes.debug.stopTask("HeroesSkillListener");
				return;
			}
			Player player = (Player) event.getEntity();
			Hero hero = plugin.getHeroManager().getHero(player);
			if (hero.hasEffect(getName())) {
				double parryChance = getSetting(hero, "chance-per-level", .02, false) * hero.getLevel(event.getSkill());
				if (Util.rand.nextDouble() > parryChance)
					return;

				event.setCancelled(true);
				String message = Messaging.parameterizeMessage(parrySkillText, player.getDisplayName(), event.getDamager().getPlayer().getDisplayName(), event.getSkill().getName());
				Messaging.send(player, message);
				Messaging.send(event.getDamager().getPlayer(), message);
				
			}
			Heroes.debug.stopTask("HeroesSkillListener");
		}
	}
}
