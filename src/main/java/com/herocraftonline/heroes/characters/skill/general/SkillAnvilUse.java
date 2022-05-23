package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.chat.ChatComponents;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;

public class SkillAnvilUse extends PassiveSkill {

    private String denyMessage;

    public SkillAnvilUse(Heroes plugin) {
        super(plugin, "AnvilUse");
        setDescription("You can use Anvils!");
        Bukkit.getPluginManager().registerEvents(new AnvilListener(this), this.plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("deny-message", ChatComponents.GENERIC_SKILL + ChatColor.GRAY + "You must be a Blacksmith to use an Anvil!");
        return config;
    }

    @Override
    public void init() {
        super.init();
        denyMessage = "    " + SkillConfigManager.getRaw(this, "deny-message",
                ChatComponents.GENERIC_SKILL + ChatColor.GRAY + "You must be a Blacksmith to use an Anvil!");
    }

    public class AnvilListener implements Listener {
        private final PassiveSkill skill;

        public AnvilListener(PassiveSkill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onOpenAnvilInventory(InventoryOpenEvent event) {
            if (event.getInventory().getType() != InventoryType.ANVIL)
                return;

            // Deny opening to those without this skill (and the right level)
            Hero hero = plugin.getCharacterManager().getHero((Player) event.getPlayer());
            if (!skill.hasPassive(hero)) {
                event.setCancelled(true);
                hero.getPlayer().sendMessage(denyMessage);
            }
        }

        /*
         * Checks for right clicks on anvil and cancels if this skill is not possessed
         * Handled as highest priority due to us not wanting people to override us canceling the event
         */
        /* // Old code, may not be necessary anymore due to the open event handling above
        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onPlayerInteract(PlayerInteractEvent event) {
            if(event.getAction() != Action.RIGHT_CLICK_BLOCK) {
                return;
            }
            if(!event.getClickedBlock().getType().equals(Material.ANVIL)) {
                return;
            }

            Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
            if(!hero.hasEffect("Anvil")) {
                event.setCancelled(true);
                hero.getPlayer().sendMessage(ChatColor.GRAY + "You lack the training to use an Anvil!");
            } else {

            }
        }
        */
    }

}
