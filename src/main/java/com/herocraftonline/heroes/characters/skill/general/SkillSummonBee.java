package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseSummonEntity;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class SkillSummonBee extends SkillBaseSummonEntity {

    public SkillSummonBee(Heroes plugin) {
        super(plugin, "SummonBee");
        setDescription("100% chance to spawn 1 Bee, $2% for 2, and $3% for 3.");
        setUsage("/skill bee");
        setIdentifiers("skill summonbee", "skill bee");
    }

    @Override
    protected EntityType getEntityType(Block targetBlock) {
        return EntityType.BEE;
    }

    @Override
    protected void applySoundEffects(World world, Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BEE_LOOP, 0.8F, 1.0F);
    }
}