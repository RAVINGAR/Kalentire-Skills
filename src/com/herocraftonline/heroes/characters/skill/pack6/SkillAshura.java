package com.herocraftonline.heroes.characters.skill.pack6;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Util;

public class SkillAshura extends TargettedSkill {

    public SkillAshura(Heroes plugin) {
        super(plugin, "Ashura");
        setDescription("Deal $1 magical damage and force your target away from you. The push power is affected by your Strength. Costs $2% of your sword.");
        setUsage("/skill ashura");
        setArgumentRange(0, 0);
        setIdentifiers("skill ashura");
        setTypes(SkillType.FORCE, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.6, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        int swordSacrificePercent = SkillConfigManager.getUseSetting(hero, this, "sword-sacrifice-percent", 5, false);

        return getDescription().replace("$1", damage + "").replace("$2", swordSacrificePercent + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 5);
        node.set(SkillSetting.DAMAGE.node(), 50);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.6);
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

        if (!damageCheck(player, target)) {
            player.sendMessage("You can't damage that target!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        Material item = NMSHandler.getInterface().getItemInMainHand(player.getInventory()).getType();
        if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.swords).contains(item.name())) {
            player.sendMessage("You can't use Ashura with that weapon!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        int duraPercent = SkillConfigManager.getUseSetting(hero, this, "sword-sacrifice-percent", 5, false);
        if (duraPercent > 0) {
            short dura = NMSHandler.getInterface().getItemInMainHand(player.getInventory()).getDurability();
            short maxDura = NMSHandler.getInterface().getItemInMainHand(player.getInventory()).getType().getMaxDurability();
            short duraCost = (short) (maxDura * (duraPercent * 0.01));

            if (dura == (short) 0) {
                NMSHandler.getInterface().getItemInMainHand(player.getInventory()).setDurability((short) (duraCost));
            } else if (maxDura - dura > duraCost) {
                NMSHandler.getInterface().getItemInMainHand(player.getInventory()).setDurability((short) (dura + duraCost));
            } else if (maxDura - dura == duraCost) {
                NMSHandler.getInterface().setItemInMainHand(player.getInventory(), null);
                player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_ITEM_BREAK.value(), 0.5F, 1.0F);
            } else {
                player.sendMessage("Your Katana doesn't have enough durability to use Ashura!");
                return SkillResult.INVALID_TARGET_NO_MSG;
            }
        }

        broadcastExecuteText(hero, target);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.6, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

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
        }, Lists.newArrayList("MOVING"), SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 1500, false));

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

        player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), org.bukkit.Effect.MAGIC_CRIT, 0, 0, 0, 0, 0, 1, 95, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), org.bukkit.Effect.TILE_BREAK, 115, 3, 0.3F, 0.2F, 0.3F, 0.5F, 45, 16);

        return SkillResult.NORMAL;
    }
}
