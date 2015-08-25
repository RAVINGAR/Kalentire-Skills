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
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class SkillEndlessNightmare extends SkillBaseSpike {

	private static final ParticleEffect PARTICLE = ParticleEffect.SPELL_MOB;

	private static final String SLOW_AMPLIFIER = "slow-amplifier";
	private static final String HUNGAR_AMPLIFIER = "hungar-amplifier";

	public SkillEndlessNightmare(Heroes plugin) {
		super(plugin, "EndlessNightmare");
		setDescription("Impales the target with a spike of chaos casting them into a nightmarish state for $1 seconds, dealing $2 damage.");
		setUsage("/skill endlessnightmare");
		setIdentifiers("skill endlessnightmare");
		setArgumentRange(0, 0);
		setTypes(SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.NO_SELF_TARGETTING, SkillType.MOVEMENT_SLOWING, SkillType.BLINDING);
	}

	@Override
	public String getDescription(Hero hero) {
		int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false) / 1000;

		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 250d, false);
		damage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 1d, false) * hero.getAttributeValue(AttributeType.INTELLECT);

		return getDescription().replace("$1", duration + "").replace("$2", damage + "");
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.MAX_DISTANCE.node(), 8d);
		node.set(SkillSetting.DAMAGE.node(), 250d);
		node.set(SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT.node(), 1d);

		node.set(SPIKE_HEIGHT, 3d);
		node.set(DOES_KNOCK_UP, true);
		node.set(KNOCK_UP_STRENGTH, 0.6);

		node.set(SkillSetting.DURATION.node(), 5000);
		node.set(SLOW_AMPLIFIER, 1);
		node.set(HUNGAR_AMPLIFIER, 1);

		return node;
	}

	@Override
	public SkillResult use(Hero hero, LivingEntity target, String[] strings) {
		Player player = hero.getPlayer();

		if (damageCheck(player, target)) {

			broadcastExecuteText(hero, target);

			double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 250d, false);
			damage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 1d, false) * hero.getAttributeValue(AttributeType.INTELLECT);
			damageEntity(target, player, damage, EntityDamageEvent.DamageCause.MAGIC, false);

			int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
			int slowAmplifier = SkillConfigManager.getUseSetting(hero, this, SLOW_AMPLIFIER, 1, false);
			int hungerAmplifier = SkillConfigManager.getUseSetting(hero, this, HUNGAR_AMPLIFIER, 1, false);

			CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
			EndlessNightmareEffect effect = new EndlessNightmareEffect(player, duration, slowAmplifier, hungerAmplifier);
			targetCT.addEffect(effect);

			double spikeHeight = SkillConfigManager.getUseSetting(hero, this, SPIKE_HEIGHT, 3d, false);
			renderSpike(target.getLocation(), spikeHeight, BLOCK_SPIKE_RADIUS, PARTICLE);

			if (SkillConfigManager.getUseSetting(hero, this, DOES_KNOCK_UP, true)) {
				Vector knockUpVector = new Vector(0, SkillConfigManager.getUseSetting(hero, this, KNOCK_UP_STRENGTH, 0.6, false), 0);
				target.setVelocity(target.getVelocity().add(knockUpVector));
			}

			target.getWorld().playSound(target.getLocation(), Sound.GHAST_MOAN, 1, 0.0001f);

			return SkillResult.NORMAL;
		} else {
			return SkillResult.INVALID_TARGET;
		}
	}

	public class EndlessNightmareEffect extends ExpirableEffect {

		public EndlessNightmareEffect(Player applier, int duration, int slowAmplifier, int hungerAmplifier) {
			super(SkillEndlessNightmare.this, SkillEndlessNightmare.this.getName(), applier, duration);

			types.add(EffectType.HARMFUL);
			types.add(EffectType.DISPELLABLE);
			types.add(EffectType.MAGIC);

			types.add(EffectType.SLOW);
			types.add(EffectType.BLIND);
			types.add(EffectType.HUNGER);
			types.add(EffectType.CONFUSION);
			types.add(EffectType.NAUSEA);

			addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 1000, slowAmplifier, true, false), false);
			addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 1000, 1, true, false), false);
			addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 1000, hungerAmplifier, true, false), false);
			addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 1000, 1, true, false), false);
		}
	}
}
