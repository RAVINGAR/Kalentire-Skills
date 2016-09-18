package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.base.SkillBaseSummonEntity;
import com.herocraftonline.heroes.util.CompatSound;

public class SkillSummonCow extends SkillBaseSummonEntity {

    public SkillSummonCow(Heroes plugin) {
        super(plugin, "SummonCow");
        setDescription("100% chance to spawn 1 cow, $2% for 2, and $3% for 3.");
        setUsage("/skill cow");
        setIdentifiers("skill summoncow", "skill cow");
    }
    
    @Override
    protected EntityType getEntityType(Block targetBlock) {
        return targetBlock.getType() == Material.HUGE_MUSHROOM_1 || 
                targetBlock.getType() == Material.HUGE_MUSHROOM_2 || 
                targetBlock.getType() == Material.MYCEL ? EntityType.MUSHROOM_COW : EntityType.COW;
    }
    
    @Override
    protected void applySoundEffects(World world, Player player) {
        player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_COW_AMBIENT.value(), 0.8F, 1.0F);         
    }
}
