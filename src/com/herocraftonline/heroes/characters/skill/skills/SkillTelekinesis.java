package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_6_R2.CraftWorld;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;

public class SkillTelekinesis extends ActiveSkill {

    public SkillTelekinesis(Heroes plugin) {
        super(plugin, "Telekinesis");
        setDescription("You can activate levers, buttons and other interactable objects from afar.");
        setUsage("/skill telekinesis");
        setArgumentRange(0, 0);
        setIdentifiers("skill telekinesis");
        setTypes(SkillType.FORCE, SkillType.KNOWLEDGE, SkillType.SILENCABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.MAX_DISTANCE.node(), 15);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int maxDist = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 15, false);
        Block block = player.getTargetBlock(null, maxDist);
        if (block.getType() == Material.AIR) {
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        Material blockMaterial = block.getType();
        if (blockMaterial == Material.LEVER
                || blockMaterial == Material.STONE_BUTTON
                || blockMaterial == Material.WOOD_BUTTON
                || blockMaterial == Material.IRON_DOOR
                || blockMaterial == Material.IRON_DOOR_BLOCK
                || blockMaterial == Material.WOODEN_DOOR
                || blockMaterial == Material.DIODE
                || blockMaterial == Material.DIODE_BLOCK_ON
                || blockMaterial == Material.DIODE_BLOCK_OFF
                || blockMaterial == Material.REDSTONE_COMPARATOR
                || blockMaterial == Material.REDSTONE_COMPARATOR_OFF
                || blockMaterial == Material.REDSTONE_COMPARATOR_ON
                || blockMaterial == Material.JUKEBOX
                || blockMaterial == Material.NOTE_BLOCK
                || blockMaterial == Material.TRAP_DOOR) {

            net.minecraft.server.v1_6_R2.Block.byId[blockMaterial.getId()].interact(((CraftWorld) block.getWorld()).getHandle(), block.getX(),
                                                                                    block.getY(), block.getZ(), null, 0, 0, 0, 0);

            // Old stuff. No longer needed
            // Lever lever = (Lever) block.getState().getData();
            // lever.setPowered(!lever.isPowered());
            // block.getState().update();

            return SkillResult.NORMAL;
        }

        Messaging.send(player, "You cannot telekinetically intreract with that object!");
        return SkillResult.INVALID_TARGET_NO_MSG;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

}
