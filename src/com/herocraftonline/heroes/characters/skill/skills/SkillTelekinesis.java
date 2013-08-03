package com.herocraftonline.heroes.characters.skill.skills;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.BlockIterator;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;

public class SkillTelekinesis extends ActiveSkill {

    private boolean ncpEnabled = false;

    public SkillTelekinesis(Heroes plugin) {
        super(plugin, "Telekinesis");
        setDescription("You can activate levers, buttons and other interactable objects from afar.");
        setUsage("/skill telekinesis");
        setArgumentRange(0, 0);
        setIdentifiers("skill telekinesis");
        setTypes(SkillType.FORCE, SkillType.KNOWLEDGE, SkillType.SILENCABLE);

        try {
            if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null) {
                ncpEnabled = true;
            }
        }
        catch (Exception e) {}
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

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int maxDist = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 15, false);
        Block targetBlock = player.getTargetBlock(null, maxDist);
        if (targetBlock.getType() == Material.AIR) {
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        BlockIterator iter = null;
        try {
            iter = new BlockIterator(player, maxDist);
        }
        catch (IllegalStateException e) {
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        List<String> allowedBlocks = SkillConfigManager.getUseSetting(hero, this, "allowed-blocks", new ArrayList<String>());

        // Make sure the player's target is actually "visible" by cycling through each block between him and his target.
        Block tempBlock = null;
        while (iter.hasNext()) {
            tempBlock = iter.next();
            // Messaging.send(player, "Iterating. CurrentBlock: " + tempBlock.getType().toString());        // DEBUG

            // Some "transparent" blocks are actually what we are looking for, so check those first.
            if (allowedBlocks.contains(tempBlock.getType().toString())) {
                targetBlock = tempBlock;
                // Messaging.send(player, "Stopped because we found a proper block");       // DEBUG
                break;
            }
            else if (!(Util.transparentBlocks.contains(tempBlock.getType())
            && (Util.transparentBlocks.contains(tempBlock.getRelative(BlockFace.UP).getType())
            || Util.transparentBlocks.contains(tempBlock.getRelative(BlockFace.DOWN).getType())))) {

                // if the block is not transparent, it should become the new "target" block.
                // Messaging.send(player, "Stopped because we hit a wall or something.");       // DEBUG
                targetBlock = tempBlock;
                break;
            }
        }

        Material blockMaterial = targetBlock.getType();
        // Messaging.send(player, "BlockMaterial: " + blockMaterial.toString());        // DEBUG

        if (allowedBlocks.contains(blockMaterial.toString())) {
            // Messaging.send(player, "Interacting with a " + blockMaterial.toString());        // DEBUG

            // Let's bypass the nocheat issues...
            if (ncpEnabled) {
                if (!player.isOp()) {
                    NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(this);
                    hero.addEffect(ncpExemptEffect);
                }
            }

            PlayerInteractEvent fakeInteractEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, tempBlock, BlockFace.UP);
            plugin.getServer().getPluginManager().callEvent(fakeInteractEvent);

            // Old method
            //            net.minecraft.server.v1_6_R2.Block.byId[blockMaterial.getId()].interact(((CraftWorld) targetBlock.getWorld()).getHandle(),
            //                                                                                    targetBlock.getX(),
            //                                                                                    targetBlock.getY(), targetBlock.getZ(), null, 0, 0, 0, 0);

            // Let's bypass the nocheat issues...
            if (ncpEnabled) {
                if (!player.isOp()) {
                    if (hero.hasEffect("NCPExemptionEffect_BLOCKINTERACT"))
                        hero.removeEffect(hero.getEffect("NCPExemptionEffect_BLOCKINTERACT"));
                }
            }

            return SkillResult.NORMAL;
        }
        else {
            // Messaging.send(player, "You cannot interact with a " + blockMaterial.toString());       // DEBUG
            Messaging.send(player, "You cannot telekinetically interact with that object!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

    }

    private class NCPExemptionEffect extends Effect {

        public NCPExemptionEffect(Skill skill) {
            super(skill, "NCPExemptionEffect_BLOCKINTERACT");
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.exemptPermanently(player, CheckType.BLOCKINTERACT);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.unexempt(player, CheckType.BLOCKINTERACT);
        }
    }
}
