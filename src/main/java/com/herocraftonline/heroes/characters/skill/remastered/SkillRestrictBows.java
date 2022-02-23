package com.herocraftonline.heroes.characters.skill.remastered;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.equipment.EquipMethod;
import com.herocraftonline.heroes.characters.equipment.EquipmentChangedEvent;
import com.herocraftonline.heroes.characters.equipment.EquipmentType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * This passive skill is just a temporary fix, possibly with problems. Just to be used in meantime while the issue is actually fixed.
 *
 * The issue for reference:
 * "A way to use a bow for unintended classes, currently only tested for ShadowKnight Locate a bow in either a chest
 * or actual inventory , Hover over bow and press F (or whatever keybind to place it in off-hand,
 * Bow is now in off-hand and actually useable"
 */
public class SkillRestrictBows extends PassiveSkill {

    public SkillRestrictBows(Heroes plugin) {
        super(plugin, "RestrictBows");
        setDescription("");
        Bukkit.getServer().getPluginManager().registerEvents(new SkillInventoryListener(this), plugin);
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
        return config;
    }

    public class SkillInventoryListener implements Listener {
        private final PassiveSkill skill;

        public SkillInventoryListener(PassiveSkill skill) {
            this.skill = skill;
        }

        //HIGHEST is above Heroes's event
        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onInventoryClick(final InventoryClickEvent event) {
            final InventoryAction action = event.getAction();
            if (action == InventoryAction.NOTHING || !(event.getWhoClicked() instanceof Player))
                return;

            // These are the only actions we're interested in, to fix the issue.
            if (action != InventoryAction.HOTBAR_SWAP && action != InventoryAction.HOTBAR_MOVE_AND_READD)
                return;

            // Ignore all other items but BOWs and CROSSBOWs, as these are the ones we're having issues with.
            final ItemStack currentItem = event.getCurrentItem(); // Item hovered over or clicked
            if (currentItem == null)
                return;
            if (currentItem.getType() != Material.BOW && currentItem.getType() != Material.CROSSBOW)
                return;

            EquipmentType newEquipmentType = EquipmentType.ensureIsTypeOrNull(currentItem, EquipmentType.OFFHAND);
            if (newEquipmentType == null)
                return; // Should pass, since we know its only bows, but might as well

            // Call event to check player meets class requirements, etc.
            // Note that we are ignoring if there is an item in the offhand already, as we're only interested in
            // preventing equipping bows (and will just stop unequipping this way as well).
            Player player = (Player) event.getWhoClicked();
            EquipmentChangedEvent equipmentChangedEvent = new EquipmentChangedEvent(player,
                    EquipMethod.OFFHAND_SWAP, newEquipmentType,
                    null,
                    event.getCurrentItem());

            Bukkit.getServer().getPluginManager().callEvent(equipmentChangedEvent);
            if (equipmentChangedEvent.isCancelled())
                event.setCancelled(true);
        }
    }
}
