package com.herocraftonline.heroes.characters.skill.reborn.chainwarden;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
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

public class SkillWardensRend extends ActiveSkill {
    public SkillWardensRend(Heroes plugin) {
        super(plugin, "WardensRend");
        setDescription("Rend all of your currently hooked enemies, Hemorrhaging each one of them.");
        setUsage("/skill wardensrend");
        setIdentifiers("skill wardensrend");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.AREA_OF_EFFECT, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    public String getDescription(Hero hero) {
        return super.getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.RADIUS.node(), 30);
        return config;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        SkillHemorrhage hemorrhageSkill = (SkillHemorrhage) plugin.getSkillManager().getSkill(SkillHemorrhage.skillName);
        if (hemorrhageSkill == null) {
            Heroes.log(Level.SEVERE, SkillHemorrhage.skillName + " is missing from the server. " + getName() + " will no longer work. "
                    + SkillHemorrhage.skillName + "_must_ be available to the class that has " + getName() + ".");

            player.sendMessage("One of the Admins or devs broke this skill. Tell them to read the heroes logs to fix it.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        int hitCount = 0;
        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 30.0, false);
        for (Entity entity : player.getNearbyEntities(radius, radius / 2.0, radius)) {
            if (!(entity instanceof LivingEntity))
                continue;

            LivingEntity target = (LivingEntity) entity;
            if (!damageCheck(player, target))
                continue;

            SkillResult result = hemorrhageSkill.use(hero, target, new String[]{"NoBroadcast"});
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