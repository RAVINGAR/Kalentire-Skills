package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseForgeItem;
import org.bukkit.Material;

public class SkillForgeChainHelmet extends SkillBaseForgeItem {

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