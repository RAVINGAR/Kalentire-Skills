package com.herocraftonline.heroes.characters.skill.general;

import org.bukkit.event.Listener;

import com.herocraftonline.heroes.Heroes;

public class SkillRecallShop extends SkillRecall implements Listener {

    public SkillRecallShop(Heroes plugin) {
        super(plugin, "RecallShop");
        setDescription("You recall to your marked shop location.");
        setUsage("/skill recallshop");
        setIdentifiers("skill recallshop");

        subChannel = "RecallShopRequest";
    }
}
