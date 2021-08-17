package com.herocraftonline.heroes.characters.skill.remastered.profs;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Created By MysticMight August 2021 based on SkillRecall runestone mechanics.
 */

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
        config.set("allowed-wardstone", true);
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
        boolean used = useRuneStone(hero, displayName, loreData);
        if (!used)
            return SkillResult.INVALID_TARGET_NO_MSG;

        // Remove 1 use from Runestone, but only if the runestone isn't unlimited.
        if (uses != -1 && maxUses != -1) {
            loreData.set(1, ChatColor.AQUA + "Uses: " + (uses - 1) + "/" + maxUses);
            metaData.setLore(loreData);
            heldItem.setItemMeta(metaData);
        }

        return SkillResult.NORMAL;
    }

    /**
     * Attempts to uses the runestone.
     * @param hero the hero using the warstone
     * @param runestoneDisplayName the name of the runestone item (with colour codes removed)
     * @param loreData the lore data lines of the runestone item
     * @return if the runestone was used (hence costs should be considered)
     */
    private boolean useRuneStone(Hero hero, String runestoneDisplayName, List<String> loreData) {
        if (runestoneDisplayName.equals("WardStone")) {
            return useWardStone(hero, loreData);
        }
        // Add extra runestone logic here...
        return false; // runestone not used
    }

    /**
     * Attempts to use the WardStone.
     * @param hero the hero using the warstone
     * @param loreData the lore data lines of the runestone item
     * @return if the runestone was used (hence costs should be considered)
     */
    private boolean useWardStone(Hero hero, List<String> loreData) {
        //TODO A runestone used during war that on use, puts up a shield in the immediate area to prevent explosions for a short-ish duration, perhaps 15 minutes
        final Player player = hero.getPlayer();
        boolean allowedWardStone = SkillConfigManager.getUseSetting(hero, this, "allowed-wardstone", true);
        if (!allowedWardStone) {
            player.sendMessage(ChatColor.RED + "You are not allowed to use WardStones!");
            return false; // runestone not used
        }

        SkillWardStone wardStoneSkill = (SkillWardStone) plugin.getSkillManager().getSkill("WardStone");
        if (wardStoneSkill == null) {
            player.sendMessage(ChatColor.RED + "There is no valid WardStone skill, contact a admin/dev!");
            return false; // runestone not used
        }
        return wardStoneSkill.tryUseItem(hero, loreData);
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
}
