package com.herocraftonline.heroes.characters.skill.remastered.bard;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.EffectAddEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.PeriodicHealEffect;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created By MysticMight 2021
 */

public class SkillBoomingVoice extends PassiveSkill {

    private static final String boomingVoiceHealingEffectName = "BoomingVoiceHealing";

    public SkillBoomingVoice(Heroes plugin) {
        super(plugin, "BoomingVoice");
        setDescription("While a song is active, you passively regain $1 health every $2 second(s).");
        setTypes(SkillType.BUFFING, SkillType.ABILITY_PROPERTY_LIGHT, SkillType.ABILITY_PROPERTY_SONG);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double health = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.HEALING_TICK, false);
        long periodMilliseconds = SkillConfigManager.getUseSettingInt(hero, this, SkillSetting.PERIOD.node(), false);
        return getDescription()
                .replace("$1", Util.decFormat.format(health))
                .replace("$2", Util.decFormat.format(periodMilliseconds / 1000));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.APPLY_TEXT.node(), "");
        config.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        config.set(SkillSetting.HEALING_TICK.node(), 20.0);
        config.set(SkillSetting.HEALING_TICK_INCREASE_PER_CHARISMA.node(), 0.0);
        config.set("tick-healing-per-level", 0.0);
        config.set(SkillSetting.PERIOD.node(), 1000);
        config.set("supported-song-buff-effects", new String[] {"Accelerando", "Battlesong", "MelodicBinding", "WarsongSong"});
        return config;
    }

    @Override
    public void apply(Hero hero) {
        // Note we don't want the default passive effect, we're making our own with a custom constructor.
        // Also note the default unapply method does what we want (removes effect of skill name)
        double healthTick = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.HEALING_TICK, false);
        long period = SkillConfigManager.getUseSettingInt(hero, this, SkillSetting.PERIOD, false);

        List<String> songEffectNamesList = SkillConfigManager.getUseSettingStringList(hero, this, "supported-song-buff-effects");
        Set<String> supportedSongBuffEffectNames = new HashSet<>(songEffectNamesList);
        hero.addEffect(new BoomingVoiceEffect(this, hero.getPlayer(), supportedSongBuffEffectNames, healthTick, period));
    }

    public class BoomingVoiceEffect extends PassiveEffect {
        private final Set<String> supportedSongBuffEffectNames;
        private final double healthTick;
        private final long period;

        protected BoomingVoiceEffect(Skill skill, Player applier, Set<String> supportedSongBuffEffectNames, double healthTick, long period) {
            super(skill, applier, new EffectType[] {EffectType.BENEFICIAL, EffectType.MAGIC});
            this.supportedSongBuffEffectNames = supportedSongBuffEffectNames;
            this.healthTick = healthTick;
            this.period = period;
        }

        public boolean isSupportedEffectName(String effectName) {
            return supportedSongBuffEffectNames.contains(effectName);
        }

        public double getHealthTick() {
            return healthTick;
        }

        public long getPeriod() {
            return period;
        }
    }
    
    public class BoomingVoiceHealingEffect extends PeriodicHealEffect {

        public BoomingVoiceHealingEffect(Skill skill, Player applier, BoomingVoiceEffect passiveEffect, long duration) {
            this(skill, applier, passiveEffect.getPeriod(), duration, passiveEffect.getHealthTick());
        }

        public BoomingVoiceHealingEffect(Skill skill, Player applier, long period, long duration, double tickHealth) {
            super(skill, boomingVoiceHealingEffectName, applier, period, duration, tickHealth);
        }
    }  

    public class SkillHeroListener implements Listener {
        private final PassiveSkill skill;

        public SkillHeroListener(PassiveSkill skill) {
            this.skill = skill;
        }
        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onSongBuffEffectApplied(EffectAddEvent event) {
            if (!(event.getCharacter() instanceof Hero))
                return;

            final Hero hero = (Hero) event.getCharacter();
            if (hero.hasEffect(skill.getName()))
                return; // Handle only for those with this passive booming voice effect

            final Effect effectApplied = event.getEffect();
            final Skill effectSkill = effectApplied.getSkill();
            if (effectSkill != null && !effectSkill.isType(SkillType.ABILITY_PROPERTY_SONG))
                return; // Not necessary but will reduce effects needed to be checked for, more quickly

            final String appliedEffectName = effectApplied.getName();
            if (appliedEffectName.equals(skill.getName()) || appliedEffectName.equals(boomingVoiceHealingEffectName))
                return; // Skip handling this skill's passive and applied healing effect

            // Check if the effect is a supported effect
            final BoomingVoiceEffect passiveEffect = (BoomingVoiceEffect) hero.getEffect(skill.getName());
            assert passiveEffect != null;
            if (!passiveEffect.isSupportedEffectName(appliedEffectName))
                return;

            if (effectApplied instanceof ExpirableEffect) {
                final ExpirableEffect appliedExpireableEffect = (ExpirableEffect) effectApplied;
                final long duration = appliedExpireableEffect.getDuration();
                
                if (hero.hasEffect(boomingVoiceHealingEffectName)) {
                    BoomingVoiceHealingEffect healingEffect = (BoomingVoiceHealingEffect) hero.getEffect(boomingVoiceHealingEffectName);
                    assert healingEffect != null;
                    if (healingEffect.getRemainingTime() < duration) {
                        healingEffect.setDuration(duration); // extend healing effect with higher duration
                    } // else keep current duration (which is longer)
                } else {
                    hero.addEffect(new BoomingVoiceHealingEffect(skill, hero.getPlayer(), passiveEffect, duration));
                }
            } else if (!hero.isSuppressing(skill)) {
                // Allowing this to be suppressed so players can silence it from spamming while awaiting fix
                hero.getPlayer().sendMessage(ChatColor.RED + "Contact a admin or dev to fix BoomingVoice working on this song effect (" 
                        + appliedEffectName
                        + ")! In meantime you can use '/hero stfu " + skill.getName().toLowerCase() + "' to silence this message.");
            }
        }
    }
}
