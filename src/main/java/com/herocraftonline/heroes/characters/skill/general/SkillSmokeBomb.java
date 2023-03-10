package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.InvisibleEffect;
import com.herocraftonline.heroes.characters.effects.common.SpeedEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class SkillSmokeBomb extends ActiveSkill {
    private String applyText;
    private String expireText;

    public SkillSmokeBomb(final Heroes plugin) {
        super(plugin, "SmokeBomb");
        setDescription("Vanish in a smokebomb! " +
                "You will not be visible to other players for the next $1 second(s). " +
                "You will also break free of " + ChatColor.ITALIC + "any" + ChatColor.RESET + " movement impeding effect, even those that are not normally dispellable. " +
                "If you break free of a movement imparing effect, you will be given a burst of speed as well! " +
                "Taking damage or using abilities will cause you to reappear.");
        setUsage("/skill smokebomb");
        setArgumentRange(0, 0);
        setIdentifiers("skill smokebomb", "skill smoke");
        setNotes("Note: Interacting with anything removes the effect.");
        setNotes("Note: Taking damage removes the effect.");
        setNotes("Note: Using un-stealthy skills while invisible will remove the effect.");
        setTypes(SkillType.ABILITY_PROPERTY_ILLUSION, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.MOVEMENT_PREVENTION_COUNTERING, SkillType.STEALTHY);
    }

    @Override
    public String getDescription(final Hero hero) {
        final int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);

        return getDescription().replace("$1", Util.decFormat.format(duration / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DURATION.node(), 5500);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "Someone vanished in a cloud of smoke!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has reappeared!");
        config.set("speed-amplifier-on-effect-break", 2);
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                        ChatComponents.GENERIC_SKILL + "Someone vanished in a cloud of smoke!")
                .replace("%hero%", "$1").replace("$hero$", "$1");

        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                        ChatComponents.GENERIC_SKILL + "%hero% has reappeared!")
                .replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        final long duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        escape(hero, player, duration);

        //Util.playClientEffect(player, Particle.EXPLOSION_HUGE.toString(), new Vector(0, 0, 0), 1F, 10, true);
        player.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, player.getEyeLocation(), 10, 0.5, 0.3, 0.5, 1F);
        hero.addEffect(new SmokeEffect(this, player, duration));

        return SkillResult.NORMAL;
    }

    public void escape(final Hero hero, final Player player, final long duration) {
        boolean removed = false;
        for (final Effect effect : hero.getEffects()) {
            if (effect.isType(EffectType.SLOW) ||
                    effect.isType(EffectType.VELOCITY_DECREASING) ||
                    effect.isType(EffectType.WALK_SPEED_DECREASING) ||
                    effect.isType(EffectType.ROOT)) {

                removed = true;
                hero.removeEffect(effect);
            }
        }

        if (removed) {
            final int amplifier = SkillConfigManager.getUseSetting(hero, this, "speed-amplifier-on-effect-break", 2, false);
            if (duration > 0 && amplifier > 0) {
                hero.addEffect(new SpeedEffect(this, getName(), player, duration, amplifier, null, null));
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_DEATH, 0.8F, 1.0F);
            }
        }
    }

    public class SmokeEffect extends InvisibleEffect {
        SmokeEffect(final Skill skill, final Player applier, final long duration) {
            super(skill, "Smoked", applier, duration, null, null);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);

            final Player player = hero.getPlayer();
            if (applyText != null && applyText.length() > 0) {
                // Override the standard invis effect message display so that we actually display a message to nearby players
                //      even though we have a "silent actions" effect type.
                if (hero.isInCombat()) {
                    broadcast(player.getLocation(), "    " + applyText, player.getName());
                }
            }
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);

            final Player player = hero.getPlayer();
            if (expireText != null && expireText.length() > 0) {
                // Override the standard invis effect message display so that we actually display a message to nearby players
                //      even though we have a "silent actions" effect type.
                if (hero.isInCombat()) {
                    broadcast(player.getLocation(), expireText, player.getName());
                } else {
                    player.sendMessage(expireText.replace("$1", player.getName()));
                }
            }
        }
    }
}
