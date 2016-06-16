package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Material;

public class SkillForgeShield extends SkillBaseForgeItem {

    public SkillForgeShield(Heroes plugin) {
        super(plugin, "ForgeShield");
        setDescription("You forge a mighty shield!!");
        setUsage("/skill FORGESHIELD");
        setArgumentRange(0, 0);
        setIdentifiers("skill FORGESHIELD");
        setTypes(SkillType.ITEM_CREATION);
        defaultAmount = 1;
        deafultItem = Material.SHIELD;
    }
}