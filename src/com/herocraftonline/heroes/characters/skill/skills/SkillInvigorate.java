package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Setting;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

public class SkillInvigorate extends TargettedSkill{

    public SkillInvigorate(Heroes plugin) {
        super(plugin, "Invigorate");
        setDescription("Resplenishes $1 points of target's stamina.");
        setUsage("/skill Invigorate");
        setArgumentRange(0, 0);
        setIdentifiers(new String[]{"skill Invigorate"});

        setTypes(SkillType.SILENCABLE, SkillType.BUFF);
    }

    @Override
    public String getDescription(Hero hero) {

        //AMOUNT
        int amount = (int) (SkillConfigManager.getUseSetting(hero, this, Setting.AMOUNT.node(), 20.0, false) +
                (SkillConfigManager.getUseSetting(hero, this, "amount-increase", 0.0, false) * hero.getSkillLevel(this)));
        amount = amount > 0 ? amount : 0;
        String description = getDescription().replace("$1", amount + "");

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
        return description;
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.AMOUNT.node(), 20);
        node.set("amount-increase", 0);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity entity, String[] args) {
        if(!(entity instanceof Player)){
            return SkillResult.CANCELLED;
        }

        Player target = (Player) entity;

        if(target.getFoodLevel()>=20){
            return SkillResult.CANCELLED;
        }

        int amount = (int) (SkillConfigManager.getUseSetting(hero, this, Setting.AMOUNT.node(), 20.0, false) +
                (SkillConfigManager.getUseSetting(hero, this, "amount-increase", 0.0, false) * hero.getSkillLevel(this)));
        amount = amount > 0 ? amount : 0;

        if(target.getFoodLevel()+amount<20){
            target.setFoodLevel(target.getFoodLevel()+amount);
        }else{
            target.setFoodLevel(20);
        }
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }
}