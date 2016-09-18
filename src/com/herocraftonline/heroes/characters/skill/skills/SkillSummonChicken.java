package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.base.SkillBaseSummonEntity;
import com.herocraftonline.heroes.util.CompatSound;

public class SkillSummonChicken extends SkillBaseSummonEntity {

    public SkillSummonChicken(Heroes plugin) {
        super(plugin, "SummonChicken");
        setDescription("100% chance to spawn 1 chicken, $2% for 2, and $3% for 3.");
        setUsage("/skill chicken");
        setIdentifiers("skill summonchicken", "skill chicken");
    }

    @Override
    protected EntityType getEntityType(Block targetBlock) {
        return EntityType.CHICKEN;
    }
    
    @Override
    protected void applySoundEffects(World world, Player player) {
        player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_CHICKEN_HURT.value(), 0.8F, 1.0F); 
    }

}
