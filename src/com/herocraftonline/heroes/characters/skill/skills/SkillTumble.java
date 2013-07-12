package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillTumble extends PassiveSkill {

    public SkillTumble(Heroes plugin) {
        super(plugin, "Tumble");
        setDescription("You are able to fall $1 blocks without taking damage.");
        setEffectTypes(EffectType.BENEFICIAL, EffectType.PHYSICAL);
        setTypes(SkillType.PHYSICAL, SkillType.BUFF);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }
    
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("distance-per-level", .5);
        node.set("base-distance", 3);
        return node;
    }
    
    public class SkillEntityListener implements Listener {

    	private Skill skill;
    	
    	SkillEntityListener(Skill skill) {
    		this.skill = skill;
    	}
    	
    	@EventHandler(priority = EventPriority.LOW)
        public void onEntityDamage(EntityDamageEvent event) {
            if (!(event.getEntity() instanceof Player) || event.getCause() != DamageCause.FALL) {
                return;
            }
            Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (!hero.hasEffect("Tumble")) {
                return;
            }
            int distance = (int) (SkillConfigManager.getUseSetting(hero, skill, "base-distance", 3, false) + (hero.getSkillLevel(skill) * SkillConfigManager.getUseSetting(hero, skill, "distance-per-level", .5, false)));
            double fallDistance = (event.getDamage() - 3) * 3;
            fallDistance -= distance;
            if (fallDistance <= 0) {
                event.setCancelled(true);
            } else {
                event.setDamage(3 + (fallDistance / 3));
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int dist = SkillConfigManager.getUseSetting(hero, this, "base-distance", 3, false);
        double distlev = SkillConfigManager.getUseSetting(hero, this, "distance-per-level", .5, false);
        int level = hero.getSkillLevel(this);
        if (level < 0)
            level = 0;
        dist += (int) (distlev * level);
        return getDescription().replace("$1", dist + "");
    }
}
