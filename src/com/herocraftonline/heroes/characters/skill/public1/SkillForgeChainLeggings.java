package com.herocraftonline.heroes.characters.skill.public1;

import org.bukkit.Material;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillForgeChainLeggings extends com.herocraftonline.heroes.characters.skill.skills.SkillBaseForgeItem {

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