package com.herocraftonline.heroes.characters.skill.remastered;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseSummonEntity;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class SkillSummonCow extends SkillBaseSummonEntity {

    public SkillSummonCow(Heroes plugin) {
        super(plugin, "SummonCow");
        setDescription("100% chance to spawn 1 cow, $2% for 2, and $3% for 3.");
        setUsage("/skill cow");
        setIdentifiers("skill summoncow", "skill cow");
    }
    
    @Override
    protected EntityType getEntityType(Block targetBlock) {
        //FIXME Find all the mushroom biome stuff?
        return EntityType.COW;
//        return targetBlock.getType() == Material.HUGE_MUSHROOM_1 ||
//                targetBlock.getType() == Material.HUGE_MUSHROOM_2 ||
//                targetBlock.getType() == Material.MYCELIUM ? EntityType.MUSHROOM_COW : EntityType.COW;
    }
    
    @Override
    protected void applySoundEffects(World world, Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_COW_AMBIENT, 0.8F, 1.0F);
    }
}
