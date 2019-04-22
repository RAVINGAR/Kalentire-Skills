package com.herocraftonline.heroes.characters.skill.pack4;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.Sound;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseSummonEntity;

public class SkillSummonRabbit extends SkillBaseSummonEntity {

    public SkillSummonRabbit(Heroes plugin) {
        super(plugin, "SummonRabbit");
        setDescription("100% chance to spawn 1 rabbit, $2% for 2, and $3% for 3.");
        setUsage("/skill rabbit");
        setIdentifiers("skill summonrabbit", "skill rabbit");
    }

    @Override
    protected EntityType getEntityType(Block targetBlock) {
        return EntityType.RABBIT;
    }
    
    @Override
    protected void applySoundEffects(World world, Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_BURN , 0.8F, 1.0F);
    }
}
