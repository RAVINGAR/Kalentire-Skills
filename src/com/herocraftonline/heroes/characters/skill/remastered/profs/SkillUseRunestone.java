package com.herocraftonline.heroes.characters.skill.remastered.profs;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class SkillUseRunestone extends ActiveSkill {
    public SkillUseRunestone(Heroes plugin) {
        super(plugin, "UseRunestone");
        setDescription("Utilizes the currently held runestone.");
        setArgumentRange(0, 0);
        setUsage("/skill userunestone");
        setIdentifiers("skill userunestone");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE);
//        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DURATION.node(), 20000);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        // RUNESTONE FUNCTIONALITY
        ItemStack heldItem = NMSHandler.getInterface().getItemInMainHand(player.getInventory());
        if (heldItem.getType() != Material.REDSTONE_BLOCK) {
            player.sendMessage("Invalid Runestone.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        // We have a possible Runestone object.

        // Read the meta data (if it has any)
        ItemMeta metaData = heldItem.getItemMeta();
        List<String> loreData = metaData.getLore();
        String displayName = stripColor(metaData.getDisplayName());

        // Ensure that we actually have meta data and at least three rows of available on the block
        if (loreData == null || loreData.isEmpty()) {
            player.sendMessage("Not a Valid Runestone Object. Uses Value is not Valid.");
            return SkillResult.FAIL;
        }

        // Quick check to see if this is a teleportation runestone. We're not interested in allowing them for this skill?
        if (displayName.toLowerCase().endsWith("runestone")) {
            player.sendMessage("Teleportation Runestones aren't supported for this skill, use /skill recall instead.");
            return SkillResult.FAIL;
        }

        // Get the uses on the Runestone and ensure it is greater than 1
        String usesString = loreData.get(1);
        usesString = usesString.toLowerCase();

        // Strip the usesString of the "Uses:" text.
        int currentIndexLocation = usesString.indexOf(":", 0) + 2;  // Set the start point
        int endIndexLocation = usesString.length();                 // Get the end point for grabbing remaining uses data
        String currentUsesString = usesString.substring(currentIndexLocation, endIndexLocation);

        // Strip the currentUsesString of the "max uses" text.
        currentIndexLocation = 0;
        endIndexLocation = currentUsesString.indexOf("/", 0);
        if (endIndexLocation != -1) {
            // There is a possible maximum uses located within the usesString
            currentUsesString = currentUsesString.substring(currentIndexLocation, endIndexLocation);
        }

        if (!isValidUses(currentUsesString, player)) {
            player.sendMessage("Runestone Contains Invalid Data.");
            return SkillResult.FAIL;
        }

        // We have a valid value for "uses". It is either a number, or "unlimited"

        int uses = -1;
        if (!currentUsesString.equals("unlimited"))
            uses = Integer.parseInt(currentUsesString);    // Grab the uses from the string

        // If it's empty, tell them to recharge it.
        if (uses == 0) {
            player.sendMessage("Runestone is out of uses and needs to be recharged.");
            return SkillResult.FAIL;
        }

        int maxUses = -1;
        if (uses != -1) {
            // We will need to set the max uses later, validate and store the value now.
            currentIndexLocation = usesString.indexOf("/", 0);
            if (currentIndexLocation != 0) {

                // There is a possible maximum uses located within the usesString
                currentIndexLocation++;
                endIndexLocation = usesString.length();
                String maxUsesString = usesString.substring(currentIndexLocation, endIndexLocation);

                if (isValidUses(maxUsesString, player)) {
                    if (!maxUsesString.equals("unlimited"))
                        maxUses = Integer.parseInt(maxUsesString);    // Grab the uses from the string
                }
            }
        }

        // We have at least 1 use available--start grabbing location data from the Runestone.

        //TODO our specific new runestone code
        String locationString = loreData.get(0);


        // Remove 1 use from Runestone, but only if the runestone isn't unlimited.
        if (uses != -1 && maxUses != -1) {
            loreData.set(1, ChatColor.AQUA + "Uses: " + (uses - 1) + "/" + maxUses);
            metaData.setLore(loreData);
            heldItem.setItemMeta(metaData);
        }

        return SkillResult.NORMAL;
    }

    private boolean isValidUses(String uses, Player player) {
        if (uses.equals("unlimited"))
            return true;

        return Util.toInt(uses) != null;
    }

    private static String stripColor(final String s) {
        // Trim the color string if it starts with one. Color strings are 2 chars, the color char and the alphanumeric value
        if (s.startsWith(String.valueOf(ChatColor.COLOR_CHAR))) {
            return s.substring(2);
        }
        return s;
    }

//    public class SkillHeroListener implements Listener {
//        private Skill skill;
//
//        public SkillHeroListener(Skill skill) {
//            this.skill = skill;
//        }
//    }
}
