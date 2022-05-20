package com.herocraftonline.heroes.characters.skill.remastered;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseRunestone;
import org.bukkit.ChatColor;

public class SkillMinorRunestone extends SkillBaseRunestone {

    public SkillMinorRunestone(Heroes plugin) {
        super(plugin, "MinorRunestone");
        setDescription("You imbue a redstone block with an Minor Runestone. Minor Runestones $1");
        setUsage("/skill minorrunestone");
        setArgumentRange(0, 0);
        setIdentifiers("skill minorrunestone");
        setTypes(SkillType.ITEM_MODIFYING, SkillType.SILENCEABLE);

        defaultMaxUses = 2;
        defaultDelay = 5000;
        displayName = "Minor Runestone";
        displayNameColor = ChatColor.GREEN;

        new RunestoneListener();
    }
}