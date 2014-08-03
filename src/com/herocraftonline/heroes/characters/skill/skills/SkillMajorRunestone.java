package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.ChatColor;

import com.herocraftonline.heroes.Heroes;

public class SkillMajorRunestone extends SkillAbstractRunestone {

    public SkillMajorRunestone(Heroes plugin) {
        super(plugin, "MajorRunestone");
        setDescription("You imbue a redstone block with an Major Runestone. Major Runestones $1");
        setUsage("/skill majorrunestone");
        setIdentifiers("skill majorrunestone");

        defaultMaxUses = 8;
        defaultDelay = 5000;
        displayName = "Major Runestone";
        displayNameColor = ChatColor.BLUE;

        registerListener();
    }
}