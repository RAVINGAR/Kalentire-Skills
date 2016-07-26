package com.herocraftonline.heroes.characters.skill.public1;

import org.bukkit.Material;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillForgeChainHelmet extends com.herocraftonline.heroes.characters.skill.skills.SkillBaseForgeItem {

    public SkillForgeChainHelmet(Heroes plugin) {
        super(plugin, "ForgeChainHelmet");
        setDescription("You forge a chain helmet!");
        setUsage("/skill forgechainchest");
        setArgumentRange(0, 0);
        setIdentifiers("skill forgechainhelmet", "skill chainhelm");
        setTypes(SkillType.ITEM_CREATION);
        defaultAmount = 2;
        deafultItem = Material.CHAINMAIL_HELMET;
    }
}