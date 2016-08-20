package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Material;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.base.SkillBaseForgeItem;

public class SkillForgeChainLeggings extends SkillBaseForgeItem {

    public SkillForgeChainLeggings(Heroes plugin) {
        super(plugin, "ForgeChainLeggings");
        setDescription("You forge a chain leggings!");
        setUsage("/skill forgechainchest");
        setArgumentRange(0, 0);
        setIdentifiers("skill forgechainleggings");
        setTypes(SkillType.ITEM_CREATION);
        defaultAmount = 1;
        deafultItem = Material.CHAINMAIL_LEGGINGS;
    }
}