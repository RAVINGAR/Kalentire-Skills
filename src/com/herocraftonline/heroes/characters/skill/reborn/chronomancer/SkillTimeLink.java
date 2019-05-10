package com.herocraftonline.heroes.characters.skill.reborn.chronomancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.logging.Level;

// IF YOU WANT TO SET THE RANGE OF TIMELINK EDIT IT IN TIMESHIFT
public class SkillTimeLink extends TargettedSkill {
    private String applyText;
    private String expireText;
    private static String timeLinkEffectName = "TimeLinkEffect-";

    public SkillTimeLink(Heroes plugin) {
        super(plugin, "TimeLink");
        setDescription("You create a time link with your target for $1 seconds "
                + "If your target is within $2 blocks any TimeShift used on any target will also be applied to the linked target");
        setUsage("/skill timelink");
        setIdentifiers("skill timelink");
        setTypes(SkillType.MULTI_GRESSIVE, SkillType.NO_SELF_TARGETTING, SkillType.MOVEMENT_INCREASING, SkillType.MOVEMENT_SLOWING);

        setToggleableEffectName(timeLinkEffectName);
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero% has linked with %target%!")
                .replace("%hero%", "$1")
                .replace("%target%", "$2");

        expireText = SkillConfigManager.getRaw(this,
                SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero%`s link with %target% has vanished!")
                .replace("%hero%", "$1")
                .replace("%target%", "$2");

        setUseText(null);

    }

    @Override
    public String getDescription(Hero hero) {
        // IF YOU WANT TO SET THE RANGE OF TIMELINK EDIT IT IN TIMESHIFT
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);

        return getDescription()
                .replace("$1", Util.decFormat.format((double) duration / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DURATION.node(), 10000);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity livingEntity, String[] strings) {
        Player player = hero.getPlayer();
        Skill timeShiftSkill = plugin.getSkillManager().getSkill(SkillTimeShift.skillName);
        if (timeShiftSkill == null) {
            Heroes.log(Level.SEVERE, SkillTimeShift.skillName + " is missing from the server. " + getName() + " will no longer work. "
                    + SkillTimeShift.skillName + "_must_ be available to the class that has " + getName() + ".");
            player.sendMessage("One of the Admins or devs broke this skill. Tell them to read the heroes logs to fix it.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        broadcastExecuteText(hero);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(livingEntity);

        timeLinkEffectName += hero.getName();
        hero.addEffect(new TimeLinkEffect(this, player, duration, targetCT));
        return SkillResult.NORMAL;
    }


    public class TimeLinkEffect extends ExpirableEffect {
        private CharacterTemplate targetCT;
        public TimeLinkEffect(Skill skill, Player applier, long duration, CharacterTemplate target) {
            super(skill, timeLinkEffectName, applier, duration, applyText, expireText);
            this.targetCT = target;
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
        }

        public CharacterTemplate getTargetCT() {
            return this.targetCT;
        }


    }
}
