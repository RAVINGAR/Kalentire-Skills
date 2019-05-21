package com.herocraftonline.heroes.characters.skill.reborn.unused;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.InvisibleEffect;
import com.herocraftonline.heroes.characters.skill.*;
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

    public SkillPhase(Heroes plugin) {
        super(plugin, "Phase");
        setDescription("You phase into a different reality, causing you to flicker in and out of this world for $1 second(s). "
                + "Beings in different realities cannot interact with each other.");
        setUsage("/skill phase");
        setArgumentRange(0, 0);
        setIdentifiers("skill phase");
        setTypes(SkillType.BUFFING, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
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

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% begins to phase!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is no longer phasing.").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);

        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1500, false);
        long duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        long phaseDuration = SkillConfigManager.getUseSetting(hero, this, "phase-duration", 500, false);
        hero.addEffect(new PhasingEffect(this, hero.getPlayer(), period, duration, phaseDuration));

        return SkillResult.NORMAL;
    }

    private class PhasingEffect extends PeriodicExpirableEffect {

        long phaseDuration;

        PhasingEffect(Skill skill, Player applier, long period, long duration, long phaseDuration) {
            super(skill, "Phasing", applier, period, duration, applyText, expireText);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.MAGIC);
            types.add(EffectType.DISPELLABLE);

            this.phaseDuration = phaseDuration;
        }

        @Override
        public void tickHero(Hero hero) {
            Player player = hero.getPlayer();
            Location playerLoc = player.getLocation();
            playerLoc.getWorld().playEffect(playerLoc, Effect.ENDER_SIGNAL, 3);
            playerLoc.getWorld().playSound(playerLoc, Sound.ENTITY_ENDERMEN_TELEPORT, 0.8F, 1.0F);

            PhasedEffect phasedEffect = new PhasedEffect(skill, player, phaseDuration);
            hero.addEffect(phasedEffect);
        }

        @Override
        public void tickMonster(Monster monster) {}
    }

    private class PhasedEffect extends InvisibleEffect {
        PhasedEffect(Skill skill, Player applier, long duration) {
            super(skill, "Phased", applier, duration, null, null);

            types.add(EffectType.UNBREAKABLE);
            types.add(EffectType.SILENCE);
            types.add(EffectType.INVULNERABILITY);
            types.add(EffectType.DISARM);
        }
    }
}
