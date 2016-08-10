package com.herocraftonline.heroes.characters.skill.public1;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.HashSet;

public class SkillSummonMooshroomCow extends ActiveSkill {

    public SkillSummonMooshroomCow(Heroes plugin) {
        super(plugin, "SummonMooshroomCow");
        this.setDescription("100% chance to spawn 1 mooshroom cow, $2% for 2, and $3% for 3.");
        this.setUsage("/skill mushroomcow");
        this.setArgumentRange(0, 0);
        this.setIdentifiers("skill mushroom", "skill mushroomcow", "skill mcow");
        this.setTypes(SkillType.KNOWLEDGE, SkillType.SUMMONING, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        final int chance2x = (int) ((SkillConfigManager.getUseSetting(hero, this, "chance-2x", 0.2, false) * 100) + (SkillConfigManager.getUseSetting(hero, this, "chance-2x-per-level", 0.0, false) * hero.getSkillLevel(this)));
        final int chance3x = (int) ((SkillConfigManager.getUseSetting(hero, this, "chance-3x", 0.1, false) * 100) + (SkillConfigManager.getUseSetting(hero, this, "chance-3x-per-level", 0.0, false) * hero.getSkillLevel(this)));
        String description = this.getDescription().replace("$2", chance2x + "").replace("$3", chance3x + "");

        //COOLDOWN
        final int cooldown = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 0, false) - (SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN_REDUCE, 0, false) * hero.getSkillLevel(this))) / 1000;
        if (cooldown > 0) {
            description += " CD:" + cooldown + "s";
        }

        //MANA
        final int mana = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 10, false) - (SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA_REDUCE, 0, false) * hero.getSkillLevel(this));
        if (mana > 0) {
            description += " M:" + mana;
        }

        //HEALTH_COST
        final int healthCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST, 0, false) - (SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST_REDUCE, mana, true) * hero.getSkillLevel(this));
        if (healthCost > 0) {
            description += " HP:" + healthCost;
        }

        //STAMINA
        final int staminaCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA, 0, false) - (SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA_REDUCE, 0, false) * hero.getSkillLevel(this));
        if (staminaCost > 0) {
            description += " FP:" + staminaCost;
        }

        //DELAY
        final int delay = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DELAY, 0, false) / 1000;
        if (delay > 0) {
            description += " W:" + delay + "s";
        }

        //EXP
        final int exp = SkillConfigManager.getUseSetting(hero, this, SkillSetting.EXP, 0, false);
        if (exp > 0) {
            description += " XP:" + exp;
        }
        return description;
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set("chance-2x", 0.2);
        node.set("chance-3x", 0.1);
        node.set("chance-2x-per-level", 0.0);
        node.set("chance-3x-per-level", 0.0);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();
        final double chance2x = SkillConfigManager.getUseSetting(hero, this, "chance-2x", 0.2, false);
        final double chance3x = SkillConfigManager.getUseSetting(hero, this, "chance-3x", 0.1, false);
        final Block wTargetBlock = player.getTargetBlock((HashSet<Byte>) null, 20).getRelative(BlockFace.UP);
        player.getWorld().spawnEntity(wTargetBlock.getLocation(), EntityType.MUSHROOM_COW);
        final double chance = Util.nextRand();
        if (chance <= chance3x) {
            player.getWorld().spawnEntity(wTargetBlock.getLocation(), EntityType.MUSHROOM_COW);
            player.getWorld().spawnEntity(wTargetBlock.getLocation(), EntityType.MUSHROOM_COW);
        } else if (chance <= chance2x) {
            player.getWorld().spawnEntity(wTargetBlock.getLocation(), EntityType.MUSHROOM_COW);
        }
        this.broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }
}
