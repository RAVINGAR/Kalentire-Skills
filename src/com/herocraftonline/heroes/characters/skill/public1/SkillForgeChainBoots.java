package com.herocraftonline.heroes.characters.skill.public1;

import org.bukkit.Material;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillForgeChainBoots extends com.herocraftonline.heroes.characters.skill.skills.SkillBaseForgeItem {

    public SkillForgeChainBoots(Heroes plugin) {
        super(plugin, "ForgeChainBoots");
        setDescription("You forge chain boots!");
        setUsage("/skill forgechainchest");
        setArgumentRange(0, 0);
        setIdentifiers("skill forgechainboots");
        setTypes(SkillType.ITEM_CREATION);
        defaultAmount = 2;
        deafultItem = Material.CHAINMAIL_BOOTS;
    }
}