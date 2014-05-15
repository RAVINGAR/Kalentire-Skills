package com.herocraftonline.heroes.characters.skill.skills;

import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;

public class SkillSummonVillager extends ActiveSkill {

    public SkillSummonVillager(Heroes plugin) {
        super(plugin, "SummonVillager");
        setDescription("100% chance to spawn 1 villager, $2% for 2, and $3% for 3. Profession will be as specified, or random if none is specified.");
        setUsage("/skill villager [profession]");
        setArgumentRange(0, 1);
        setIdentifiers("skill summonvillager", "skill villager");
        setTypes(SkillType.SUMMONING, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        int chance2x = (int) (SkillConfigManager.getUseSetting(hero, this, "chance-2x", 0.2, false) * 100 + SkillConfigManager.getUseSetting(hero, this, "chance-2x-per-level", 0.0, false) * hero.getLevel());
        int chance3x = (int) (SkillConfigManager.getUseSetting(hero, this, "chance-3x", 0.1, false) * 100 + SkillConfigManager.getUseSetting(hero, this, "chance-3x-per-level", 0.0, false) * hero.getLevel());
        return getDescription().replace("$2", chance2x + "").replace("$3", chance3x + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("chance-2x", 0.2);
        node.set("chance-3x", 0.1);
        node.set(SkillSetting.MAX_DISTANCE.node(), 20);
        node.set("chance-2x-per-level", 0.0);
        node.set("chance-3x-per-level", 0.0);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        double chance2x = SkillConfigManager.getUseSetting(hero, this, "chance-2x", 0.2, false) + (int) SkillConfigManager.getUseSetting(hero, this, "chance-2x-per-level", 0.0, false) * hero.getSkillLevel(this);
        double chance3x = SkillConfigManager.getUseSetting(hero, this, "chance-3x", 0.1, false) + (int) SkillConfigManager.getUseSetting(hero, this, "chance-3x-per-level", 0.0, false) * hero.getSkillLevel(this);
        int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 20, false);
        Block wTargetBlock = player.getTargetBlock(null, distance).getRelative(BlockFace.UP);
        double chance = Util.nextRand();
        Profession profession = null;
        if(args.length == 1) {
            String profName = args[0];
            if(profName.equalsIgnoreCase("SMITH")) {
                profession = Profession.BLACKSMITH;
            }
            else {
                for(Profession prof : Profession.values()) {
                    if(prof.name().equalsIgnoreCase(profName)) {
                        profession = prof;
                    }
                }
            }
            if(profession == null) {
                hero.getPlayer().sendMessage(ChatColor.RED + "Invalid villager profession!");
                return SkillResult.FAIL;
            }
        }
        else {
            int randomProf = Util.nextInt(4);
            if(randomProf == 0) {
                profession = Profession.FARMER;
            }
            else if(randomProf == 1) {
                profession = Profession.LIBRARIAN;
            }
            else if(randomProf == 2) {
                profession = Profession.PRIEST;
            }
            else if(randomProf == 3) {
                profession = Profession.BLACKSMITH;
            }
            else if(randomProf == 4) {
                profession = Profession.BUTCHER;
            }
        }
        ((Villager) player.getWorld().spawnEntity(wTargetBlock.getLocation(), EntityType.VILLAGER)).setProfession(profession);
        if (chance <= chance3x) {
            ((Villager) player.getWorld().spawnEntity(wTargetBlock.getLocation(), EntityType.VILLAGER)).setProfession(profession);
            ((Villager) player.getWorld().spawnEntity(wTargetBlock.getLocation(), EntityType.VILLAGER)).setProfession(profession);
        } else if (chance <= chance2x) {
            ((Villager) player.getWorld().spawnEntity(wTargetBlock.getLocation(), EntityType.VILLAGER)).setProfession(profession);
        }
        //hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.VILLAGER_HAGGLE , 0.8F, 1.0F);
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }


}