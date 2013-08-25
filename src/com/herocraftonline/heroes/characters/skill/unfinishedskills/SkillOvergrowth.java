package com.herocraftonline.heroes.characters.skill.unfinishedskills;

import java.util.HashSet;

import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.TreeType;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillOvergrowth extends ActiveSkill {

    public SkillOvergrowth(Heroes plugin) {
        super(plugin, "Overgrowth");
        setDescription("You turn a sapling into a full grown tree.");
        setUsage("/skill overgrowth");
        setArgumentRange(0, 0);
        setIdentifiers("skill overgrowth", "skill ogrowth");
        setTypes(SkillType.SILENCABLE, SkillType.EARTH);
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
        Block targetBlock = player.getTargetBlock((HashSet<Byte>) null, range);
        Material mat = targetBlock.getType();
        TreeType tType = null;
        if (mat == Material.SAPLING) {
            tType = null;
            switch (targetBlock.getData()) {
                case 0x0:
                    if (Util.nextInt(2) == 0) {
                        tType = TreeType.TREE;
                    } else {
                        tType = TreeType.BIG_TREE;
                    }
                    break;
                case 0x1:
                    if (Util.nextInt(2) == 0) {
                        tType = TreeType.REDWOOD;
                    } else {
                        tType = TreeType.TALL_REDWOOD;
                    }
                    break;
                case 0x2:
                    tType = TreeType.BIRCH;
                    break;
                case 0x3:
                    tType = TreeType.JUNGLE;
                    break;
                default:
                    tType = TreeType.TREE;
            };
        } else if (mat == Material.RED_MUSHROOM) {
            tType = TreeType.RED_MUSHROOM;
        } else if (mat == Material.BROWN_MUSHROOM) {
            tType = TreeType.BROWN_MUSHROOM;
        } else {
            Messaging.send(player, "Target is not a sapling!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        byte data = targetBlock.getData();
        targetBlock.setType(Material.AIR);
        if (!player.getWorld().generateTree(targetBlock.getLocation(), tType)) {
            targetBlock.setTypeIdAndData(mat.getId(), data, false);
            Messaging.send(player, "The spell fizzled!");
            return SkillResult.FAIL;
        }
        player.getWorld().playEffect(player.getLocation(), Effect.EXTINGUISH, 3);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.AMBIENCE_RAIN , 0.8F, 1.0F);
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
        
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

}
