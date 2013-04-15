package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
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
        node.set(SkillSetting.CHANCE_LEVEL.node(), .001);
        return node;
    }

    public class SkillListener implements Listener {

        SkillListener(Skill skill) {
        }
        
        // If right-clicking on an animal and the player does not have a pair of shears, a bucket, a bowl or any form of dye
        // in hand, then check if they're trying to tame. If not, check if they have the breeding skill. If not, then
        // cancel this event and send them a purty message.
    	@EventHandler
    	public void onPlayerVillagerBuy(PlayerInteractEntityEvent event) {
            Player player = event.getPlayer();
            Hero hero = plugin.getCharacterManager().getHero(player);
            Material material = player.getItemInHand().getType();

    		if(event.getRightClicked() instanceof Animals && !material.equals(Material.SHEARS) && !material.equals(Material.BUCKET)
                    && !material.equals(Material.INK_SACK) && !material.equals(Material.BOWL)) {
                if (isWolfTamingAttempt(event) && hero.canUseSkill("Wolf")) {
                    event.setCancelled(false);
                } else if (hero.canUseSkill("Breed")) {
                    event.setCancelled(false);
                } else {
                    player.sendMessage(ChatColor.DARK_GRAY + "You must be a farmer to do that!");
                    event.setCancelled(true);
                }
            }
    	}
    }

    private boolean isWolfTamingAttempt(PlayerInteractEntityEvent event) {
        boolean isWolfTamingAttempt = false;

        if (event.getRightClicked() instanceof Wolf) {
            isWolfTamingAttempt = event.getPlayer().getItemInHand().getType() == Material.BONE;
        }

        return isWolfTamingAttempt;
    }

    @Override
    public String getDescription(Hero hero) {
        double chance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.CHANCE_LEVEL, .001, false);
        int level = hero.getSkillLevel(this);
        if (level < 1)
            level = 1;
        return getDescription().replace("$1", Util.stringDouble(chance * level * 100));
    }
}
