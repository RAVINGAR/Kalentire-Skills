package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseSteed;
import org.bukkit.entity.Horse.Color;

public class SkillDreadSteed extends SkillBaseSteed {
    
    public SkillDreadSteed(Heroes plugin) {
        super(plugin, "DreadSteed");
        setDescription("Summons a dread steed for $1");
        setIdentifiers("skill dreadsteed");
        setUsage("/skill dreadsteed");
        setArgumentRange(0,0);

        steedName = "DreadSteed";
        steedColor = Color.BLACK;

        new SteedListener();
    }
}