/*
 * TODO: re-write me
package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.herocraftonline.dev.heroes.util.Setting;

public class SkillLickWounds extends ActiveSkill {

    public SkillLickWounds(Heroes plugin) {
        super(plugin, "LickWounds");
        setDescription("Heals your nearby wolves");
        setUsage("/skill lickwounds");
        setArgumentRange(0, 0);
        setIdentifiers("skill lickwounds", "skill lwounds");
        setTypes(SkillType.HEAL, SkillType.SILENCABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.RADIUS.node(), 10);
        node.set("heal-amount", .25); // % heal of maximum health
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        int rangeSquared = (int) Math.pow(SkillConfigManager.getUseSetting(hero, this, Setting.RADIUS, 10, false), 2);
        Skill skill = plugin.getSkillManager().getSkill("Wolf");
        if (skill == null)
            return SkillResult.FAIL;

        if (!hero.canUseSkill(skill) || SkillConfigManager.getUseSetting(hero, skill, Setting.LEVEL.node(), 1, true) > hero.getSkillLevel(this)) {
            Messaging.send(player, "You don't have the proper skills to do that!");
            return SkillResult.FAIL;
        }

        double healed = SkillConfigManager.getUseSetting(hero, this, "heal-amount", .25, false);
        boolean used = false;
        for (LivingEntity lEntity : hero.getSummons()) {
            if (!(lEntity instanceof Wolf) || lEntity.getLocation().distanceSquared(player.getLocation()) > rangeSquared) {
                continue;
            }

            if (lEntity.getHealth() + healed > healthMax) {
                lEntity.setHealth(healthMax);
            } else {
                lEntity.setHealth((int) (lEntity.getHealth() + healed));
            }
            used = true;
        }

        if (!used) {
            Messaging.send(player, "There are no nearby wolves to heal!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

}
*/
