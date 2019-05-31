package com.herocraftonline.heroes.characters.skill.reborn.professions;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseSummonEntity;
import com.herocraftonline.heroes.util.CompatSound;

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
        player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_COW_HURT.value() , 0.8F, 1.0F); 
    }
}