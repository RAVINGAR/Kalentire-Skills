package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;

public class SkillMarkShop extends SkillMark {

    public SkillMarkShop(Heroes plugin) {
        super(plugin, "MarkShop");
        setDescription("You mark a Shop location for use with Recall Shop.");
        setUsage("/skill markshop <info|reset>");
        setIdentifiers("skill markshop");
        skillSettingsName = "RecallShop";
    }
}
