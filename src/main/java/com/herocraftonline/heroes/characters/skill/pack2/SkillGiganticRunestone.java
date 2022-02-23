package com.herocraftonline.heroes.characters.skill.pack2;

import org.bukkit.ChatColor;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseRunestone;

public class SkillGiganticRunestone extends SkillBaseRunestone {

    public SkillGiganticRunestone(Heroes plugin) {
        super(plugin, "GiganticRunestone");
        setDescription("You imbue a redstone block with an Gigantic Runestone. Gigantic Runestones $1");
        setUsage("/skill giganticrunestone");
        setArgumentRange(0, 0);
        setIdentifiers("skill giganticrunestone");
        setTypes(SkillType.ITEM_MODIFYING, SkillType.SILENCEABLE);

        defaultMaxUses = 15;
        defaultDelay = 8000;
        displayName = "Gigantic Runestone";
        displayNameColor = ChatColor.YELLOW;

        new RunestoneListener();
    }
}