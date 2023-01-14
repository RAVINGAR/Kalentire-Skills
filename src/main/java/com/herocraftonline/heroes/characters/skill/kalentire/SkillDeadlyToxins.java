package com.herocraftonline.heroes.characters.skill.kalentire;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillDeadlyToxins extends ActiveSkill {

	private final String applyText = "§c%target% has been poisoned by a deadly toxin!";
	private final String expireText = "§c%target% has recovered from the deadly toxin!";

	private final BlockData data;
	public SkillDeadlyToxins(Heroes plugin) {
		super(plugin, "DeadlyToxins");
		setDescription("You prepare an ailment to poison your weapons which expires after $1 second(s), also extending any previous preparations by $1 second(s)." +
				"Any target hit is poisoned for the remaining duration of the preparation stack, and will take $2 damage every second. " +
				"Other preparations can postpone the expiry of this preparation.");
		setArgumentRange(0, 0);
		setUsage("/skill deadlytoxins");
		setIdentifiers("skill deadlytoxins");
		setTypes(SkillType.BUFFING, SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_POISON);

		data = Bukkit.createBlockData(Material.SLIME_BLOCK);
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
		node.set("toxin-apply-text", applyText);
		node.set("toxin-expire-text", expireText);

		return node;		
	}

	@Override
	public SkillResult use(Hero hero, String[] args) {
		long duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, 7000, false);
		double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE_TICK, 5, false);

		String apply = SkillConfigManager.getUseSetting(hero, this, SkillSetting.APPLY_TEXT, "You poison your weapons!");
		String toxinApply = SkillConfigManager.getUseSetting(hero, this, "toxin-apply-text", applyText);
		String toxinExpire = SkillConfigManager.getUseSetting(hero, this, "toxin-expire-text", expireText);

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
			tickBoth(monster);
		}

		@Override
		public void tickHero(Hero hero) {
			tickBoth(hero);
		}

		private void tickBoth(CharacterTemplate character) {
			Location location = character.getEntity().getLocation().add(0, 1, 0);
			location.getWorld().spawnParticle(Particle.BLOCK_CRACK, location, 4, 0.1, 0.1, 0.1, data);
			addSpellTarget(character.getEntity(), applier);
			damageEntity(character.getEntity(), applier.getPlayer(), dmg, DamageCause.POISON, 0.0f);
		}
	}
}
