package com.herocraftonline.heroes.characters.skill.reborn.chronomancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class SkillTimeLink extends TargettedSkill {
    public static String timeLinkEffectName = "TimeLinking";

    private String expireText;

    public SkillTimeLink(Heroes plugin) {
        super(plugin, "TimeLink");
        setDescription("You create a time link with your target for $1 second(s). " +
                "While linked, any of your spells that Time Shift a player will also shift your linked target. " +
                "The link has a maximum range of $2");
        setUsage("/skill timelink");
        setIdentifiers("skill timelink");
        setTypes(SkillType.MULTI_GRESSIVE, SkillType.NO_SELF_TARGETTING);
    }

    @Override
    public String getDescription(Hero hero) {
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 12000, false);
        double breakDistance = SkillConfigManager.getUseSetting(hero, this, "break-distance", 16.0, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(duration / 1000.0))
                .replace("$1", Util.decFormat.format(breakDistance));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 12);
        config.set(SkillSetting.DURATION.node(), 12000);
        config.set("break-distance", 16.0);
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero%'s time link with %target% has vanished!");
        return config;
    }

    @Override
    public void init() {
        super.init();

        expireText = SkillConfigManager.getRaw(this,
                SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero%'s time link with %target% has vanished!")
                .replace("%hero%", "$1")
                .replace("%target%", "$2");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] strings) {
        Player player = hero.getPlayer();

        Skill timeShiftSkill = plugin.getSkillManager().getSkill(SkillTimeShift.skillName);
        if (timeShiftSkill == null) {
            Heroes.log(Level.SEVERE, SkillTimeShift.skillName + " is missing from the server. " + getName() + " will no longer work. "
                    + SkillTimeShift.skillName + "_must_ be available to the class that has " + getName() + ".");
            player.sendMessage("One of the Admins or devs broke this skill. Tell them to read the heroes logs to fix it.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        broadcastExecuteText(hero, target);

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        double breakDistance = SkillConfigManager.getUseSetting(hero, this, "break-distance", 16.0, false);

        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
        hero.addEffect(new TimeLinkEffect(this, player, duration, targetCT, breakDistance));

        return SkillResult.NORMAL;
    }

    public class TimeLinkEffect extends ExpirableEffect {
        private final double breakDistSquared;
        private Hero appliedHero;
        private CharacterTemplate linkedTarget;

        TimeLinkEffect(Skill skill, Player applier, long duration, CharacterTemplate target, double breakDistance) {
            super(skill, timeLinkEffectName, applier, duration, null, null);
            this.linkedTarget = target;
            this.breakDistSquared = breakDistance * breakDistance;
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            this.appliedHero = hero;
        }

        public CharacterTemplate getTargetCT() {
            if (linkedTarget == null) {
                appliedHero.removeEffect(this);
                return null;
            }

            if (this.linkedTarget.getEntity().getLocation().distanceSquared(applier.getLocation()) > breakDistSquared) {
                appliedHero.removeEffect(this);
                return null;
            }

            return this.linkedTarget;
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            Player player = hero.getPlayer();
            LivingEntity linkedEntity = linkedTarget.getEntity();
            if (expireText != null && expireText.length() > 0) {
                if (hero.hasEffectType(EffectType.SILENT_ACTIONS)) {
                    Messaging.send(player, "    " + expireText, player.getName(), linkedEntity == null
                            ? "<UNKNOWN PLAYER>"
                            : linkedEntity.getName(), this.skill.getName());
                } else {
                    this.broadcast(player.getLocation(), "    " + expireText, player.getName(), linkedEntity == null
                            ? "<UNKNOWN PLAYER>"
                            : linkedEntity.getName(), this.skill.getName());
                }
            }
        }
    }
}
