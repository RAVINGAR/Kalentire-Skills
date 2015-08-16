package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.sk89q.worldguard.protection.events.DisallowedPVPEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SkillAttackOnSight extends PassiveSkill {

    public SkillAttackOnSight(Heroes plugin) {
        super(plugin, "AttackOnSight");
        setDescription("Allows the hero to be attacked anywhere.");
        setArgumentRange(0, 0);
        Bukkit.getServer().getPluginManager().registerEvents(new AttackOnSightListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    public class AttackOnSightListener implements Listener {

        @EventHandler()
        public void onDisallowedPVP(DisallowedPVPEvent event) {
            Hero defender = plugin.getCharacterManager().getHero(event.getDefender());
            if (defender.canUseSkill(SkillAttackOnSight.this)) {
                event.setCancelled(true);
            }
        }
    }
}