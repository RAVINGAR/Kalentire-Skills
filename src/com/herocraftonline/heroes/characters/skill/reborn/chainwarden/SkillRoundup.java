package com.herocraftonline.heroes.characters.skill.reborn.chainwarden;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class SkillRoundup extends ActiveSkill {
    public SkillRoundup(Heroes plugin) {
        super(plugin, "Roundup");
        setDescription("You roundup your currently hooked targets, yanking each one of them towards you. " +
                "Does not remove hooks.");
        setUsage("/skill roundup");
        setIdentifiers("skill roundup");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.MULTI_GRESSIVE);
    }

    public String getDescription(Hero hero) {
        return super.getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.RADIUS.node(), 30.0);
        return config;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        SkillYank yankSkill = (SkillYank) plugin.getSkillManager().getSkill(SkillYank.skillName);
        if (yankSkill == null) {
            Heroes.log(Level.SEVERE, SkillYank.skillName + " is missing from the server. " + getName() + " will no longer work. "
                    + SkillYank.skillName + "_must_ be available to the class that has " + getName() + ".");

            player.sendMessage("One of the Admins or devs broke this skill. Tell them to read the heroes logs to fix it.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        int hitCount = 0;
        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 30.0, false);
        for (Entity entity : player.getNearbyEntities(radius, radius / 2.0, radius)) {
            if (!(entity instanceof LivingEntity))
                continue;

            LivingEntity target = (LivingEntity) entity;
            SkillResult result = yankSkill.use(hero, target, new String[]{"NoBroadcast", "RemoveHook"});
            if (result == SkillResult.NORMAL) {
                hitCount++;
            }
        }

        if (hitCount < 1)
            return SkillResult.INVALID_TARGET;

        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }
}