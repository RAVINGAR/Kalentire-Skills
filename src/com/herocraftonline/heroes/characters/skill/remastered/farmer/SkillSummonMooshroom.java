package com.herocraftonline.heroes.characters.skill.remastered.farmer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseSummonEntity;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class SkillSummonMooshroom extends SkillBaseSummonEntity {
    
    public SkillSummonMooshroom(Heroes plugin) {
        super(plugin, "SummonMooshroom");
        setDescription("100% chance to spawn 1 mooshroom cow, $2% for 2, and $3% for 3.");
        setUsage("/skill mushroomcow");
        setIdentifiers("skill mushroomc", "skill mushroomcow", "skill mcow", "skill summonmooshroom", "skill mooshroom", "skill summonmushroomcow", "skill mushroomcow");
    }

    @Override
    protected EntityType getEntityType(Block targetBlock) {
        return EntityType.MUSHROOM_COW;
    }
    
    @Override
    protected void applySoundEffects(World world, Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_COW_HURT , 0.8F, 1.0F);
    }
}
