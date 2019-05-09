package com.herocraftonline.heroes.characters.skill.reborn.ninja;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

public class SkillShadowstep extends TargettedSkill {

    public SkillShadowstep(Heroes plugin) {
        super(plugin, "Shadowstep");
        setDescription("Shadow step your target, teleporting behind them and dealing $1 damage.");
        setUsage("/skill shadowstep");
        setIdentifiers("skill shadowstep");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.TELEPORTING, SkillType.SILENCEABLE, SkillType.STEALTHY);
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 4.0, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.15, false);
        damage += hero.getAttributeValue(AttributeType.STRENGTH) * damageIncrease;

        return getDescription()
                .replace("$1", Util.decFormat.format(damage));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 8);
        config.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), 0.0);
        config.set(SkillSetting.DAMAGE.node(), 35.0);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 0.0);
        config.set("teleport-blocks-behind-target", 1);
        config.set(SkillSetting.USE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% ShadowStepped behind %target%!");
        return config;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (target == player || !(target instanceof Player))
            return SkillResult.INVALID_TARGET_NO_MSG;

        Location playerLoc = player.getLocation();
        Location targetLoc = target.getLocation().clone();
        targetLoc.setPitch(0);      // Reset pitch so that we don't have to worry about it.

        BlockIterator iter = null;
        try {
            Vector direction = targetLoc.getDirection().multiply(-1);
            int blocksBehindTarget = SkillConfigManager.getUseSetting(hero, this, "teleport-blocks-behind-target", 1, false);
            iter = new BlockIterator(target.getWorld(), targetLoc.toVector(), direction, 0, blocksBehindTarget);
        } catch (IllegalStateException e) {
            player.sendMessage("There was an error getting the Shadowstep location!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        Block prev = null;
        Block b;
        while (iter.hasNext()) {
            b = iter.next();

            // Validate blocks near destination
            if (Util.transparentBlocks.contains(b.getType()) && (
                    Util.transparentBlocks.contains(b.getRelative(BlockFace.UP).getType()) ||
                    Util.transparentBlocks.contains(b.getRelative(BlockFace.DOWN).getType()))) {
                prev = b;
            } else {
                break;
            }
        }

        if (prev == null) {
            player.sendMessage("No location to shadowstep to.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        broadcastExecuteText(hero, target);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50.0, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK, false);

        Location targetTeleportLoc = prev.getLocation().clone();
        targetTeleportLoc.add(new Vector(.5, 0, .5));

        // Set the blink location yaw/pitch to that of the target
        targetTeleportLoc.setPitch(0);
        targetTeleportLoc.setYaw(targetLoc.getYaw());
        player.teleport(targetTeleportLoc);

        //plugin.getCharacterManager().getCharacter(target).addEffect(new StunEffect(this, player, duration));
        player.getWorld().playEffect(playerLoc, Effect.ENDER_SIGNAL, 3);
        player.getWorld().playSound(playerLoc, Sound.ENTITY_ENDERMEN_TELEPORT, 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }
}
