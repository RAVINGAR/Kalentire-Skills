package com.herocraftonline.heroes.characters.skill.general;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseSummonEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class SkillSummonHorse extends SkillBaseSummonEntity {

    public SkillSummonHorse(Heroes plugin) {
        super(plugin, "SummonHorse");
        setDescription("100% chance to spawn 1 Horse, Donkey or Mule, $2% for 2, and $3% for 3.");
        setUsage("/skill horse");
        setIdentifiers("skill summonhorse", "skill horse");
    }

    @Override
    protected EntityType getEntityType(Block targetBlock) {
        List<EntityType> horses = Arrays.asList(EntityType.HORSE, EntityType.MULE, EntityType.DONKEY);
        Random rand = new Random();

        return horses.get(rand.nextInt(horses.size()));
    }
    
    @Override
    protected void applySoundEffects(World world, Player player) {
        //player.getWorld().playSound(player.getLocation(), Sound.HORSE_SOFT , 0.8F, 1.0F);
    }
}
