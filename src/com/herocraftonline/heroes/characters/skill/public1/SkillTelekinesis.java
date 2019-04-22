package com.herocraftonline.heroes.characters.skill.public1;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.material.Attachable;
import org.bukkit.material.Button;
import org.bukkit.material.Lever;
import org.bukkit.material.MaterialData;

import java.util.HashSet;

public class SkillTelekinesis extends ActiveSkill {

    public SkillTelekinesis(Heroes plugin) {
        super(plugin, "Telekinesis");
        this.setDescription("You can activate levers, buttons and other interactable objects from afar.");
        this.setUsage("/skill telekinesis");
        this.setArgumentRange(0, 0);
        this.setIdentifiers("skill telekinesis");
        this.setTypes(SkillType.FORCE, SkillType.SILENCEABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.MAX_DISTANCE.node(), 15);
        return node;
    }

    @SuppressWarnings("deprecation")
    @Override
    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();
        final HashSet<Material> transparent = new HashSet<Material>();
        transparent.add(Material.AIR);
        transparent.add(Material.WATER);
        transparent.add(Material.REDSTONE_TORCH);
        transparent.add(Material.REDSTONE_WALL_TORCH);
        transparent.add(Material.REDSTONE_WIRE);
        transparent.add(Material.TORCH);
        transparent.add(Material.SNOW);
        final Block block = player.getTargetBlock(transparent, SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 15, false));

        switch (block.getType()) {
            case STONE_BUTTON:
            case OAK_BUTTON:
            case BIRCH_BUTTON:
            case ACACIA_BUTTON:
            case DARK_OAK_BUTTON:
            case JUNGLE_BUTTON:
            case SPRUCE_BUTTON: {
                Button button = (Button) block;
                button.setPowered(true);
                break;
            }
            case LEVER: {
                Lever lever = (Lever) block;
                lever.setPowered(!lever.isPowered());
                break;
            }
            default: {
                player.sendMessage(ChatColor.GRAY + "You must target a lever or button!");
                return SkillResult.INVALID_TARGET_NO_MSG;
            }
        }

        //Update block state -
        BlockFace face = BlockFace.SELF;
        BlockState state = block.getState();
        state.update();
        //Update attached state - workaround for Bukkit-1858
        MaterialData matData = state.getData();
        if (matData instanceof Attachable) {
            face = ((Attachable) matData).getAttachedFace();
        }
        Block attached = block.getRelative(face);
        BlockState attachedState = attached.getState();
        //FIXME Can't do this anymore
        //attached.setTypeId(0, true);
        attachedState.update(true);

        this.broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        return this.getDescription();
    }

}
