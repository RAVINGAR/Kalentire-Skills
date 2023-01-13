package com.herocraftonline.heroes.characters.skill.kalentire;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillDeadlyToxins extends ActiveSkill {
	public SkillDeadlyToxins(Heroes plugin) {
		super(plugin, "DeadlyToxins");
		setDescription("You prepare an ailment to poison your weapons which expires after $1 second(s), also extending any previous preparations by $1 second(s)." +
				"Any target hit is poisoned for the remaining duration of the preparation stack, and will take $2 damage every second. " +
				"Other preparations can postpone the expiry of this preparation.");
		setArgumentRange(0, 0);
		setUsage("/skill deadlytoxins");
		setIdentifiers("skill deadlytoxins");
		setTypes(SkillType.BUFFING, SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_POISON);
	}

	@Override
	public String getDescription(Hero hero)
	{
		long duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, 14000, false);
		String formattedDuration = String.valueOf(duration / 1000);
		double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, 5, false);

		return getDescription().replace("$1", formattedDuration).replace("$2", damage + "");
	}

	@Override
	public ConfigurationSection getDefaultConfig()
	{		
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.DURATION.node(), 7000);
		node.set(SkillSetting.DAMAGE_TICK.node(), 5);
		node.set(SkillSetting.APPLY_TEXT.node(), "§7You poison your weapons!");
		node.set("toxin-apply-text", "You have been poisoned by a deadly toxin!");
		node.set("toxin-expire-text", "You are no longer poisoned.");

		return node;		
	}

	@Override
	public SkillResult use(Hero hero, String[] args) {
		long duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, 7000, false);
		double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE_TICK, 5, false);

		String apply = SkillConfigManager.getUseSetting(hero, this, SkillSetting.APPLY_TEXT, "You poison your weapons!");
		String toxinApply = SkillConfigManager.getUseSetting(hero, this, "toxin-apply-text", "§7You have been poisoned by a deadly toxin!").replace("%hero%", hero.getName());
		String toxinExpire = SkillConfigManager.getUseSetting(hero, this, "toxin-expire-text", "§7You are no longer poisoned.");

		DeadlyToxinsEffect toxinEffect = new DeadlyToxinsEffect(this, hero, duration, damage, toxinApply, toxinExpire);
		plugin.getServer().getPluginManager().callEvent(new SkillAssassinsGuile.EffectPreparationEvent(hero, toxinEffect, duration, apply));

		return SkillResult.NORMAL;
	}

	public class DeadlyToxinsEffect extends PeriodicExpirableEffect {
		private final Hero applier;
		private final double dmg;

		public DeadlyToxinsEffect(Skill skill, Hero applier, long duration, double dmgPerTick, String applyText, String expireText) {
			super(skill, "DeadlyToxins", applier.getPlayer(), 1000, duration, applyText, expireText);
			this.dmg = dmgPerTick;
			this.applier = applier;

			types.add(EffectType.DISPELLABLE);
			types.add(EffectType.POISON);
			types.add(EffectType.HARMFUL);
		}

		@Override
		public void tickMonster(Monster monster) {
			addSpellTarget(monster.getEntity(), applier);
			damageEntity(monster.getEntity(), applier.getPlayer(), dmg, DamageCause.POISON, 0.0f);
		}

		@Override
		public void tickHero(Hero hero) {
			addSpellTarget(hero.getEntity(), applier);
			damageEntity(hero.getEntity(), applier.getPlayer(), dmg, DamageCause.POISON, 0.0f);
		}
	}
}
