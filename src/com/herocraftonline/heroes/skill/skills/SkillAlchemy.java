package com.herocraftonline.heroes.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.effects.EffectType;
import com.herocraftonline.heroes.hero.Hero;
import com.herocraftonline.heroes.skill.PassiveSkill;
import com.herocraftonline.heroes.skill.Skill;
import com.herocraftonline.heroes.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;

public class SkillAlchemy extends PassiveSkill {

    public SkillAlchemy(Heroes plugin) {
        super(plugin, "Alchemy");
        setDescription("You are able to craft potions!");
        setArgumentRange(0, 0);
        setTypes(SkillType.KNOWLEDGE, SkillType.ITEM);
        setEffectTypes(EffectType.BENEFICIAL);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillListener(this), plugin);
    }
    
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection section = super.getDefaultConfig();
        section.set(Setting.LEVEL.node(), 1);
        return section;
    }
    
    public class SkillListener implements Listener {
        
        private final Skill skill;
        public SkillListener(Skill skill) {
            this.skill = skill;
        }
        
        @EventHandler(priority = EventPriority.LOW)
        public void onPlayerInteract(PlayerInteractEvent event) {
            if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.BREWING_STAND) {
                return;
            }
            Hero hero = plugin.getHeroManager().getHero(event.getPlayer());
            if (!hero.canUseSkill(skill)) {
                event.setCancelled(true);
                event.setUseInteractedBlock(Result.DENY);
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
}
