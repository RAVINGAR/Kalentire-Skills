package com.herocraftonline.heroes.characters.skill.general;

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
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseSpike;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class SkillEndlessNightmare extends SkillBaseSpike {

    private static final Particle PARTICLE = Particle.SPELL_MOB_AMBIENT;

    private static final String SLOW_AMPLIFIER = "slow-amplifier";
    private static final String HUNGER_AMPLIFIER = "hunger-amplifier";

    public SkillEndlessNightmare(final Heroes plugin) {
        super(plugin, "EndlessNightmare");
        setDescription("Impales the target with a spike of chaos casting them into a nightmarish state for $1 second(s), dealing $2 damage.");
        setUsage("/skill endlessnightmare");
        setIdentifiers("skill endlessnightmare");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.NO_SELF_TARGETTING, SkillType.MOVEMENT_SLOWING, SkillType.BLINDING);
    }

    @Override
    public String getDescription(final Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 250d, false);
        damage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1d, false) * hero.getAttributeValue(AttributeType.INTELLECT);

        return getDescription()
                .replace("$1", Util.decFormat.format((double) duration / 1000))
                .replace("$2", Util.decFormat.format(damage));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 8d);
        node.set(SkillSetting.DAMAGE.node(), 250d);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1d);

        node.set(SPIKE_HEIGHT_NODE, 3d);
        node.set(DOES_KNOCK_UP_NODE, true);
        node.set(KNOCK_UP_STRENGTH_NODE, 0.6);

        node.set(SkillSetting.DURATION.node(), 5000);
        node.set(SLOW_AMPLIFIER, 1);
        node.set(HUNGER_AMPLIFIER, 1);

        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has been cast into an endless nightmare!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has awoken from an endless nightmare!");

        return node;
    }

    @Override
    public SkillResult use(final Hero hero, final LivingEntity target, final String[] strings) {
        final Player player = hero.getPlayer();

        if (damageCheck(player, target)) {

            broadcastExecuteText(hero, target);

            double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 250d, false);
            damage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1d, false) * hero.getAttributeValue(AttributeType.INTELLECT);
            damageEntity(target, player, damage, EntityDamageEvent.DamageCause.MAGIC, false);

            final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
            final int slowAmplifier = SkillConfigManager.getUseSetting(hero, this, SLOW_AMPLIFIER, 1, false);
            final int hungerAmplifier = SkillConfigManager.getUseSetting(hero, this, HUNGER_AMPLIFIER, 1, false);

            final CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
            final String applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has been cast into an endless nightmare!").replace("%target%", "$1").replace("$target$", "$1");
            final String expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has awoken from an endless nightmare!").replace("%target%", "$1").replace("$target$", "$1");
            final EndlessNightmareEffect effect = new EndlessNightmareEffect(player, duration, slowAmplifier, hungerAmplifier);
            effect.setApplyText(applyText);
            effect.setExpireText(expireText);
            targetCT.addEffect(effect);

            final double spikeHeight = SkillConfigManager.getUseSetting(hero, this, SPIKE_HEIGHT_NODE, 3d, false);
            renderSpike(target.getLocation(), spikeHeight, BLOCK_SPIKE_RADIUS, PARTICLE, Color.BLACK);

            if (SkillConfigManager.getUseSetting(hero, this, DOES_KNOCK_UP_NODE, true)) {
                final Vector knockUpVector = new Vector(0, SkillConfigManager.getUseSetting(hero, this, KNOCK_UP_STRENGTH_NODE, 0.6, false), 0);
                target.setVelocity(target.getVelocity().add(knockUpVector));
            }

            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PIGLIN_HURT, 0.2f, 0.00001f);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GHAST_WARN, 0.2f, 0.00001f);

            return SkillResult.NORMAL;
        } else {
            return SkillResult.INVALID_TARGET;
        }
    }

    public class EndlessNightmareEffect extends ExpirableEffect {

        public EndlessNightmareEffect(final Player applier, final int duration, final int slowAmplifier, final int hungerAmplifier) {
            super(SkillEndlessNightmare.this, SkillEndlessNightmare.this.getName(), applier, duration);

            types.add(EffectType.MAGIC);
            types.add(EffectType.HARMFUL);
            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.STAMINA_REGEN_FREEZING);
            types.add(EffectType.STAMINA_DECREASING);

            types.add(EffectType.SLOW);
            types.add(EffectType.BLIND);
            types.add(EffectType.HUNGER);
            types.add(EffectType.NAUSEA);

            addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration / 50 + 20, slowAmplifier, true, false));
            addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration / 50 + 20, 1, true, false));
            addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, duration / 50 + 20, hungerAmplifier, true, false));
            addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, duration / 50 + 20, 1, true, false));
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + getApplyText(), player.getName());
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + getExpireText(), player.getName());
		}
	}
}
