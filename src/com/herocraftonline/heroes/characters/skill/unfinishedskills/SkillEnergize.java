package com.herocraftonline.heroes.characters.skill.unfinishedskills;

import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;


public class SkillEnergize extends ActiveSkill{
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();
    public SkillEnergize(Heroes plugin) {
        super(plugin, "Energize");
        setDescription("Replenishes $1 points of your stamina.");
        setUsage("/skill energize");
        setArgumentRange(0, 0);
        setIdentifiers(new String[]{"skill Energize"});

        setTypes(SkillType.SILENCABLE, SkillType.PHYSICAL, SkillType.BUFF);
    }

    @Override
    public String getDescription(Hero hero) {

        //DAMAGE
        int amount = (int) (SkillConfigManager.getUseSetting(hero, this, SkillSetting.AMOUNT.node(), 20.0, false) +
                (SkillConfigManager.getUseSetting(hero, this, "amount-increase", 0.0, false) * hero.getSkillLevel(this)));
        amount = amount > 0 ? amount : 0;
        String description = getDescription().replace("$1", amount + "");

        //COOLDOWN
        int cooldown = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN.node(), 0, false)
                - SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN_REDUCE.node(), 0, false) * hero.getSkillLevel(this)) / 1000;
        if (cooldown > 0) {
            description += " CD:" + cooldown + "s";
        }

        //MANA
        int mana = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA.node(), 10, false)
                - (SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA_REDUCE.node(), 0, false) * hero.getSkillLevel(this));
        if (mana > 0) {
            description += " M:" + mana;
        }

        //HEALTH_COST
        int healthCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST, 0, false) -
                (SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST_REDUCE, mana, true) * hero.getSkillLevel(this));
        if (healthCost > 0) {
            description += " HP:" + healthCost;
        }

        //STAMINA
        int staminaCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA.node(), 0, false)
                - (SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA_REDUCE.node(), 0, false) * hero.getSkillLevel(this));
        if (staminaCost > 0) {
            description += " FP:" + staminaCost;
        }

        //DELAY
        int delay = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DELAY.node(), 0, false) / 1000;
        if (delay > 0) {
            description += " W:" + delay + "s";
        }

        //EXP
        int exp = SkillConfigManager.getUseSetting(hero, this, SkillSetting.EXP.node(), 0, false);
        if (exp > 0) {
            description += " XP:" + exp;
        }
        return description;
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.AMOUNT.node(), 20);
        node.set("amount-increase", 0);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        if(player.getFoodLevel()>=20){
            return SkillResult.CANCELLED;
        }

        int amount = (int) (SkillConfigManager.getUseSetting(hero, this, SkillSetting.AMOUNT.node(), 20.0, false) +
                (SkillConfigManager.getUseSetting(hero, this, "amount-increase", 0.0, false) * hero.getSkillLevel(this)));
        amount = amount > 0 ? amount : 0;

        if(player.getFoodLevel()+amount<20){
            player.setFoodLevel(player.getFoodLevel()+amount);
        }else{
            player.setFoodLevel(20);
        }
        player.getWorld().playEffect(player.getLocation(), Effect.ENDER_SIGNAL, 3);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.LEVEL_UP , 0.8F, 1.0F);
        broadcastExecuteText(hero);
        // this is our fireworks shit
        try {
            fplayer.playFirework(player.getWorld(), 
            		player.getLocation().add(0,1.5,0), 
            		FireworkEffect.builder().flicker(false).trail(false)
            		.with(FireworkEffect.Type.STAR)
            		.withColor(Color.YELLOW)
            		.withFade(Color.TEAL)
            		.build());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return SkillResult.NORMAL;
    }
}