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
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;

public class SkillQuiveringPalm extends TargettedSkill {

	// Default skill values
	private final int defDamage = 50;
	private final double defDamageMultiplier = 1.2;
	private final int defDuration = 2500;

	private final String skillText = "§7[§2Skill§7] ";		// Used to add "[Skill]" text to all skill related messages

	private final String defApplyText = skillText + "%target% is weakened by a §lQuiveringPalm!";
	private final String defExpireText = skillText + "%target% has recovered from the effects of the §lQuiveringPalm!";

	public SkillQuiveringPalm(Heroes plugin) {
		super(plugin, "QuiveringPalm");
		setDescription("Strike your target with a Quivering Palm dealing $1 damage and weakening the target, causing them to take $2% increased melee damage for $3 seconds.");
		setUsage("/skill quiveringpalm");
		setArgumentRange(0, 0);
		setIdentifiers("skill quiveringpalm");
		setTypes(SkillType.PHYSICAL, SkillType.DAMAGING, SkillType.HARMFUL, SkillType.DEBUFF);
		
		Bukkit.getServer().getPluginManager().registerEvents(new QuiveringPalmListener(this), plugin);
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.DAMAGE.node(), defDamage);
		node.set(SkillSetting.DURATION.node(), defDuration);
		node.set(SkillSetting.APPLY_TEXT.node(), defApplyText);
		node.set(SkillSetting.EXPIRE_TEXT.node(), defExpireText);
		node.set("damage-multiplier", defDamageMultiplier);
		//node.set(SkillSetting.MAX_DISTANCE.node(), defMaxDistance);
		return node;
	}

	@Override
	public String getDescription(Hero hero) {
		int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, defDamage, false);
		long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, defDuration, false) / 1000;
		int damageMultiplier = (int) ((SkillConfigManager.getUseSetting(hero, this, "damage-multiplier", defDamageMultiplier, false) - 1) * 100);
		return getDescription().replace("$1", damage + "").replace("$2", damageMultiplier + "").replace("$3", duration + "");
	}

	@Override
	public SkillResult use(Hero hero, LivingEntity target, String[] args) {
		Player player = hero.getPlayer();

		int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, defDamage, false);

		// Damage the target
		addSpellTarget(target, hero);
		damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);

		// Play Sound
		hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.HURT, 0.8F, 1.0F);

		// Play Effect
		// CODE HERE

		// Display use Message
		broadcastExecuteText(hero, target);

		// Prep variables
		double damageMultiplier = SkillConfigManager.getUseSetting(hero, this, "damage-multiplier", defDamageMultiplier, false);
		long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, defDuration, false);

		String applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, defApplyText).replace("%target%", "$1");
		String expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, defExpireText).replace("%target%", "$1");

		// Add the debuff to the target
		CharacterTemplate targCT = this.plugin.getCharacterManager().getCharacter(target);
		QuiveringPalmEffect qpEffect = new QuiveringPalmEffect(this, duration, damageMultiplier, hero.getPlayer(), applyText, expireText);
		targCT.addEffect(qpEffect);

		return SkillResult.NORMAL;
	}

	private class QuiveringPalmListener implements Listener {
		private final Skill skill;

		public QuiveringPalmListener(Skill skill) {
			this.skill = skill;
		}

		// Alter damage dealt by players when they are under the effect of quivering palm
		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onWeaponDamage(WeaponDamageEvent event) {

			if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
				return;
			}

			// Ensure that the target is a living entity
			Entity targEnt = event.getEntity();
			if (!(targEnt instanceof LivingEntity))
				return;

			// Check to make sure that the target has the quivering palm effect
			CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter((LivingEntity) targEnt);
			if (!targetCT.hasEffect("QuiveringPalmEffect"))
				return;

			// Get the damage multiplier
			QuiveringPalmEffect qpEffect = (QuiveringPalmEffect) targetCT.getEffect("QuiveringPalmEffect");
			double damageMultiplier = qpEffect.getDamageModifier();

			// Alter the damage being dealt to the target
			int damage = (int) (event.getDamage() * damageMultiplier);
			event.setDamage(damage);

			return;
		}
	}

	// Effect required for implementing an internal cooldown on rune application
	private class QuiveringPalmEffect extends ExpirableEffect {

		private final double damageMultiplier;

		private final String applyText;
		private final String expireText;

		private final Player applier;

		public QuiveringPalmEffect(Skill skill, long duration, double damageMultipler, Player applier, String applyText, String expireText) {
			super(skill, "QuiveringPalmEffect", duration);

			this.damageMultiplier = damageMultipler;

			this.applier = applier;
			this.applyText = applyText;
			this.expireText = expireText;

			this.types.add(EffectType.DISPELLABLE);
			this.types.add(EffectType.HARMFUL);
			this.types.add(EffectType.PHYSICAL);
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
			broadcast(player.getLocation(), applyText, player.getDisplayName());
		}

		@Override
		public void removeFromMonster(Monster monster) {
			super.removeFromMonster(monster);
			broadcast(monster.getEntity().getLocation(), expireText, Messaging.getLivingEntityName(monster), applier.getDisplayName());
		}

		@Override
		public void removeFromHero(Hero hero) {
			super.removeFromHero(hero);
			final Player player = hero.getPlayer();
			broadcast(player.getLocation(), expireText, player.getDisplayName());
		}

		public double getDamageModifier() {
			return damageMultiplier;
		}
	}
}
