package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;

public class SkillInvigorate extends TargettedSkill{
    
    public SkillInvigorate(Heroes plugin) {
        super(plugin, "Invigorate");
        setDescription("Refills $1 points of target's stamina.");
        setUsage("/skill Invigorate <target>");
        setArgumentRange(0, 1);
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
        Player target;
    	if(entity != null) 	{
        	if(!(entity instanceof Player)){
        		return SkillResult.INVALID_TARGET;
        	}
        	target = (Player)entity;
        } else {
        	target = Bukkit.getServer().getPlayer(args[0]);
        	if(target == null) {
        		Messaging.send(hero.getPlayer(), "Targetted Player not Found!", new Object[0]);
        		return SkillResult.INVALID_TARGET_NO_MSG;
        	}
        	if(target.getName() == hero.getPlayer().getName()) {
        		Messaging.send(hero.getPlayer(), "Cannot be used on self!", new Object[0]);
        		return SkillResult.INVALID_TARGET_NO_MSG;
        	}
        }

        if(target.getFoodLevel()>=20){
        	Messaging.send(hero.getPlayer(), "This player already has full stamina!", new Object[0]);
            return SkillResult.INVALID_TARGET_NO_MSG;
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
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENDERDRAGON_WINGS , 0.5F, 1.0F);
        return SkillResult.NORMAL;
    }
}
