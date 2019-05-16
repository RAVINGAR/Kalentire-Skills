package com.herocraftonline.heroes.characters.skill.reborn.professions;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseSummonEntity;
import com.herocraftonline.heroes.util.CompatSound;

public class SkillSummonPig extends SkillBaseSummonEntity {

    public SkillSummonPig(Heroes plugin) {
        super(plugin, "SummonPig");
        setDescription("100% chance to spawn 1 pig, $2% for 2, and $3% for 3.");
        setUsage("/skill pig");
        setIdentifiers("skill summonpig", "skill pig");
    }

    @Override
    protected EntityType getEntityType(Block targetBlock) {
        return EntityType.PIG;
    }
    
    @Override
    protected void applySoundEffects(World world, Player player) {
        player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_PIG_AMBIENT.value() , 0.8F, 1.0F); 
    }
}
