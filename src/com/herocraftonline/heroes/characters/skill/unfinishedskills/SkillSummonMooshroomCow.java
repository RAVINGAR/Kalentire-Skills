package com.herocraftonline.heroes.characters.skill.unfinishedskills;

import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;

public class SkillSummonMooshroomCow extends ActiveSkill {
    
    public SkillSummonMooshroomCow(Heroes plugin) {
        super(plugin, "SummonMooshroomCow");
        setDescription("100% chance to spawn 1 mooshroom cow, $2% for 2, and $3% for 3.");
        setUsage("/skill mushroomcow");
        setArgumentRange(0, 0);
        setIdentifiers("skill mushroomc", "skill mushroomcow", "skill mcow", "mooshroom", "summonmushroomcow", "mushroomcow");
        setTypes(SkillType.KNOWLEDGE, SkillType.SUMMON, SkillType.SILENCABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        int chance2x = (int) (SkillConfigManager.getUseSetting(hero, this, "chance-2x", 0.2, false) * 100 +
                SkillConfigManager.getUseSetting(hero, this, "chance-2x-per-level", 0.0, false) * hero.getSkillLevel(this));
        int chance3x = (int) (SkillConfigManager.getUseSetting(hero, this, "chance-3x", 0.1, false) * 100 +
                SkillConfigManager.getUseSetting(hero, this, "chance-3x-per-level", 0.0, false) * hero.getSkillLevel(this));
        String description = getDescription().replace("$2", chance2x + "").replace("$3", chance3x + "");
        
        //COOLDOWN
        int cooldown = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 0, false)
                - SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN_REDUCE, 0, false) * hero.getSkillLevel(this)) / 1000;
        if (cooldown > 0) {
            description += " CD:" + cooldown + "s";
        }
        
        //MANA
        int mana = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 10, false)
                - (SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA_REDUCE, 0, false) * hero.getSkillLevel(this));
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
        int staminaCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA, 0, false)
                - (SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA_REDUCE, 0, false) * hero.getSkillLevel(this));
        if (staminaCost > 0) {
            description += " FP:" + staminaCost;
        }
        
        //DELAY
        int delay = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DELAY, 0, false) / 1000;
        if (delay > 0) {
            description += " W:" + delay + "s";
        }
        
        //EXP
        int exp = SkillConfigManager.getUseSetting(hero, this, SkillSetting.EXP, 0, false);
        if (exp > 0) {
            description += " XP:" + exp;
        }
        return description;
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("chance-2x", 0.2);
        node.set("chance-3x", 0.1);
        node.set("chance-2x-per-level", 0.0);
        node.set("chance-3x-per-level", 0.0);
        return node;
    }
    
    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        double chance2x = SkillConfigManager.getUseSetting(hero, this, "chance-2x", 0.2, false);
        double chance3x = SkillConfigManager.getUseSetting(hero, this, "chance-3x", 0.1, false);
        Block wTargetBlock = player.getTargetBlock(null, 20).getRelative(BlockFace.UP);
        player.getWorld().spawnEntity(wTargetBlock.getLocation(), EntityType.MUSHROOM_COW);
        double chance = Util.nextRand();
        if (chance <= chance3x) {
            player.getWorld().spawnEntity(wTargetBlock.getLocation(), EntityType.MUSHROOM_COW);
            player.getWorld().spawnEntity(wTargetBlock.getLocation(), EntityType.MUSHROOM_COW);
        } else if (chance <= chance2x) {
            player.getWorld().spawnEntity(wTargetBlock.getLocation(), EntityType.MUSHROOM_COW);
        }
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.COW_HURT , 0.8F, 1.0F); 
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }
}
