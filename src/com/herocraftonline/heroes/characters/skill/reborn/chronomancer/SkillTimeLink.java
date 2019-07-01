package com.herocraftonline.heroes.characters.skill.reborn.chronomancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.ChatColor;
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
                "The link has a maximum range of $2, and breaks if your target moves more than $3 blocks away from you.");
        setUsage("/skill timelink <ally>");
        setIdentifiers("skill timelink");
        setArgumentRange(0, 1);
        setTypes(SkillType.ABILITY_PROPERTY_TEMPORAL, SkillType.MULTI_GRESSIVE, SkillType.NO_SELF_TARGETTING, SkillType.DEFENSIVE_NAME_TARGETTING_ENABLED, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        long duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        double maxLinkDist = SkillConfigManager.getUseSetting(hero, this, "max-link-distance", 16.0, false);
        double breakDistance = SkillConfigManager.getUseSetting(hero, this, "break-distance", 35.0, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(duration / 1000.0))
                .replace("$2", Util.decFormat.format(maxLinkDist))
                .replace("$3", Util.decFormat.format(breakDistance));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 12);
        config.set(SkillSetting.DURATION.node(), 12000);
        config.set("max-link-distance", 16.0);
        config.set("break-distance", 35.0);
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

        long duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        double maxLinkDistance = SkillConfigManager.getUseSetting(hero, this, "max-link-distance", 16.0, false);
        double breakDistance = SkillConfigManager.getUseSetting(hero, this, "break-distance", 35.0, false);

        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
        hero.addEffect(new TimeLinkEffect(this, player, duration, targetCT, maxLinkDistance, breakDistance));

        return SkillResult.NORMAL;
    }

    public class TimeLinkEffect extends ExpirableEffect {
        private final double maximumDistanceSquared;
        private final double breakDistanceSquared;

        private Hero appliedHero;
        private CharacterTemplate linkedTarget;

        TimeLinkEffect(Skill skill, Player applier, long duration, CharacterTemplate target, double maximumDistance, double breakDistance) {
            super(skill, timeLinkEffectName, applier, duration, null, null);
            this.linkedTarget = target;
            this.maximumDistanceSquared = maximumDistance * maximumDistance;
            this.breakDistanceSquared = breakDistance * breakDistance;
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            this.appliedHero = hero;
        }

        public CharacterTemplate getTargetCT() {
            if (linkedTarget == null || linkedTarget.getEntity() == null) {
                appliedHero.removeEffect(this);
                return null;
            }

            LivingEntity linkedTargetEnt = linkedTarget.getEntity();
            if (linkedTargetEnt.getWorld() != applier.getWorld()) {
                appliedHero.removeEffect(this);
                return null;
            }

            double distSquared = linkedTargetEnt.getLocation().distanceSquared(applier.getLocation());
            if (distSquared > breakDistanceSquared) {
                appliedHero.removeEffect(this);
                return null;
            } else if (distSquared > maximumDistanceSquared) {
                return null;
            }

            return this.linkedTarget;
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            Player player = hero.getPlayer();
            LivingEntity linkedEntity = linkedTarget.getEntity();

            String name = linkedEntity == null
                    ? "<UNKNOWN>"
                    : CustomNameManager.getName(linkedEntity);

            applier.sendMessage("    " + ChatComponents.GENERIC_SKILL + ChatColor.GOLD + "Your timelink with "
                    + ChatColor.WHITE + name
                    + ChatColor.GOLD + "has broken!");

            if (expireText != null && expireText.length() > 0) {
                if (hero.hasEffectType(EffectType.SILENT_ACTIONS)) {
                    Messaging.send(player, "    " + expireText, player.getName(), name, this.skill.getName());
                } else {
                    this.broadcast(player.getLocation(), "    " + expireText, player.getName(), name, this.skill.getName());
                }
            }
        }
    }
}
