package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityListener;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.effects.EffectType;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.PassiveSkill;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;

public class SkillTumble extends PassiveSkill {

    public SkillTumble(Heroes plugin) {
        super(plugin, "Tumble");
        setDescription("You are able to fall $1 blocks without taking damage.");
        setEffectTypes(EffectType.BENEFICIAL, EffectType.PHYSICAL);
        setTypes(SkillType.PHYSICAL, SkillType.BUFF);
        
        registerEvent(Type.ENTITY_DAMAGE, new SkillEntityListener(this), Priority.Low);
    }
    
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("distance-per-level", .5);
        node.set("base-distance", 3);
        return node;
    }
    
    public class SkillEntityListener extends EntityListener {

    	private Skill skill;
    	
    	SkillEntityListener(Skill skill) {
    		this.skill = skill;
    	}
    	
        @Override
        public void onEntityDamage(EntityDamageEvent event) {
            Heroes.debug.startTask("HeroesSkillListener");
            if (!(event.getEntity() instanceof Player) || event.getCause() != DamageCause.FALL) {
                Heroes.debug.stopTask("HeroesSkillListener");
                return;
            }
            Hero hero = plugin.getHeroManager().getHero((Player) event.getEntity());
            if (!hero.hasEffect("Tumble")) {
                Heroes.debug.stopTask("HeroesSkillListener");
                return;
            }
            int distance = (int) (SkillConfigManager.getUseSetting(hero, skill, "base-distance", 3, false) + (hero.getSkillLevel(skill) * SkillConfigManager.getUseSetting(hero, skill, "distance-per-level", .5, false)));
            int fallDistance = (event.getDamage() - 3) * 3;
            fallDistance -= distance;
            if (fallDistance <= 0)
                event.setCancelled(true);
            else 
                event.setDamage(3 + (fallDistance / 3));
            
            Heroes.debug.stopTask("HeroesSkillListener");
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
