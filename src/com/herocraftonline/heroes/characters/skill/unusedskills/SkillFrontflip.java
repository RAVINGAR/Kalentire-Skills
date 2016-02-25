package com.herocraftonline.heroes.characters.skill.unusedskills;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.ncp.NCPFunction;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import com.herocraftonline.heroes.util.Messaging;

public class SkillFrontflip extends ActiveSkill {

    public SkillFrontflip(Heroes plugin) {
        super(plugin, "Frontflip");
        setDescription("Frontflip forwards into the air. Distance traveled is based on your Dexterity.$1");
        setUsage("/skill frontflip");
        setArgumentRange(0, 0);
        setIdentifiers("skill frontflip");
        setTypes(SkillType.VELOCITY_INCREASING, SkillType.ABILITY_PROPERTY_PHYSICAL);
    }

    @Override
    public String getDescription(Hero hero) {

        String backflipString = " ";
        if (hero.canUseSkill("Backflip"))
            backflipString = "This ability shares a cooldown with Backflip.";

        return getDescription().replace("$1", backflipString);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("no-air-frontflip", false);
        node.set("horizontal-power", Double.valueOf(0.4));
        node.set("horizontal-power-increase-per-dexterity", Double.valueOf(0.01));
        node.set("vertical-power", Double.valueOf(0.5));
        node.set("vertical-power-increase-per-dexterity", Double.valueOf(0.00625));
        node.set("ncp-exemption-duration", Integer.valueOf(2000));

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        Location playerLoc = player.getLocation();
        Material belowMat = playerLoc.getBlock().getRelative(BlockFace.DOWN).getType();

        if ((SkillConfigManager.getUseSetting(hero, this, "no-air-frontflip", true) && requiredMaterials.contains(belowMat)) || player.isInsideVehicle()) {
            Messaging.send(player, "You can't frontflip while mid-air or from inside a vehicle!");
            return SkillResult.FAIL;
        }

        broadcastExecuteText(hero);

        // Calculate frontflip values
        float pitch = player.getEyeLocation().getPitch();
        if (pitch > 0) {
            pitch = -pitch;
        }
        float multiplier = (90f + pitch) / 50f;

        boolean weakenVelocity = false;
        switch (belowMat) {
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

        int dexterity = hero.getAttributeValue(AttributeType.DEXTERITY);

        double vPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", 0.5, false);
        double vPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "vertical-power-increase-per-dexterity", 0.0125, false);
        vPower += dexterity * vPowerIncrease;

        if (vPower > 2.0)
            vPower = 2.0;

        if (weakenVelocity)
            vPower /= 2;

        final Vector velocity = player.getVelocity().setY(vPower);

        Vector directionVector = player.getLocation().getDirection();
        directionVector.setY(0);
        directionVector.normalize();
        directionVector.multiply(multiplier);

        velocity.add(directionVector);
        double hPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", 0.5, false);
        double hPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "horizontal-power-increase-per-dexterity", 0.0125, false);
        hPower += dexterity * hPowerIncrease;

        if (weakenVelocity)
            hPower /= 2;

        velocity.multiply(new Vector(hPower, 1, hPower));

        // Let's bypass the nocheat issues...
        NCPUtils.applyExemptions(player, new NCPFunction() {

            @Override
            public void execute()
            {
                // Frontflip!
                player.setVelocity(velocity);
                player.setFallDistance(-8f);
            }
        }, Lists.newArrayList("MOVING"), SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 2000, false));

        player.getWorld().playSound(player.getLocation(), Sound.SKELETON_IDLE, 10.0F, 1.0F);

        if (hero.canUseSkill("Backflip")) {
            long cooldown = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 1000, false);
            hero.setCooldown("Backflip", System.currentTimeMillis() + cooldown);
        }

        return SkillResult.NORMAL;
    }

    private static final Set<Material> requiredMaterials;
    static {
        requiredMaterials = new HashSet<>();
        requiredMaterials.add(Material.STATIONARY_WATER);
        requiredMaterials.add(Material.STATIONARY_LAVA);
        requiredMaterials.add(Material.WATER);
        requiredMaterials.add(Material.LAVA);
        requiredMaterials.add(Material.AIR);
        requiredMaterials.add(Material.LEAVES);
        requiredMaterials.add(Material.SOUL_SAND);
    }
}
