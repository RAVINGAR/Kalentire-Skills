package com.herocraftonline.dev.heroes.skill.skills;

import java.util.HashSet;

import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.herocraftonline.dev.heroes.util.Setting;
import com.herocraftonline.dev.heroes.util.Util;

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
        node.set(Setting.MAX_DISTANCE.node(), 15);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        int range = SkillConfigManager.getUseSetting(hero, this, Setting.MAX_DISTANCE, 15, false);
        if (player.getTargetBlock((HashSet<Byte>) null, range).getType() == Material.SAPLING) {
            Block targetBlock = player.getTargetBlock((HashSet<Byte>) null, range);
            TreeType tType = null;

            switch (targetBlock.getData()) {
                case 0x0:
                    if (Util.rand.nextInt(2) == 0) {
                        tType = TreeType.TREE;
                    } else {
                        tType = TreeType.BIG_TREE;
                    }
                    break;
                case 0x1:
                    if (Util.rand.nextInt(2) == 0) {
                        tType = TreeType.REDWOOD;
                    } else {
                        tType = TreeType.TALL_REDWOOD;
                    }
                    break;
                case 0x2:
                    tType = TreeType.BIRCH;
                    break;
                default:
                    tType = TreeType.TREE;
            }
            Material sapling = targetBlock.getType();
            byte data = targetBlock.getData();
            targetBlock.setType(Material.AIR);
            if (!player.getWorld().generateTree(targetBlock.getLocation(), tType)) {
                targetBlock.setTypeIdAndData(sapling.getId(), data, false);
                Messaging.send(player, "The spell fizzled!");
                return SkillResult.FAIL;
            }
            broadcastExecuteText(hero);
            return SkillResult.NORMAL;
        } else {
            Messaging.send(player, "Target is not a sapling!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

}
