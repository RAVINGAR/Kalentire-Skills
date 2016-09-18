package com.herocraftonline.heroes.characters.skill.public1;

import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.base.SkillBaseSummonEntity;

public class SkillSummonPig extends SkillBaseSummonEntity {

    public SkillSummonPig(Heroes plugin) {
        super(plugin, "SummonPig");
        setDescription("100% chance to spawn 1 pig, $1% for 2, and $2% for 3.");
        setUsage("/skill pig");
        setIdentifiers("skill summonpig", "skill pig");
        setTypes(SkillType.SUMMONING, SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_EARTH);
    }
    
    @Override
    protected EntityType getEntityType(Block targetBlock) {
        return EntityType.PIG;
    }
}
