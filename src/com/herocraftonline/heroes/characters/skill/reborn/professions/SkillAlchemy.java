package com.herocraftonline.heroes.characters.skill.reborn.professions;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Hopper;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

import java.util.logging.Level;

public class SkillAlchemy extends PassiveSkill {

    public SkillAlchemy(Heroes plugin) {
        super(plugin, "Alchemy");
        setDescription("You are able to craft potions!");
        setArgumentRange(0, 0);
        setTypes(SkillType.ITEM_CREATION);
        setEffectTypes(EffectType.BENEFICIAL);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.APPLY_TEXT.node(), "");
        config.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        config.set(SkillSetting.LEVEL.node(), 1);
        return config;
    }
    
    public class SkillListener implements Listener {
        
        private final Skill skill;
        public SkillListener(Skill skill) {
            this.skill = skill;
        }
        
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onPlayerInteract(PlayerInteractEvent event) {
            if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.BREWING_STAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
                return;
            }
            Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
            if (!hero.canUseSkill(skill)) {
                event.setCancelled(true);
                event.setUseInteractedBlock(Result.DENY);
            }
        }

        // Prevent all alchemy via hoppers.
        @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
        public void onInventoryPickup(InventoryMoveItemEvent event) {
            if (event.getSource() == null || event.getSource().getHolder() == null || event.getDestination() == null || event.getDestination().getHolder() == null)
                return;
            if (!(event.getSource().getHolder() instanceof BrewingStand))
                return;

            if (event.getDestination().getHolder() instanceof Hopper) {
                Hopper hopper = (Hopper) event.getDestination().getHolder();
                Block block = hopper.getBlock();
                Chunk chunk = block.getChunk();
                if (!chunk.isLoaded())
                    chunk.load();

                if (!block.breakNaturally())
                    Heroes.log(Level.INFO, "FAILED TO BREAK HOPPER");
                else
                    Heroes.log(Level.INFO, "Succeeded at breaking hopper");

                event.setCancelled(true);
            } else if (event.getDestination().getHolder() instanceof HopperMinecart) {
                HopperMinecart hopperCart = (HopperMinecart) event.getDestination().getHolder();
                Chunk chunk = hopperCart.getLocation().getBlock().getChunk();
                if (!chunk.isLoaded())
                    chunk.load();

                hopperCart.setEnabled(false);
                hopperCart.eject();
                hopperCart.setDamage(9999999);

                event.setCancelled(true);
            }
        }
    }
}
