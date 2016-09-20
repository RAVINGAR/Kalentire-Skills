package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseSummonEntity;
import com.herocraftonline.heroes.util.CompatSound;

public class SkillSummonCat extends SkillBaseSummonEntity {

    public SkillSummonCat(Heroes plugin) {
        super(plugin, "SummonCat");
        setDescription("100% chance to spawn 1 cat, $2% for 2, and $3% for 3.");
        setUsage("/skill cat");
        setIdentifiers("skill summoncat", "skill cat");
    }
    
    @Override
    protected EntityType getEntityType(Block targetBlock) {
        return EntityType.OCELOT;
    }
    
    @Override
    protected void applySoundEffects(World world, Player player) {
        player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_CAT_PURREOW.value(), 0.8F, 1.0F);
    }
}
