package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.herocraftonline.heroes.Heroes;

public class SkillRecallShop extends SkillRecall implements Listener, PluginMessageListener {

    public SkillRecallShop(Heroes plugin) {
        super(plugin, "RecallShop");
        setDescription("You recall to your marked shop location.");
        setUsage("/skill recallshop");
        setIdentifiers("skill recallshop");

        subChannel = "RecallShopRequest";
    }
}
