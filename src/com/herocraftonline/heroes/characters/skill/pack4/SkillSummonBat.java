package com.herocraftonline.heroes.characters.skill.pack4;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseSummonEntity;
import com.herocraftonline.heroes.util.CompatSound;

public class SkillSummonBat extends SkillBaseSummonEntity {

    public SkillSummonBat(Heroes plugin) {
        super(plugin, "SummonBat");
        setDescription("100% chance to spawn 1 bat, $2% for 2, and $3% for 3.");
        setUsage("/skill bat");
        setIdentifiers("skill summonbat", "skill bat");
    }

    @Override
    protected EntityType getEntityType(Block targetBlock) {
        return EntityType.BAT;
    }
    
    @Override
    protected void applySoundEffects(World world, Player player) {
        player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_BAT_TAKEOFF.value(), 0.8F, 1.0F); 
    }
}
