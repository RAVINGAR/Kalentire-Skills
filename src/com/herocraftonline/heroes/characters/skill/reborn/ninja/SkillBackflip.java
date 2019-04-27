package com.herocraftonline.heroes.characters.skill.reborn.ninja;

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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

import com.herocraftonline.heroes.characters.skill.reborn.ninja.SkillShurikens;

public class SkillBackflip extends ActiveSkill {

    public SkillBackflip(Heroes plugin) {
        super(plugin, "Backflip");
        setDescription("Backflip away from your enemies. $1Distance traveled is based on your Dexterity.$2");
        setUsage("/skill backflip");
        setArgumentRange(0, 0);
        setIdentifiers("skill backflip");
        setTypes(SkillType.VELOCITY_INCREASING, SkillType.ABILITY_PROPERTY_PROJECTILE, SkillType.AGGRESSIVE, SkillType.DAMAGING);
    }

    @Override
    public String getDescription(Hero hero) {

        String throwShurikenDescription = "";
        boolean throwShuriken = SkillConfigManager.getUseSetting(hero, this, "thow-shuriken", true);
        if (throwShuriken)
            throwShurikenDescription = "If you are able to currently throw Shuriken, you will do so as well. ";

        String frontFlipString = "";
        if (hero.canUseSkill("Frontflip"))
            frontFlipString = " This ability shares a cooldown with Frontflip.";

        return getDescription().replace("$1", throwShurikenDescription).replace("$2", frontFlipString);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("horizontal-power", 0.5);
        config.set("horizontal-power-increase-per-dexterity", 0.0125);
        config.set("vertical-power", 0.5);
        config.set("vertical-power-increase-per-dexterity", 0.00625);
        config.set("ncp-exemption-duration", 2000);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        Location playerLoc = player.getLocation();
        Material belowMat = playerLoc.getBlock().getRelative(BlockFace.DOWN).getType();

        broadcastExecuteText(hero);

        // Calculate backflip values
        float pitch = player.getEyeLocation().getPitch();
        if (pitch > 0) {
            pitch = -pitch;
        }
        float multiplier = (90f + pitch) / 50f;

        boolean weakenVelocity = false;
        switch (belowMat) {
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
        double vPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "vertical-power-increase-per-dexterity", 0.0, false);
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
        double hPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "horizontal-power-increase-per-dexterity", 0.0, false);
        hPower += dexterity * hPowerIncrease;

        if (weakenVelocity)
            hPower *= 0.75;

        velocity.multiply(new Vector(-hPower, 1, -hPower));

        // Let's bypass the nocheat issues...
        NCPUtils.applyExemptions(player, new NCPFunction() {

            @Override
            public void execute() {
                // Backflip!
                player.setVelocity(velocity);
                player.setFallDistance(-8f);
            }
        }, Lists.newArrayList("MOVING"), SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 1000, false));

        // If they can use shuriken, let's make them throw a few after they backflip
        boolean throwShuriken = SkillConfigManager.getUseSetting(hero, this, "thow-shuriken", true);
        if (throwShuriken) {
            if (hero.canUseSkill("Shuriken")) {
                SkillShurikens shurikenSkill = (SkillShurikens) plugin.getSkillManager().getSkill("Shuriken");

                if (shurikenSkill != null)
                    shurikenSkill.shurikenToss(player);
            }
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 4.0F, 1.0F);

        if (hero.canUseSkill("Frontflip")) {
            long cooldown = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 1000, false);
            hero.setCooldown("Frontflip", System.currentTimeMillis() + cooldown);
        }

        return SkillResult.NORMAL;
    }

    private static final Set<Material> requiredMaterials;

    static {
        requiredMaterials = new HashSet<>();
        requiredMaterials.add(Material.WATER);
        requiredMaterials.add(Material.LAVA);
        requiredMaterials.add(Material.AIR);
        requiredMaterials.add(Material.ACACIA_LEAVES);
        requiredMaterials.add(Material.BIRCH_LEAVES);
        requiredMaterials.add(Material.DARK_OAK_LEAVES);
        requiredMaterials.add(Material.JUNGLE_LEAVES);
        requiredMaterials.add(Material.OAK_LEAVES);
        requiredMaterials.add(Material.SPRUCE_LEAVES);
        requiredMaterials.add(Material.SOUL_SAND);
    }
}
