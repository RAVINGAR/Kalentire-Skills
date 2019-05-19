package com.herocraftonline.heroes.characters.skill.reborn.professions;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseSummonEntity;
import com.herocraftonline.heroes.util.CompatSound;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class SkillSummonSquid extends SkillBaseSummonEntity {

    public SkillSummonSquid(Heroes plugin) {
        super(plugin, "SummonSquid");
        setDescription("100% chance to spawn 1 squid, $2% for 2, and $3% for 3.");
        setUsage("/skill squid");
        setIdentifiers("skill summonsquid", "skill squid");
    }

    @Override
    protected EntityType getEntityType(Block targetBlock) {
        return EntityType.SQUID;
    }
    
    @Override
    protected void applySoundEffects(World world, Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SQUID_HURT, 0.8F, 1.0F);
    }

}
