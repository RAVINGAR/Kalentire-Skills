package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
        node.set("allow-horse-breeding", false);

        return node;
    }

    public class SkillListener implements Listener {

        private Skill skill;

        SkillListener(Skill skill) {
            this.skill = skill;
        }

        // If right-clicking on an animal and the player does not have a pair of shears, a bucket, a bowl or any form of dye
        // in hand, then check if they're trying to tame. If not, check if they have the breeding skill. If not, then
        // cancel this event and send them a purty message.
        @EventHandler
        public void onPlayerEntityInteract(PlayerInteractEntityEvent event) {
            Player player = event.getPlayer();
            Hero hero = plugin.getCharacterManager().getHero(player);
            Material material = player.getItemInHand().getType();

            Entity targetEntity = event.getRightClicked();

            // Only deal with animals.
            if (!(targetEntity instanceof Animals))
                return;

            // Allow them to do normal item interaction.
            if (material.equals(Material.SHEARS)
                    || material.equals(Material.BUCKET)
                    || material.equals(Material.INK_SACK)
                    || material.equals(Material.BOWL)) {

                event.setCancelled(false);
                return;
            }

            // Handle horse interaction
            if (targetEntity instanceof Horse && !material.equals(Material.WHEAT) && !material.equals(Material.HAY_BLOCK)
                    && !material.equals(Material.APPLE) && !material.equals(Material.SUGAR) && !material.equals(Material.BREAD)
                    && !material.equals(Material.GOLDEN_APPLE) && !material.equals(Material.GOLDEN_CARROT)) {

                // If they are just trying to mount the horse, let them
                event.setCancelled(false);
                return;
            }
            else if (targetEntity instanceof Horse) {
                // They are trying to breed a horse.

                // If they are trying to breed the horse, check to make sure they are allowed to.
                boolean canBreedHorses = SkillConfigManager.getUseSetting(hero, skill, "allow-horse-breeding", false);
                if (!hero.canUseSkill("Breed") || canBreedHorses == false) {
                    event.setCancelled(true);
                    return;     // Return early. We do not wish to display a message for horse breeding right now.
                }
            }

            if (isWolfTamingAttempt(event) && !hero.canUseSkill("Wolf")) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.GRAY + "You are not allowed to tame wolves!");
                return;
            }

            // If we make it this far, they are trying to breed.
            if (!hero.canUseSkill(skill)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.GRAY + "You must be a farmer to do that!");
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
