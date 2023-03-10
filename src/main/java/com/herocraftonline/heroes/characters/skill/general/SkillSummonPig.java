package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseSummonEntity;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class SkillSummonPig extends SkillBaseSummonEntity {

    public SkillSummonPig(Heroes plugin) {
        super(plugin, "SummonPig");
        setDescription("100% chance to spawn 1 pig, $2% for 2, and $3% for 3.");
        setUsage("/skill pig");
        setIdentifiers("skill summonpig", "skill pig");
    }

    @Override
    protected EntityType getEntityType(Block targetBlock) {
        return EntityType.PIG;
    }
    
    @Override
    protected void applySoundEffects(World world, Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PIG_AMBIENT , 0.8F, 1.0F);
    }
}
