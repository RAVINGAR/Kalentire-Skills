package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseSteed;
import org.bukkit.entity.Horse.Color;

public class SkillHolySteed extends SkillBaseSteed {

    public SkillHolySteed(Heroes plugin) {
        super(plugin, "HolySteed");
        setDescription("Summons a holy steed for $1");
        setIdentifiers("skill holysteed");
        setUsage("/skill holysteed");
        setArgumentRange(0,0);

        steedName = "HolySteed";
        steedColor = Color.WHITE;

        new SteedListener();
    }
}