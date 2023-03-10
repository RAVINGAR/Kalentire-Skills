package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseForgeItem;
import org.bukkit.Material;

public class SkillForgeChainChest extends SkillBaseForgeItem {

    public SkillForgeChainChest(Heroes plugin) {
        super(plugin, "ForgeChainChest");
        setDescription("You forge a chain chestplate!");
        setUsage("/skill forgechainchest");
        setArgumentRange(0, 0);
        setIdentifiers("skill forgechainchest");
        setTypes(SkillType.ITEM_CREATION);
        defaultAmount = 1;
        deafultItem = Material.CHAINMAIL_CHESTPLATE;
    }
}