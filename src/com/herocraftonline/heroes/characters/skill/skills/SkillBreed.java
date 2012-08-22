package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;
import com.herocraftonline.heroes.util.Util;

public class SkillBreed extends PassiveSkill {

    public SkillBreed(Heroes plugin) {
        super(plugin, "Breed");
        setDescription("You have gained the ability to breed animals.");
        setEffectTypes(EffectType.BENEFICIAL);
        setTypes(SkillType.KNOWLEDGE, SkillType.BUFF);
        Bukkit.getPluginManager().registerEvents(new SkillListener(this), plugin);
    }
    
   
    
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.CHANCE_LEVEL.node(), .001);
        return node;
    }

    public class SkillListener implements Listener {

        SkillListener(Skill skill) {
        }
        
        //TODO: Add all the listener shit in here
        //TODO: Change the logic on this to deny/allow based on the player having this skill instead of permission:
    	@EventHandler
    	public void onPlayerVillagerBuy(PlayerInteractEntityEvent event) {
            Player player = event.getPlayer();
            Hero hero = plugin.getCharacterManager().getHero(player);
    		if(event.getRightClicked().getType() == EntityType.COW) {
    			if (hero.canUseSkill("Breed")) {
    				event.setCancelled(false);
    			} else {
    				player.sendMessage(ChatColor.DARK_GRAY + "You are unable to breed animals!");
    				event.setCancelled(true);
    			}
    		}
    	}
    }

    @Override
    public String getDescription(Hero hero) {
        double chance = SkillConfigManager.getUseSetting(hero, this, Setting.CHANCE_LEVEL, .001, false);
        int level = hero.getSkillLevel(this);
        if (level < 1)
            level = 1;
        return getDescription().replace("$1", Util.stringDouble(chance * level * 100));
    }
}
