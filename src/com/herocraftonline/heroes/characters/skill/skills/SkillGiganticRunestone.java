package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.ChatColor;

import com.herocraftonline.heroes.Heroes;

public class SkillGiganticRunestone extends SkillAbstractRunestone {

    public SkillGiganticRunestone(Heroes plugin) {
        super(plugin, "GiganticRunestone");
        setDescription("You imbue a redstone block with an Gigantic Runestone. Gigantic Runestones $1");
        setUsage("/skill giganticrunestone");
        setIdentifiers("skill giganticrunestone");

        defaultMaxUses = 15;
        defaultDelay = 8000;
        displayName = "Gigantic Runestone";
        displayNameColor = ChatColor.YELLOW;

        registerListener();
    }
}