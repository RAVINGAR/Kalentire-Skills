package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Sapling;

import java.util.HashSet;

public class SkillOvergrowth extends ActiveSkill {

    public SkillOvergrowth(Heroes plugin) {
        super(plugin, "Overgrowth");
        setDescription("You turn a sapling into a full grown tree.");
        setUsage("/skill overgrowth");
        setArgumentRange(0, 0);
        setIdentifiers("skill overgrowth");
        setTypes(SkillType.ABILITY_PROPERTY_EARTH);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 15);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        int range = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 15, false);
        Block targetBlock = player.getTargetBlock((HashSet<Material>)null, range);
        Material mat = targetBlock.getType();
        TreeType tType = null;
        //FIXME Anoying flattening will deal with later
//        if (mat == Material.SAPLING) {
//            MaterialData matDat = targetBlock.getState().getData();
//            if(!(matDat instanceof Sapling)) {
//                player.sendMessage(ChatColor.GRAY + "This doesn't look like a tree... (ERROR)");
//                return SkillResult.FAIL;
//            }
//            Sapling sDat = (Sapling) matDat;
//            switch (sDat.getSpecies()) {
//                case GENERIC:
//                    if (Util.nextInt(2) == 0) {
//                        tType = TreeType.TREE;
//                    } else {
//                        tType = TreeType.BIG_TREE;
//                    }
//                    break;
//                case REDWOOD:
//                    if (Util.nextInt(2) == 0) {
//                        tType = TreeType.REDWOOD;
//                    } else {
//                        tType = TreeType.TALL_REDWOOD;
//                    }
//                    break;
//                case BIRCH:
//                    if (Util.nextInt(2) == 0) {
//                        tType = TreeType.BIRCH;
//                    } else {
//                        tType = TreeType.TALL_BIRCH;
//                    }
//                    break;
//                case JUNGLE:
//                    tType = TreeType.JUNGLE;
//                    break;
//                case ACACIA:
//                    tType = TreeType.ACACIA;
//                    break;
//                case DARK_OAK:
//                    tType = TreeType.DARK_OAK;
//                    break;
//                default:
//                    tType = TreeType.TREE;
//            }
//        } else if (mat == Material.RED_MUSHROOM) {
//            tType = TreeType.RED_MUSHROOM;
//        } else if (mat == Material.BROWN_MUSHROOM) {
//            tType = TreeType.BROWN_MUSHROOM;
//        } else {
//            player.sendMessage("Target is not a sapling!");
//            return SkillResult.INVALID_TARGET_NO_MSG;
//        }
//        byte data = targetBlock.getData();
//        targetBlock.setType(Material.AIR);
//        if (!player.getWorld().generateTree(targetBlock.getLocation(), tType)) {
//            targetBlock.setTypeIdAndData(mat.getId(), data, false);
//            player.sendMessage("The spell fizzled!");
//            return SkillResult.FAIL;
//        }
//        player.getWorld().playEffect(player.getLocation(), Effect.EXTINGUISH, 3);
//        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.WEATHER_RAIN.value() , 0.8F, 1.0F);
//        broadcastExecuteText(hero);
        return SkillResult.NORMAL;

    }
}