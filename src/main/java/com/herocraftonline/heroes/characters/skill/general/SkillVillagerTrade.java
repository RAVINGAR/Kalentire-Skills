package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class SkillVillagerTrade extends PassiveSkill {

    public SkillVillagerTrade(Heroes plugin) {
        super(plugin, "VillagerTrade");
        setDescription("You have gained the ability to communicate with Villagers.");

        Bukkit.getPluginManager().registerEvents(new SkillListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double chance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.CHANCE_PER_LEVEL, .001, false);
        int level = hero.getHeroLevel(this);
        if (level < 1)
            level = 1;
        return getDescription().replace("$1", Util.stringDouble(chance * level * 100));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.APPLY_TEXT.node(), "");
        node.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        node.set(SkillSetting.CHANCE_PER_LEVEL.node(), .001);

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
            if (event.getRightClicked().getType() == EntityType.VILLAGER) {
                if (hero.canUseSkill("VillagerTrade")) {
                    event.setCancelled(false);
                } else {
                    player.sendMessage(ChatColor.DARK_GRAY + "The villager cannot understand you!");
                    event.setCancelled(true);
                }
            }
        }
    }
}
