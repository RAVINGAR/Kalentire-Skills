package com.herocraftonline.heroes.characters.skill.unusedskills;

import org.bukkit.entity.Horse.Color;
import org.bukkit.entity.Horse.Variant;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseSteed;

public class SkillHolySteed extends SkillBaseSteed {

    public SkillHolySteed(Heroes plugin) {
        super(plugin, "HolySteed");
        setDescription("Summons a holy steed for $1");
        setIdentifiers("skill holysteed");
        setUsage("/skill holysteed");
        setArgumentRange(0,0);

        steedName = "HolySteed";
        steedVariant = Variant.HORSE;
        steedColor = Color.WHITE;

        new SteedListener();
    }
}