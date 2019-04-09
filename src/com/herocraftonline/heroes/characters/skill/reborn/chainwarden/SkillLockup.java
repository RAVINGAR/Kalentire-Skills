package com.herocraftonline.heroes.characters.skill.reborn.chainwarden;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class SkillLockup extends ActiveSkill {
    public SkillLockup(Heroes plugin) {
        super(plugin, "Lockup");
        setDescription("You launch a hook for every chain in your chain belt to all nearby targets, instantly latching onto them and dealing $1 physical damage. " +
                "Due to the hasty nature of deploying your entire arsenal, these chains are not attached as firmly as normal, and only last for $2 seconds. " +
                "You will also lose your grip on them more easily. Enemies will be hit first, followed by allies if you have any remaining chains left. " +
                "Allies will not be dealt damage.");
        setUsage("/skill lockup");
        setIdentifiers("skill lockup");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.MULTI_GRESSIVE, SkillType.AREA_OF_EFFECT);
    }

    public String getDescription(Hero hero) {
        return super.getDescription()
                .replace("$1", "FIX THIS")
                .replace("$2", "FIX THIS");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.RADIUS.node(), 12.0);
        config.set(SkillSetting.DAMAGE.node(), 35.0);
        config.set(SkillSetting.DURATION.node(), 5000);
        config.set("hook-leash-distance", 15.0);
        config.set("hook-leash-power", 1.0);
        return config;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        SkillHookshot hookSkill = (SkillHookshot) plugin.getSkillManager().getSkill(SkillHookshot.skillName);
        if (hookSkill == null) {
            Heroes.log(Level.SEVERE, SkillHookshot.skillName + " is missing from the server. " + getName() + " will no longer work. "
                    + SkillHookshot.skillName + "_must_ be available to the class that has " + getName() + ".");
            player.sendMessage("One of the Admins or devs broke this skill. Tell them to read the heroes logs to fix it.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        int numChains = SkillChainBelt.tryGetCurrentChainCount(hero);
        if (numChains == 0) {
            player.sendMessage("    " + ChatComponents.GENERIC_SKILL + "No chains available to throw!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        broadcastExecuteText(hero);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 35.0, false);
        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 12.0, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        double hookLeashDistance = SkillConfigManager.getUseSetting(hero, this, "hook-leash-distance", 15.0, false);
        double hookLeashPower = SkillConfigManager.getUseSetting(hero, this, "hook-leash-power", 1.0, false);

        List<Entity> entities = player.getNearbyEntities(radius, radius / 2.0, radius);
        List<LivingEntity> allies = new ArrayList<LivingEntity>();
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity))
                continue;

            final LivingEntity target = (LivingEntity) entity;
            boolean isAlliedTo = hero.isAlliedTo(target);
            if (isAlliedTo) {
                allies.add(target);
                continue;
            }

            if (!damageCheck(player, target))
                continue;
            if (!SkillChainBelt.tryRemoveChain(this, hero, false))
                break;

            addSpellTarget(target, hero);
            damageEntity(target, player, damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK);

            hookTarget(hero, target, duration, hookLeashDistance, hookLeashPower);
        }

        for (LivingEntity ally : allies) {
            if (!SkillChainBelt.tryRemoveChain(this, hero, false))
                break;
            hookTarget(hero, ally, duration, hookLeashDistance, hookLeashPower);
        }

        SkillChainBelt.showCurrentChainCount(hero);
        return SkillResult.NORMAL;
    }

    private void hookTarget(Hero hero, LivingEntity target, long duration, double hookLeashDistance, double hookLeashPower) {
        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
        SkillHookshot.HookedEffect hookedEffect = new SkillHookshot.HookedEffect(this, hero, duration, hookLeashDistance, hookLeashPower);
        targetCT.addEffect(hookedEffect);
    }
}