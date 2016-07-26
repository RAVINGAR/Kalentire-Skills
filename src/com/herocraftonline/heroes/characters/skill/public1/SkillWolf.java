package com.herocraftonline.heroes.characters.skill.public1;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTameEvent;

public class SkillWolf extends PassiveSkill {

    public SkillWolf(Heroes plugin) {
        super(plugin, "Wolf");
        this.setDescription("You have the ability to tame wolves.");
        this.setUsage("/skill wolf <release|summon>");
        this.setArgumentRange(0, 1);
        this.setIdentifiers("skill wolf");
        this.setTypes(SkillType.SUMMONING, SkillType.KNOWLEDGE);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }


    public class SkillEntityListener implements Listener {

        private final SkillWolf skill;

        SkillEntityListener(SkillWolf skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onEntityTame(EntityTameEvent event) {
            final AnimalTamer owner = event.getOwner();
            final Entity animal = event.getEntity();
            if (event.isCancelled() || !(animal instanceof Wolf) || !(owner instanceof Player)) {
                return;
            }

            final Player player = (Player) owner;
            final Hero hero = SkillWolf.this.plugin.getCharacterManager().getHero(player);
            if (!hero.canUseSkill(this.skill.getName())) {
                player.sendMessage(ChatColor.GRAY + "You can't tame wolves!");
                event.setCancelled(true);
            }
        }
    }


    @Override
    public String getDescription(Hero hero) {
        return this.getDescription();
    }
}
