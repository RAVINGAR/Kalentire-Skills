package com.herocraftonline.heroes.characters.skill.remastered;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseSummonEntity;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

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
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_CHICKEN_HURT, 0.8F, 1.0F);
    }

}
