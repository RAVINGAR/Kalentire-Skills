package com.herocraftonline.heroes.characters.skill.unusedskills;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Messaging;

public class SkillWolf extends PassiveSkill {

    public SkillWolf(Heroes plugin) {
        super(plugin, "Wolf");
        setDescription("You have the ability to tame wolves.");
        setUsage("/skill wolf <release|summon>");
        setArgumentRange(0, 1);
        setIdentifiers("skill wolf");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    public class SkillEntityListener implements Listener {

        private final SkillWolf skill;

        SkillEntityListener(SkillWolf skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onPlayerEntityInteract(PlayerInteractEntityEvent event) {
            Player player = event.getPlayer();
            Hero hero = plugin.getCharacterManager().getHero(player);
            Material material = NMSHandler.getInterface().getItemInMainHand(player.getInventory()).getType();

            Entity targetEntity = event.getRightClicked();

            if (targetEntity instanceof Wolf) {
                if (material == Material.BONE) {
                    if (!hero.canUseSkill(skill.getName()))
                        event.setCancelled(true);
                }
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onEntityTame(EntityTameEvent event) {
            AnimalTamer owner = event.getOwner();
            Entity animal = event.getEntity();
            if (event.isCancelled() || !(animal instanceof Wolf) || !(owner instanceof Player)) {
                return;
            }

            Player player = (Player) owner;
            Hero hero = plugin.getCharacterManager().getHero(player);
            if (!hero.canUseSkill(skill.getName())) {
                Messaging.send(player, "You can't tame wolves!");
                event.setCancelled(true);
            }
        }
    }
}
