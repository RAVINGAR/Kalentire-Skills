package com.herocraftonline.heroes.characters.skill.public1;

import org.bukkit.DyeColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Sheep;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseSummonEntity;
import com.herocraftonline.heroes.util.Util;

public class SkillSummonSheep extends SkillBaseSummonEntity {

    public SkillSummonSheep(Heroes plugin) {
        super(plugin, "SummonSheep");
        setDescription("100% chance to spawn 1 sheep, $1% for 2, and $2% for 3.");
        setUsage("/skill sheep");
        setIdentifiers("skill summonsheep", "skill sheep");
        setTypes(SkillType.SUMMONING, SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_EARTH);
    }
    
    @Override
    protected Entity summonEntity(Hero hero, String[] args, Block targetBlock) {
        Sheep sheep = (Sheep) hero.getPlayer().getWorld().spawnEntity(targetBlock.getLocation(), getEntityType(targetBlock));
        sheep.setColor(DyeColor.getByData((byte) Util.nextInt(DyeColor.values().length)));
        return sheep;
    }
    
    @Override
    protected EntityType getEntityType(Block targetBlock) {
        return EntityType.SHEEP;
    }
}
