package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
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
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.characters.skill.ncp.NCPFunction;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;

import fr.neatmonster.nocheatplus.checks.CheckType;

public class SkillToss extends TargettedSkill {

    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();

    public SkillToss(Heroes plugin) {
        super(plugin, "Toss");
        setDescription("Grab your target and throw them in the opposite direction! Distance thrown is determined by your Strength.");
        setUsage("/skill toss");
        setArgumentRange(0, 0);
        setIdentifiers("skill toss");
        setTypes(SkillType.FORCE, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.INTERRUPTING, SkillType.MULTI_GRESSIVE, SkillType.NO_SELF_TARGETTING);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 5);
        node.set("horizontal-power", 0.5);
        node.set("horizontal-power-increase-per-strength", 0.025);
        node.set("vertical-power", 0.5);
        node.set("vertical-power-increase-per-strength", 0.0175);
        node.set("ncp-exemption-duration", 1500);
        node.set("toss-delay", 0.2);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, final LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        Location originalLoc = player.getLocation();
        Location flippedLoc = new Location(originalLoc.getWorld(), originalLoc.getX(), originalLoc.getY(), originalLoc.getZ(), (originalLoc.getYaw() < 180 ? originalLoc.getYaw() - 180 : originalLoc.getYaw() + 180), originalLoc.getPitch());
        player.teleport(flippedLoc);

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

        final double xDir = playerLoc.getX() - targetLoc.getX();
        final double zDir = playerLoc.getZ() - targetLoc.getZ();

        double tempHPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", 1.5, false);
        double hPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "horizontal-power-increase-per-strength", 0.0375, false);
        tempHPower += (hPowerIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        if (weakenVelocity)
            tempHPower *= 0.75;

        final double hPower = tempHPower;

        // Push them "up" first. THEN toss them.
        double delay = SkillConfigManager.getUseSetting(hero, this, "toss-delay", 0.2, false);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                Vector pushVector = new Vector(xDir, 0, zDir).normalize().multiply(hPower).setY(vPower);
                target.setVelocity(pushVector);
            }
        }, (long) (delay * 20));

        // Play sound        
        target.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), Effect.CLOUD, 0, 0, 0, 0, 0, 1, 100, 16);

        return SkillResult.NORMAL;
    }
}
