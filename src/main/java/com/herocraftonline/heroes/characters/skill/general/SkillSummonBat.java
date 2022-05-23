package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseSummonEntity;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

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
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 0.8F, 1.0F);
    }
}
