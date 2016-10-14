package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import org.bukkit.entity.Horse.Color;
import org.bukkit.entity.Horse.Variant;

public class SkillDreadSteed extends SkillBaseSteed {
    
    public SkillDreadSteed(Heroes plugin) {
        super(plugin, "DreadSteed");
        setDescription("Summons a dread steed for $1");
        setIdentifiers("skill dreadsteed");
        setUsage("/skill dreadsteed");
        setArgumentRange(0,0);

        steedName = "DreadSteed";
        steedVariant = Variant.HORSE;
        steedColor = Color.BLACK;

        new SteedListener();
    }
}