package com.herocraftonline.heroes.characters.skill.unusedskills;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.base.SkillBaseSummonEntity;
import com.herocraftonline.heroes.util.Util;

public class SkillSummonVillager extends SkillBaseSummonEntity {

    private static final List<Profession> PROFESSIONS = Lists.newArrayList(Profession.FARMER, Profession.LIBRARIAN, Profession.PRIEST, Profession.BLACKSMITH,
            Profession.BUTCHER);
    
    public SkillSummonVillager(Heroes plugin) {
        super(plugin, "SummonVillager");
        setDescription("100% chance to spawn 1 villager, $2% for 2, and $3% for 3. Profession will be as specified, or random if none is specified.");
        setUsage("/skill villager [profession]");
        setArgumentRange(0, 1);
        setIdentifiers("skill summonvillager", "skill villager");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        SkillResult result = getProfession(args) != null ? super.use(hero, args) : SkillResult.FAIL;
        
        if (SkillResult.FAIL.equals(result)) {
            hero.getPlayer().sendMessage(ChatColor.RED + "Invalid villager profession!");
        }
        
        return result;
    }
    
    @Override
    protected Entity summonEntity(Hero hero, String[] args, Block targetBlock)
    {
        Entity villager = hero.getPlayer().getWorld().spawnEntity(targetBlock.getLocation(), getEntityType(targetBlock));
        ((Villager) villager).setProfession(getProfession(args));
        return villager;
    }
    
    private Profession getProfession(String args[]) {
        Profession profession = null;

        if (args.length > 0) {
            try {
                profession = "SMITH".equalsIgnoreCase(args[0]) ? Profession.BLACKSMITH : Profession.valueOf(args[0].toUpperCase());
            }
            catch (Exception e) {
                // do nothing
            }
        }
        else {
            profession = PROFESSIONS.get(Util.nextInt(PROFESSIONS.size()));
        }

        return profession;
    }

    @Override
    protected EntityType getEntityType(Block targetBlock) {
        return EntityType.VILLAGER;
    }
    
    @Override
    protected void applySoundEffects(World world, Player player) {
        //player.getWorld().playSound(player.getLocation(), Sound.VILLAGER_HAGGLE , 0.8F, 1.0F);
    }
}