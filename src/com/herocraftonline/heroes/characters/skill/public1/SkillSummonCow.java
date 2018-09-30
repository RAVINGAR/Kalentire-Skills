package com.herocraftonline.heroes.characters.skill.public1;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseSummonEntity;

public class SkillSummonCow extends SkillBaseSummonEntity {

    public SkillSummonCow(Heroes plugin) {
        super(plugin, "SummonCow");
        setDescription("100% chance to spawn 1 cow, $1% for 2, and $2% for 3.");
        setUsage("/skill cow");
        setIdentifiers("skill summoncow", "skill cow");
        setTypes(SkillType.SUMMONING, SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_EARTH);
    }
    
    @Override
    protected EntityType getEntityType(Block targetBlock) {
        //FIXME Find all the mushroom biome stuff?
        return (targetBlock.getType() == Material.HUGE_MUSHROOM_1) || (targetBlock.getType() == Material.HUGE_MUSHROOM_2)
                || (targetBlock.getType() == Material.MYCELIUM) ? EntityType.MUSHROOM_COW : EntityType.COW;
    }
}
