package com.herocraftonline.heroes.characters.skill.skills.codari;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.sk89q.worldguard.protection.events.DisallowedPVPEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SkillSoulEnlightened extends PassiveSkill {

    public SkillSoulEnlightened(Heroes plugin) {
        super(plugin, "SoulEnlightened");
        setDescription("Your soul has been enlightened!");
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
            if (defender.canUseSkill(SkillSoulEnlightened.this)) {
                event.setCancelled(true);
            }
        }
    }
}
