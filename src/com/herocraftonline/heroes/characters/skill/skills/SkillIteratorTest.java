package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillIteratorTest extends ActiveSkill {

    public SkillIteratorTest(Heroes plugin) {
        super(plugin, "IteratorTest");
        setDescription("Delf's block test");
        setUsage("/skill iteratortest <Test>");
        setArgumentRange(1, 1);
        setIdentifiers("skill iteratortest");
        setTypes(SkillType.BLOCK_CREATING);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 8);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 6, false);

        Block b;
        BlockIterator iter = null;
        try {
            iter = new BlockIterator(player, distance);
        }
        catch (IllegalStateException e) {
            Messaging.send(player, "Errors yo.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        if (args[0].equals("1")) {
            while (iter.hasNext()) {
                b = iter.next();
                if (Util.transparentBlocks.contains(b.getType())) {
                    b.setType(Material.GOLD_BLOCK);
                }
                else {
                    break;
                }
            }
            return SkillResult.NORMAL;
        }
        else if (args[0].equals("2")) {
            while (iter.hasNext()) {
                b = iter.next();
                if (Util.transparentBlocks.contains(b.getType()) && (Util.transparentBlocks.contains(b.getRelative(BlockFace.UP).getType()) || Util.transparentBlocks.contains(b.getRelative(BlockFace.DOWN).getType()))) {
                    b.setType(Material.GOLD_BLOCK);
                }
                else {
                    break;
                }
            }
            return SkillResult.NORMAL;
        }
        else {
            while (iter.hasNext()) {
                b = iter.next();
                if (Util.transparentBlocks.contains(b.getRelative(BlockFace.DOWN).getType())) {
                    b.setType(Material.GOLD_BLOCK);
                }
                else {
                    break;
                }
            }
            return SkillResult.NORMAL;
        }
    }
}