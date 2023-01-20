package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.InvisibleEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class SkillPhase extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillPhase(final Heroes plugin) {
        super(plugin, "Phase");
        setDescription("You phase into a different reality, causing you to flicker in and out of this world for $1 second(s). "
                + "Beings in different realities cannot interact with each other.");
        setUsage("/skill phase");
        setArgumentRange(0, 0);
        setIdentifiers("skill phase");
        setTypes(SkillType.BUFFING, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(final Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 7500, false);
        final String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DURATION.node(), 7500);
        config.set(SkillSetting.PERIOD.node(), 1500);
        config.set("phase-duration", 500);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% begins to phase!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is no longer phasing.");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% begins to phase!").replace("%hero%", "$1").replace("$hero$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is no longer phasing.").replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        broadcastExecuteText(hero);

        final long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1500, false);
        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 7500, false);
        final long phaseDuration = SkillConfigManager.getUseSetting(hero, this, "phase-duration", 500, false);
        hero.addEffect(new PhasingEffect(this, hero.getPlayer(), period, duration, phaseDuration));

        return SkillResult.NORMAL;
    }

    private static class PhasedEffect extends InvisibleEffect {
        PhasedEffect(final Skill skill, final Player applier, final long duration) {
            super(skill, "Phased", applier, duration, null, null);

            types.add(EffectType.UNBREAKABLE);
            types.add(EffectType.SILENCE);
            types.add(EffectType.INVULNERABILITY);
            types.add(EffectType.DISARM);
        }
    }

    private class PhasingEffect extends PeriodicExpirableEffect {

        final long phaseDuration;

        PhasingEffect(final Skill skill, final Player applier, final long period, final long duration, final long phaseDuration) {
            super(skill, "Phasing", applier, period, duration, applyText, expireText);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.MAGIC);
            types.add(EffectType.DISPELLABLE);

            this.phaseDuration = phaseDuration;
        }

        @Override
        public void tickHero(final Hero hero) {
            final Player player = hero.getPlayer();
            final Location playerLoc = player.getLocation();
            playerLoc.getWorld().playEffect(playerLoc, Effect.ENDER_SIGNAL, 3);
            playerLoc.getWorld().playSound(playerLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8F, 1.0F);

            final PhasedEffect phasedEffect = new PhasedEffect(skill, player, phaseDuration);
            hero.addEffect(phasedEffect);
        }

        @Override
        public void tickMonster(final Monster monster) {
        }
    }
}
