package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.ncp.NCPFunction;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

import fr.neatmonster.nocheatplus.checks.CheckType;

public class SkillAshura extends TargettedSkill {

    public SkillAshura(Heroes plugin) {
        super(plugin, "Ashura");
        setDescription("Deal $1 physical damage and force your target away from you. The push power is affected by your Strength. Coasts $2% of your sword.");
        setUsage("/skill ashura");
        setArgumentRange(0, 0);
        setIdentifiers("skill ashura");
        setTypes(SkillType.FORCE, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.6, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        int swordSacrificePercent = SkillConfigManager.getUseSetting(hero, this, "sword-sacrifice-percent", 5, false);
        
        return getDescription().replace("$1", damage + "").replace("$2", swordSacrificePercent + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 5);
        node.set(SkillSetting.DAMAGE.node(), 50);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 1.6);
        node.set("sword-sacrifice-percent", 5);
        node.set("horizontal-power", 1.5);
        node.set("horizontal-power-increase-per-strength", 0.0375);
        node.set("vertical-power", 0.25);
        node.set("vertical-power-increase-per-strength", 0.0075);
        node.set("ncp-exemption-duration", 1500);
        node.set("push-delay", 0.2);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, final LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        Material item = player.getItemInHand().getType();
        if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.swords).contains(item.name())) {
            Messaging.send(player, "You can't use Ashura with that weapon!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        
        short dura = player.getItemInHand().getDurability();
        short maxDura = player.getItemInHand().getType().getMaxDurability();
        short duraCost = (short) (maxDura * (SkillConfigManager.getUseSetting(hero, this, "sword-sacrifice-percent", 5, false) * 0.01));
        
        if(dura == (short)0) {
            player.getItemInHand().setDurability((short) (maxDura - duraCost));
        }
        else if(maxDura - dura > duraCost) {
            player.getItemInHand().setDurability((short) (dura + duraCost));
        }
        else if(maxDura - dura == duraCost) {
            player.setItemInHand(null);
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_BREAK, 0.5F, 1.0F);
        }
        else {
            Messaging.send(player, "Your Katana doesn't have enough durability to use Ashura!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        broadcastExecuteText(hero, target);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.6, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        if (damage > 0) {
            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.MAGIC, false);
        }

        Location playerLoc = player.getLocation();
        Location targetLoc = target.getLocation();

        Material mat = targetLoc.getBlock().getRelative(BlockFace.DOWN).getType();

        boolean weakenVelocity = false;
        switch (mat) {
            case STATIONARY_WATER:
            case STATIONARY_LAVA:
            case WATER:
            case LAVA:
            case SOUL_SAND:
                weakenVelocity = true;
                break;
            default:
                break;
        }

        double tempVPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", 0.25, false);
        double vPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "vertical-power-increase-per-strength", 0.0075, false);
        tempVPower += (vPowerIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        if (weakenVelocity)
            tempVPower *= 0.75;

        final double vPower = tempVPower;

        final Vector pushUpVector = new Vector(0, vPower, 0);
        // Let's bypass the nocheat issues...
        NCPUtils.applyExemptions(target, new NCPFunction() {
            
            @Override
            public void execute()
            {
                target.setVelocity(pushUpVector);                
            }
        }, Lists.newArrayList(CheckType.MOVING), SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 1500, false));

        final double xDir = targetLoc.getX() - playerLoc.getX();
        final double zDir = targetLoc.getZ() - playerLoc.getZ();

        double tempHPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", 1.5, false);
        double hPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "horizontal-power-increase-per-strength", 0.0375, false);
        tempHPower += hPowerIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        if (weakenVelocity)
            tempHPower *= 0.75;

        final double hPower = tempHPower;

        // Push them "up" first. THEN we can push them away.
        double delay = SkillConfigManager.getUseSetting(hero, this, "push-delay", 0.2, false);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                // Push them away
                //double yDir = player.getVelocity().getY();
                Vector pushVector = new Vector(xDir, 0, zDir).normalize().multiply(hPower).setY(vPower);
                target.setVelocity(pushVector);
            }
        }, (long) (delay * 20));

        player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), org.bukkit.Effect.MAGIC_CRIT, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.6, 0), org.bukkit.Effect.MAGIC_CRIT, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.7, 0), org.bukkit.Effect.MAGIC_CRIT, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.9, 0), org.bukkit.Effect.MAGIC_CRIT, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 1.0, 0), org.bukkit.Effect.MAGIC_CRIT, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 1.1, 0), org.bukkit.Effect.MAGIC_CRIT, 0, 0, 0, 0, 0, 1, 25, 16);

        return SkillResult.NORMAL;
    }
}