package com.herocraftonline.heroes.characters.skill.remastered.farmer;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseSummonEntity;

public class SkillSummonHorse extends SkillBaseSummonEntity {

    public SkillSummonHorse(Heroes plugin) {
        super(plugin, "SummonHorse");
        setDescription("100% chance to spawn 1 horse, $2% for 2, and $3% for 3.");
        setUsage("/skill horse");
        setIdentifiers("skill summonhorse", "skill horse");
    }

    @Override
    protected EntityType getEntityType(Block targetBlock) {
        return EntityType.HORSE;
    }
    
    @Override
    protected void applySoundEffects(World world, Player player) {
        //player.getWorld().playSound(player.getLocation(), Sound.HORSE_SOFT , 0.8F, 1.0F);
    }
}
