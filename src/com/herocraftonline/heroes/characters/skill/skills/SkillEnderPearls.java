package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillEnderPearls extends PassiveSkill {

    public SkillEnderPearls(Heroes plugin) {
        super(plugin, "EnderPearls");
        setDescription("You can throw ender pearls!");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_PROJECTILE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("velocity-multiplier", Double.valueOf(0.75));

        return node;
    }

    public class SkillEntityListener implements Listener {

        private Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onProjectileLaunch(ProjectileLaunchEvent event) {
            if (!(event.getEntity() instanceof EnderPearl))
                return; 
            
            if (!(event.getEntity().getShooter() instanceof Player))
                return;
            
            Player player = (Player) event.getEntity().getShooter();
            Hero hero = plugin.getCharacterManager().getHero(player);
            if (!hero.canUseSkill(skill)) {
                event.setCancelled(true);
                return;
            }
            
            double velocityMultiplier = SkillConfigManager.getUseSetting(hero, skill, "velocity-multiplier", Double.valueOf(0.75), false);
            EnderPearl enderPearl = (EnderPearl) event.getEntity();
            enderPearl.setVelocity(enderPearl.getVelocity().multiply(velocityMultiplier));
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onPlayerTeleport(PlayerTeleportEvent event) {
            if (event.getCause() == TeleportCause.ENDER_PEARL) {
                Player player = event.getPlayer();
                Hero hero = plugin.getCharacterManager().getHero(player);
                if (!hero.canUseSkill(skill)) {
                    event.setCancelled(true);
                    return;
                }
                else {
                    Location teleportLoc = event.getTo();
                    Block teleportLocBlock = teleportLoc.getBlock();
                    switch (teleportLocBlock.getType()) {
                        case IRON_DOOR:
                        case IRON_DOOR_BLOCK:
                        case WOODEN_DOOR:
                        case IRON_FENCE:
                        case FENCE:
                        case THIN_GLASS:
                        case GLASS:
                            // Cancel immediately when dealing with exploitable blocks.
                            event.setCancelled(true);
                            break;
                        default:
                            event.setCancelled(false);
                            break;

                    }
                    //                    if (!Util.transparentBlocks.contains(teleportLocBlock)) {
                    //                        // Ender pearl has landed on an invalid block.
                    //                        event.setCancelled(true);
                    //                        return;
                    //                    }
                    //                    else
                    //                        event.setCancelled(false);
                }
            }
        }
    }
}
