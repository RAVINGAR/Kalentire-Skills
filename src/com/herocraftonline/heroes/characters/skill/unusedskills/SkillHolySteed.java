package com.herocraftonline.heroes.characters.skill.unusedskills;

import com.herocraftonline.heroes.characters.skill.skills.SkillBaseSteed;
import org.bukkit.entity.Horse.Color;
import org.bukkit.entity.Horse.Variant;

import com.herocraftonline.heroes.Heroes;

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