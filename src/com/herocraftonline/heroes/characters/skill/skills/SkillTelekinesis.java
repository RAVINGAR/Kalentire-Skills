package com.herocraftonline.heroes.characters.skill.skills;

import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;
import com.griefcraft.model.Protection;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;
import net.minecraft.server.v1_7_R4.EntityHuman;
import net.minecraft.server.v1_7_R4.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Jukebox;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.BlockIterator;

import java.util.ArrayList;
import java.util.List;

public class SkillTelekinesis extends ActiveSkill {

    private boolean lwcEnabled = false;
    private boolean ncpEnabled = false;

    public SkillTelekinesis(Heroes plugin) {
        super(plugin, "Telekinesis");
        setDescription("You can activate levers, buttons and other interactable tables from afar.");
        setUsage("/skill telekinesis");
        setArgumentRange(0, 0);
        setIdentifiers("skill telekinesis");
        setTypes(SkillType.FORCE, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE);

        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null)
            ncpEnabled = true;

        if (Bukkit.getServer().getPluginManager().getPlugin("LWC") != null)
            lwcEnabled = true;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 15);

        return node;
    }

    @SuppressWarnings("deprecation")
    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int maxDist = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 15, false);
        Block targetBlock = player.getTargetBlock(null, maxDist);
        if (targetBlock.getType() == Material.AIR) {
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        BlockIterator iter;
        try {
            iter = new BlockIterator(player, maxDist);
        }
        catch (IllegalStateException e) {
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        List<String> allowedBlocks = SkillConfigManager.getUseSetting(hero, this, "allowed-blocks", new ArrayList<String>());

        // Make sure the player's target is actually "visible" by cycling through each block between him and his target.
        Block tempBlock;
        while (iter.hasNext()) {
            tempBlock = iter.next();

            // Some "transparent" blocks are actually what we are looking for, so check those first.
            if (allowedBlocks.contains(tempBlock.getType().toString())) {
                targetBlock = tempBlock;
                break;
            }
            else if (!Util.transparentBlocks.contains(tempBlock.getType())) {
                // if the block is not transparent, it should become the new "target" block.
                targetBlock = tempBlock;
                break;
            }
        }

        Material blockMaterial = targetBlock.getType();

        if (allowedBlocks.contains(blockMaterial.toString())) {
            boolean canInteractWithBlock = true;
            boolean isLWCd = false;
            if (lwcEnabled) {
                LWC lwc = ((LWCPlugin) plugin.getServer().getPluginManager().getPlugin("LWC")).getLWC();
                Protection protection = lwc.findProtection(targetBlock);
                if (protection != null) {
                    if (!lwc.canAccessProtection(player, protection)) {
                        canInteractWithBlock = false;
                    }
                    else
                        isLWCd = true;
                }
            }

            if (canInteractWithBlock) {
                // Let's bypass the nocheat issues...
                if (ncpEnabled) {
                    if (!player.isOp()) {
                        NCPExemptionManager.exemptPermanently(player, CheckType.BLOCKINTERACT);
                    }
                }

                // LWC plugin handles the Iron door stuff themselves, let them do it if we're dealing with an LWC.
                Material blockType = targetBlock.getType();
                if (blockType.equals(Material.IRON_DOOR_BLOCK) && isLWCd) {

                    // Simpler way that doesn't make me want to die. (as much...)
                    Material heldItem = player.getItemInHand().getType();
                    boolean hasBind = false;
                    String[] boundAbility = null;

                    // If they have a bind, remove it temporarily. so that we dont fuck shit up.
                    if (hero.hasBind(heldItem)) {
                        hasBind = true;
                        boundAbility = hero.getBind(heldItem);
                        hero.unbind(heldItem);
                    }

                    // Interact with the block
                    PlayerInteractEvent fakeInteractEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, player.getItemInHand(), targetBlock, BlockFace.UP);
                    plugin.getServer().getPluginManager().callEvent(fakeInteractEvent);

                    // Give their bind back
                    if (hasBind)
                        hero.bind(heldItem, boundAbility);
                }
                else if (blockType.equals(Material.JUKEBOX)) {
                    Jukebox jukeBox = (Jukebox) targetBlock.getState();

                    if (jukeBox.isPlaying()) {
                        jukeBox.eject();
                    }
                    else {
                        Material heldItem = player.getItemInHand().getType();
                        if (isRecord(heldItem)) {

                            // Remove the record
                            PlayerInventory inventory = player.getInventory();
                            player.getInventory().clear(inventory.getHeldItemSlot());
                            player.updateInventory();

                            // Play the disk
                            jukeBox.setPlaying(heldItem);
                        }
                        else
                            Messaging.send(player, "Hmm...nothing seemed to have happend.");
                    }
                }
                else {
                    int blockID = blockMaterial.getId();
                    WorldServer worldServer = ((CraftWorld) targetBlock.getWorld()).getHandle();
                    EntityHuman entityHuman = ((CraftPlayer) player).getHandle();
                    net.minecraft.server.v1_7_R4.Block block = net.minecraft.server.v1_7_R4.Block.b(Integer.toString(blockID));

                    block.interact(worldServer, targetBlock.getX(), targetBlock.getY(), targetBlock.getZ(), entityHuman, 0, 0, 0, 0);

                    // DEAR GOD WHYYYY. I DONT WANNA HAVE TO DO IT THIS WAY.
                    //                    if ((targetBlock.getData() & 0x8) == 0x8)
                    //                        targetBlock = targetBlock.getRelative(BlockFace.DOWN);
                    //
                    //                    // Get the top half of the door
                    //                    Block topHalf = targetBlock.getRelative(BlockFace.UP);
                    //
                    //                    // Now xor both data values with 0x4, the flag that states if the door is open
                    //                    targetBlock.setData((byte) (targetBlock.getData() ^ 0x4));
                    //
                    //                    // Play the door open/close sound
                    //                    targetBlock.getWorld().playEffect(targetBlock.getLocation(), Effect.DOOR_TOGGLE, 0);
                    //
                    //                    // Only change the block above it if it is something we can open or close
                    //                    if (topHalf.getType().equals(Material.IRON_DOOR_BLOCK)) {
                    //                        topHalf.setData((byte) (topHalf.getData() ^ 0x4));
                    //                    }
                }

                // Let's bypass the nocheat issues...
                if (ncpEnabled) {
                    if (!player.isOp()) {
                        NCPExemptionManager.unexempt(player, CheckType.BLOCKINTERACT);
                    }
                }

                return SkillResult.NORMAL;
            }
        }

        Messaging.send(player, "You cannot telekinetically interact with that object!");
        return SkillResult.INVALID_TARGET_NO_MSG;

    }

    private boolean isRecord(Material mat) {
        switch (mat) {
            case RECORD_3:
            case RECORD_4:
            case RECORD_5:
            case RECORD_6:
            case RECORD_7:
            case RECORD_8:
            case RECORD_9:
            case RECORD_10:
            case RECORD_11:
            case RECORD_12:
            case GREEN_RECORD:
            case GOLD_RECORD:
                return true;
            default:
                return false;
        }
    }
}
