package com.herocraftonline.heroes.characters.skill.remastered.profs;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseForgeItem;
import org.bukkit.Material;

import java.util.logging.Level;

public class SkillForgeShield extends SkillBaseForgeItem {

    public SkillForgeShield(Heroes plugin) {
        super(plugin, "ForgeShield");
        setDescription("You forge a mighty shield!!");
        setUsage("/skill forgeshield");
        setArgumentRange(0, 0);
        setIdentifiers("skill forgeshield");
        setTypes(SkillType.ITEM_CREATION);
        defaultAmount = 1;
        try {
            deafultItem = Material.valueOf("SHIELD");
        } catch (IllegalArgumentException e) {
            Heroes.log(Level.SEVERE, "The server is not compatible with this skill");
        }
    }
}