package com.herocraftonline.heroes.characters.skill.public1;

import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.base.SkillBaseSummonEntity;

public class SkillSummonChicken extends SkillBaseSummonEntity {

    public SkillSummonChicken(Heroes plugin) {
        super(plugin, "SummonChicken");
        setDescription("100% chance to spawn 1 chicken, $1% for 2, and $2% for 3.");
        setUsage("/skill chicken");
        setIdentifiers("skill summonchicken", "skill chicken");
        setTypes(SkillType.SUMMONING, SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_EARTH);
    }

    @Override
    protected EntityType getEntityType(Block targetBlock) {
        return EntityType.CHICKEN;
    }
}
