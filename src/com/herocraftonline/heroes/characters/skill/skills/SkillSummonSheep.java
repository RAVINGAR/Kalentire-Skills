package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.DyeColor;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseSummonEntity;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Util;

public class SkillSummonSheep extends SkillBaseSummonEntity {

    public SkillSummonSheep(Heroes plugin) {
        super(plugin, "SummonSheep");
        setDescription("100% chance to spawn 1 sheep, $2% for 2, and $3% for 3.");
        setUsage("/skill sheep");
        setIdentifiers("skill summonsheep", "skill sheep");
    }

    @Override
    public String getDescription(Hero hero) {
        int chance2x = (int) (SkillConfigManager.getUseSetting(hero, this, "chance-2x", 0.2, false) * 100 + SkillConfigManager.getUseSetting(hero, this, "chance-2x-per-level", 0.0, false) * hero.getLevel());
        int chance3x = (int) (SkillConfigManager.getUseSetting(hero, this, "chance-3x", 0.1, false) * 100 + SkillConfigManager.getUseSetting(hero, this, "chance-3x-per-level", 0.0, false) * hero.getLevel());
        return getDescription().replace("$2", chance2x + "").replace("$3", chance3x + "");
    }

    @Override
    protected EntityType getEntityType(Block targetBlock) {
        return EntityType.SHEEP;
    }
    
    @Override
    protected Entity summonEntity(Hero hero, String[] args, Block targetBlock) {
        Sheep sheep = (Sheep) hero.getPlayer().getWorld().spawnEntity(targetBlock.getLocation(), getEntityType(targetBlock));
        sheep.setColor(DyeColor.getByData((byte) Util.nextInt(DyeColor.values().length)));
        return sheep;
    }
    
    @Override
    protected void applySoundEffects(World world, Player player) {
        player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_SHEEP_AMBIENT.value() , 0.8F, 1.0F); 
    }
}
