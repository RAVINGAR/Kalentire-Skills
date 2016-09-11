package com.herocraftonline.heroes.characters.skill.pack6;

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
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Messaging;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class SkillBackflip extends ActiveSkill {

    public SkillBackflip(Heroes plugin) {
        super(plugin, "Backflip");
        setDescription("Backflip away from your enemies.$1Distance traveled is based on your Dexterity.$2");
        setUsage("/skill backflip");
        setArgumentRange(0, 0);
        setIdentifiers("skill backflip");
        setTypes(SkillType.VELOCITY_INCREASING, SkillType.ABILITY_PROPERTY_PROJECTILE, SkillType.AGGRESSIVE, SkillType.DAMAGING);
    }

    @Override
    public String getDescription(Hero hero) {

        String throwShurikenDescription = " ";
        boolean throwShuriken = SkillConfigManager.getUseSetting(hero, this, "thow-shuriken", true);
        if (throwShuriken)
            throwShurikenDescription = " If you are able to currently throw Shuriken, you will do so as well. ";

        String frontFlipString = " ";
        if (hero.canUseSkill("Frontflip"))
            frontFlipString = " This ability shares a cooldown with Frontflip.";

        return getDescription().replace("$1", throwShurikenDescription).replace("$2", frontFlipString);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("no-air-backflip", false);
        node.set("horizontal-power", 0.5);
        node.set("horizontal-power-increase-per-dexterity", 0.0125);
        node.set("vertical-power", 0.5);
        node.set("vertical-power-increase-per-dexterity", 0.00625);
        node.set("ncp-exemption-duration", 2000);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        Location playerLoc = player.getLocation();
        Material belowMat = playerLoc.getBlock().getRelative(BlockFace.DOWN).getType();

        if ((SkillConfigManager.getUseSetting(hero, this, "no-air-backflip", true) && requiredMaterials.contains(belowMat)) || player.isInsideVehicle()) {
            Messaging.send(player, "You can't backflip while mid-air or from inside a vehicle!");
            return SkillResult.FAIL;
        }

        broadcastExecuteText(hero);

        // Calculate backflip values
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
            vPower *= 0.75;

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
            hPower *= 0.75;

        velocity.multiply(new Vector(-hPower, 1, -hPower));

        // Let's bypass the nocheat issues...
        NCPUtils.applyExemptions(player, new NCPFunction() {

            @Override
            public void execute()
            {
                // Backflip!
                player.setVelocity(velocity);
                player.setFallDistance(-8f);
            }
        }, Lists.newArrayList("MOVING"), SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 1000, false));

        // If they can use shuriken, let's make them throw a few after they backflip
        boolean throwShuriken = SkillConfigManager.getUseSetting(hero, this, "thow-shuriken", true);
        if (throwShuriken) {
            if (hero.canUseSkill("Shuriken")) {
                SkillShuriken shurikenSkill = (SkillShuriken) plugin.getSkillManager().getSkill("Shuriken");

                if (shurikenSkill != null)
                    shurikenSkill.shurikenToss(player);
            }
        }

        player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_ENDERDRAGON_FLAP.value(), 4.0F, 1.0F);

        if (hero.canUseSkill("Frontflip")) {
            long cooldown = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 1000, false);
            hero.setCooldown("Frontflip", System.currentTimeMillis() + cooldown);
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
