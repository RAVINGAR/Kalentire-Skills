package com.herocraftonline.heroes.characters.skill.pack2;

import org.bukkit.ChatColor;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.skills.*;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseRunestone;

public class SkillMajorRunestone extends SkillBaseRunestone {

    public SkillMajorRunestone(Heroes plugin) {
        super(plugin, "MajorRunestone");
        setDescription("You imbue a redstone block with an Major Runestone. Major Runestones $1");
        setUsage("/skill majorrunestone");
        setArgumentRange(0, 0);
        setIdentifiers("skill majorrunestone");
        setTypes(SkillType.ITEM_MODIFYING, SkillType.SILENCEABLE);

        defaultMaxUses = 8;
        defaultDelay = 5000;
        displayName = "Major Runestone";
        displayNameColor = ChatColor.BLUE;

        new RunestoneListener();
    }
}