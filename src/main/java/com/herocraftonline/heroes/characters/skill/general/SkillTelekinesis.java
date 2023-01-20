package com.herocraftonline.heroes.characters.skill.general;

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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.material.Button;
import org.bukkit.material.Lever;

import java.util.HashSet;
import java.util.Objects;

public class SkillTelekinesis extends ActiveSkill {

    private boolean lwcEnabled = false;

    public SkillTelekinesis(Heroes plugin) {
        super(plugin, "Telekinesis");
        this.setDescription("You can activate levers, buttons and other interactable objects from afar.");
        this.setUsage("/skill telekinesis");
        this.setArgumentRange(0, 0);
        this.setIdentifiers("skill telekinesis");
        this.setTypes(SkillType.FORCE, SkillType.SILENCEABLE);

        if (Bukkit.getServer().getPluginManager().getPlugin("LWC") != null){
            lwcEnabled = true;
        }
    }

    @Override
    public String getDescription(Hero hero) {
        return this.getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.MAX_DISTANCE.node(), 15);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();
        final HashSet<Material> transparent = new HashSet<>();
        transparent.add(Material.AIR);
        transparent.add(Material.WATER);
        transparent.add(Material.REDSTONE_TORCH);
        transparent.add(Material.REDSTONE_WALL_TORCH);
        transparent.add(Material.REDSTONE_WIRE);
        transparent.add(Material.TORCH);
        transparent.add(Material.SNOW);
        final Block block = player.getTargetBlock(transparent, SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 15, false));

        BlockState state = block.getState();
        switch (block.getType()) {
            case STONE_BUTTON:
            case OAK_BUTTON:
            case BIRCH_BUTTON:
            case ACACIA_BUTTON:
            case DARK_OAK_BUTTON:
            case JUNGLE_BUTTON:
            case SPRUCE_BUTTON: {
                if (playerCannotAccessBlockLWCProtection(player, block)) return SkillResult.INVALID_TARGET_NO_MSG;
                Button button = (Button) state.getData();
                button.setPowered(true);
                state.setData(button); // not sure if this is required...

                // Schedule button turn off (rebounce)
                long turnOffDelayTicks = block.getType() == Material.STONE_BUTTON ? 10L : 15L;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    BlockState state1 = block.getState();
                    Button button1 = (Button) state1.getData();
                    button1.setPowered(false);
                    state1.setData(button1);
                    state1.update();
                }, turnOffDelayTicks);
                break;
            }
            case LEVER: {
                if (playerCannotAccessBlockLWCProtection(player, block)) return SkillResult.INVALID_TARGET_NO_MSG;
                Lever lever = (Lever) block.getState().getData();
                lever.setPowered(!lever.isPowered());
                state.setData(lever); // not sure if this is required...
                break;
            }
            default: {
                player.sendMessage(ChatColor.GRAY + "You must target a lever or button!");
                return SkillResult.INVALID_TARGET_NO_MSG;
            }
        }

        //Update block state -
        state.update();

        // Old code that doesn't appear to be required anymore and causes the button/lever to pop into a item.
        // Left for reference, in case its needed again.
//        BlockFace face = BlockFace.SELF;
//        //Update attached state - workaround for Bukkit-1858
//        MaterialData matData = state.getData();
//        if (matData instanceof Attachable) {
//            face = ((Attachable) matData).getAttachedFace();
//        }
//        Block attached = block.getRelative(face);
//        BlockState attachedState = attached.getState();
//        // debug message
//        player.sendMessage("attached block: " + attached.getType()
//                + " (" + attached.getLocation().toString() + ")");
//        attached.setTypeId(0, true);
//        attachedState.update(true);
//        // debug message
//        player.sendMessage("After: Updated " + block.getType() + " state and attached block: " + attached.getType()
//                + " (" + attached.getLocation().toString() + ")");

        this.broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    private boolean playerCannotAccessBlockLWCProtection(Player player, Block block) {
        if (lwcEnabled) {
            LWC lwc = ((LWCPlugin) Objects.requireNonNull(plugin.getServer().getPluginManager().getPlugin("LWC"))).getLWC();
            Protection protection = lwc.findProtection(block);
            if (protection != null) {
                // block is LWC protected
                if (!lwc.canAccessProtection(player, block)) {
                    player.sendMessage(ChatColor.GRAY + "You cannot telekinetically interact with that object, it is protected by magic!");
                    return true;
                }
            }
        }
        return false; // can access
    }

}
