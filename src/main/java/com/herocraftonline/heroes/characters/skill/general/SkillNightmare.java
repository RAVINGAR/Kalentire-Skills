package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class SkillNightmare extends SkillBaseSpike {

    private static final Particle PARTICLE = Particle.SPELL_MOB_AMBIENT;

    public SkillNightmare(final Heroes plugin) {
        super(plugin, "Nightmare");
        setDescription("Impales the target with a spike of darkness casting them into a nightmarish state, dealing $1 damage per $2 second(s) for $3 second(s).");
        setUsage("/skill nightmare");
        setIdentifiers("skill nightmare");
        setArgumentRange(0, 0);
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_DARK, SkillType.AGGRESSIVE, SkillType.NO_SELF_TARGETTING, SkillType.BLINDING);
    }

    @Override
    public String getDescription(final Hero hero) {
        final int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1000, false);
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 250d, false);
        damage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 1d, false) * hero.getAttributeValue(AttributeType.INTELLECT);

        return getDescription()
                .replace("$1", Util.decFormat.format(damage))
                .replace("$2", Util.decFormat.format((double) period / 1000))
                .replace("$3", Util.decFormat.format((double) duration / 1000));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 8d);
        node.set(SkillSetting.DAMAGE_TICK.node(), 250d);
        node.set(SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT.node(), 1d);

        node.set(SPIKE_HEIGHT_NODE, 3d);
        node.set(DOES_KNOCK_UP_NODE, true);
        node.set(KNOCK_UP_STRENGTH_NODE, 0.6);

        node.set(SkillSetting.DURATION.node(), 5000);
        node.set(SkillSetting.PERIOD.node(), 1000);

        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has been cast into a nightmare!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has awoken from a nightmare!");

        return node;
    }

    @Override
    public SkillResult use(final Hero hero, final LivingEntity target, final String[] strings) {
        final Player player = hero.getPlayer();

        if (damageCheck(player, target)) {

            broadcastExecuteText(hero, target);

            double tickDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 250d, false);
            tickDamage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 1d, false) * hero.getAttributeValue(AttributeType.INTELLECT);

            final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
            final int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1000, false);

            final CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
            final String applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has been cast into a nightmare!").replace("%target%", "$1").replace("$target$", "$1");
            final String expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has awoken from a nightmare!").replace("%target%", "$1").replace("$target$", "$1");
            final NightmareEffect effect = new NightmareEffect(player, period, duration, tickDamage);
            effect.setApplyText(applyText);
            effect.setRemoveText(expireText);
            targetCT.addEffect(effect);

            final double spikeHeight = SkillConfigManager.getUseSetting(hero, this, SPIKE_HEIGHT_NODE, 3d, false);
            renderSpike(target.getLocation(), spikeHeight, BLOCK_SPIKE_RADIUS, PARTICLE, Color.BLACK);

            if (SkillConfigManager.getUseSetting(hero, this, DOES_KNOCK_UP_NODE, true)) {
                final Vector knockUpVector = new Vector(0, SkillConfigManager.getUseSetting(hero, this, KNOCK_UP_STRENGTH_NODE, 0.6, false), 0);
                target.setVelocity(target.getVelocity().add(knockUpVector));
            }

//			target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_PIGMAN_HURT, 0.2f, 0.00001f);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ZOMBIFIED_PIGLIN_HURT, 0.2f, 0.00001f);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GHAST_WARN, 0.2f, 0.00001f);

            return SkillResult.NORMAL;
        } else {
            return SkillResult.INVALID_TARGET;
        }
    }

    public class NightmareEffect extends PeriodicDamageEffect {

        public NightmareEffect(final Player applier, final int period, final int duration, final double tickDamage) {
            super(SkillNightmare.this, SkillNightmare.this.getName(), applier, period, duration, tickDamage);

            types.add(EffectType.MAGIC);
            types.add(EffectType.HARMFUL);
            types.add(EffectType.DISPELLABLE);

            types.add(EffectType.DAMAGING);
            types.add(EffectType.BLIND);
            types.add(EffectType.NAUSEA);

            addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration / 50 + 20, 1, true, false), false);
            addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, duration / 50 + 20, 1, true, false), false);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            // hmm might have double up of messages (since its sent to this player in super)
            broadcast(player.getLocation(), "    " + getApplyText(), player.getName());
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            // hmm might have double up of messages (since its sent to this player in super)
            broadcast(player.getLocation(), "    " + getRemoveText(), player.getName());
        }
    }
}
