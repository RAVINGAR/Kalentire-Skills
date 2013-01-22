package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SafeFallEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;

public class SkillDropTheBass extends ActiveSkill {

    public SkillDropTheBass(Heroes plugin) {
        super(plugin, "DropTheBass");
        setDescription("Stops your close group members from taking fall damage for $1 seconds.");
        setUsage("/skill dropthebass");
        setArgumentRange(0, 0);
        setIdentifiers("skill dropthebass");
        setTypes(SkillType.MOVEMENT, SkillType.BUFF, SkillType.SILENCABLE);

    }

    @Override
    public String getDescription(Hero hero) {
        String description = getDescription();
        //DURATION
        int duration = (SkillConfigManager.getUseSetting(hero, this, Setting.DURATION.node(), 10000, false)
                + SkillConfigManager.getUseSetting(hero, this, Setting.DURATION_INCREASE.node(), 0, false) * hero.getSkillLevel(this)) / 1000;
        if (duration > 0) {
            description += " D:" + duration + "s";
        }

        //RADIUS
        int radius = (int) ((SkillConfigManager.getUseSetting(hero, this, Setting.RADIUS.node(), 15.0, false)
                + SkillConfigManager.getUseSetting(hero, this, Setting.RADIUS_INCREASE.node(), 0, false) * hero.getSkillLevel(this)));
        if (duration > 0) {
            description += " R:" + radius + "s";
        }

        //COOLDOWN
        int cooldown = (SkillConfigManager.getUseSetting(hero, this, Setting.COOLDOWN.node(), 0, false)
                - SkillConfigManager.getUseSetting(hero, this, Setting.COOLDOWN_REDUCE.node(), 0, false) * hero.getSkillLevel(this)) / 1000;
        if (cooldown > 0) {
            description += " CD:" + cooldown + "s";
        }

        //MANA
        int mana = SkillConfigManager.getUseSetting(hero, this, Setting.MANA.node(), 0, false)
                - (SkillConfigManager.getUseSetting(hero, this, Setting.MANA_REDUCE.node(), 0, false) * hero.getSkillLevel(this));
        if (mana > 0) {
            description += " M:" + mana;
        }

        //HEALTH_COST
        int healthCost = SkillConfigManager.getUseSetting(hero, this, Setting.HEALTH_COST, 0, false) -
                (SkillConfigManager.getUseSetting(hero, this, Setting.HEALTH_COST_REDUCE, mana, true) * hero.getSkillLevel(this));
        if (healthCost > 0) {
            description += " HP:" + healthCost;
        }

        //STAMINA
        int staminaCost = SkillConfigManager.getUseSetting(hero, this, Setting.STAMINA.node(), 0, false)
                - (SkillConfigManager.getUseSetting(hero, this, Setting.STAMINA_REDUCE.node(), 0, false) * hero.getSkillLevel(this));
        if (staminaCost > 0) {
            description += " FP:" + staminaCost;
        }

        //DELAY
        int delay = SkillConfigManager.getUseSetting(hero, this, Setting.DELAY.node(), 0, false) / 1000;
        if (delay > 0) {
            description += " W:" + delay + "s";
        }

        //EXP
        int exp = SkillConfigManager.getUseSetting(hero, this, Setting.EXP.node(), 0, false);
        if (exp > 0) {
            description += " XP:" + exp;
        }
        return description.replace("$1", duration / 1000 + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.APPLY_TEXT.node(), "%hero%'s party celebrates bass-drops!");
        node.set(Setting.EXPIRE_TEXT.node(), "%hero%'s party no longer is dropping bass!");
        node.set(Setting.DURATION.node(), 10000);
        node.set(Setting.DURATION_INCREASE.node(), 0);
        node.set(Setting.RADIUS.node(), 15.0);
        node.set(Setting.RADIUS_INCREASE.node(), 0.0);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        if (!hero.hasParty()){
                return SkillResult.CANCELLED;
        }

        int duration = (SkillConfigManager.getUseSetting(hero, this, Setting.DURATION.node(), 10000, false)
                + SkillConfigManager.getUseSetting(hero, this, Setting.DURATION_INCREASE.node(), 0, false) * hero.getSkillLevel(this));
        int radius = (int) ((SkillConfigManager.getUseSetting(hero, this, Setting.RADIUS.node(), 15.0, false)
                + SkillConfigManager.getUseSetting(hero, this, Setting.RADIUS_INCREASE.node(), 0, false) * hero.getSkillLevel(this)));
        for(Hero member : hero.getParty().getMembers()){
            if(member.getPlayer().getLocation().distance(hero.getPlayer().getLocation()) <= radius){
                member.addEffect(new SafeFallEffect(this, duration));
            }
        }
        broadcastExecuteText(hero);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.NOTE_BASS, 10.0F, 1.0F);
        return SkillResult.NORMAL;
    }
}