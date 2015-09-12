package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.Color;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class SkillSoulSpike extends SkillBaseSpike {

	private static final ParticleEffect PARTICLE = ParticleEffect.SPELL_MOB_AMBIENT;

	private static final String SLOW_AMPLIFIER = "slow-amplifier";
	private static final String HUNGER_AMPLIFIER = "hunger-amplifier";

	public SkillSoulSpike(Heroes plugin) {
		super(plugin, "SoulSpike");
		setDescription("Impales the target's soul with a spike of negative energy, casting them into a soul shaken state for $1 seconds, dealing $2 damage. $3 $4");
		setUsage("/skill soulspike");
		setIdentifiers("skill soulspike");
		setArgumentRange(0, 0);
		setTypes(SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.NO_SELF_TARGETTING, SkillType.MOVEMENT_SLOWING, SkillType.BLINDING, SkillType.ABILITY_PROPERTY_DARK);
	}

	@Override
	public String getDescription(Hero hero) {
		int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false) / 1000;

		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 250d, false);
		damage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 1d, false) * hero.getAttributeValue(AttributeType.INTELLECT);

		int mana = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 0, false);
		long cooldown = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 0, false);

		return getDescription()
				.replace("$1", duration + "")
				.replace("$2", Util.decFormat.format(damage))
				.replace("$3", mana > 0 ? "Mana: " + mana : "")
				.replace("$4", cooldown > 0 ? "C: " + Util.decFormat.format((double) cooldown / 1000) : "");
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.MAX_DISTANCE.node(), 8d);
		node.set(SkillSetting.DAMAGE.node(), 250d);
		node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1d);

		node.set(SPIKE_HEIGHT_NODE, 3d);
		node.set(DOES_KNOCK_UP_NODE, true);
		node.set(KNOCK_UP_STRENGTH_NODE, 0.6);

		node.set(SkillSetting.DURATION.node(), 5000);
		node.set(SLOW_AMPLIFIER, 1);
		node.set(HUNGER_AMPLIFIER, 1);

		node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target%'s soul was spiked!");
		node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target%'s soul is no longer spiked!");

		return node;
	}

	@Override
	public SkillResult use(Hero hero, LivingEntity target, String[] strings) {
		Player player = hero.getPlayer();

		if (damageCheck(player, target)) {

			broadcastExecuteText(hero, target);

			double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 250d, false);
			damage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1d, false) * hero.getAttributeValue(AttributeType.INTELLECT);
			damageEntity(target, player, damage, EntityDamageEvent.DamageCause.MAGIC, false);

			int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
			int slowAmplifier = SkillConfigManager.getUseSetting(hero, this, SLOW_AMPLIFIER, 1, false);
			int hungerAmplifier = SkillConfigManager.getUseSetting(hero, this, HUNGER_AMPLIFIER, 1, false);

			CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
			String applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target%'s soul was spiked!").replace("%target%", "$1");
			String expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target%'s soul is no longer spiked!").replace("%target%", "$1");
			SkillSoulSpikeEffect effect = new SkillSoulSpikeEffect(player, duration, slowAmplifier, hungerAmplifier);
			effect.setApplyText(applyText);
			effect.setExpireText(expireText);
			targetCT.addEffect(effect);

			double spikeHeight = SkillConfigManager.getUseSetting(hero, this, SPIKE_HEIGHT_NODE, 3d, false);
			renderSpike(target.getLocation(), spikeHeight, BLOCK_SPIKE_RADIUS, PARTICLE, Color.fromRGB(70, 0, 130));//TODO Testing

			if (SkillConfigManager.getUseSetting(hero, this, DOES_KNOCK_UP_NODE, true)) {
				Vector knockUpVector = new Vector(0, SkillConfigManager.getUseSetting(hero, this, KNOCK_UP_STRENGTH_NODE, 0.6, false), 0);
				target.setVelocity(target.getVelocity().add(knockUpVector));
			}

			target.getWorld().playSound(target.getLocation(), Sound.ZOMBIE_PIG_HURT, 0.2f, 0.00001f);

			return SkillResult.NORMAL;
		} else {
			return SkillResult.INVALID_TARGET;
		}
	}

	public class SkillSoulSpikeEffect extends ExpirableEffect {

		public SkillSoulSpikeEffect(Player applier, int duration, int slowAmplifier, int hungerAmplifier) {
			super(SkillSoulSpike.this, SkillSoulSpike.this.getName(), applier, duration);

			types.add(EffectType.MAGIC);
			types.add(EffectType.HARMFUL);
			types.add(EffectType.DISPELLABLE);
			types.add(EffectType.STAMINA_REGEN_FREEZING);
			types.add(EffectType.STAMINA_DECREASING);

			types.add(EffectType.SLOW);
			types.add(EffectType.BLIND);
			types.add(EffectType.HUNGER);
			types.add(EffectType.NAUSEA);

			addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration / 50 + 20, slowAmplifier, true, false), false);
			addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration / 50 + 20, 1, true, false), false);
			addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, duration / 50 + 20, hungerAmplifier, true, false), false);
			addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, duration / 50 + 20, 1, true, false), false);
		}

		@Override
		public void applyToHero(Hero hero) {
			super.applyToHero(hero);
			Player player = hero.getPlayer();
			broadcast(player.getLocation(), "    " + getApplyText(), player.getName());
		}

		@Override
		public void removeFromHero(Hero hero) {
			super.removeFromHero(hero);
			Player player = hero.getPlayer();
			broadcast(player.getLocation(), "    " + getExpireText(), player.getName());
		}
	}
}
