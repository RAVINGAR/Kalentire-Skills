package com.herocraftonline.heroes.characters.skill.remastered.dragoon;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.ncp.NCPFunction;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class SkillJump extends ActiveSkill {

    public SkillJump(Heroes plugin) {
        super(plugin, "Jump");
        setDescription("Jump forwards into the air.");
        setUsage("/skill jump");
        setArgumentRange(0, 0);
        setIdentifiers("skill jump");
        setTypes(SkillType.VELOCITY_INCREASING, SkillType.ABILITY_PROPERTY_PHYSICAL);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("no-air-jump", false);
        config.set("horizontal-power", 1.0);
        config.set("horizontal-power-increase-per-dexterity", 0.0);
        config.set("vertical-power", 0.5);
        config.set("vertical-power-increase-per-dexterity", 0.0);
        config.set("ncp-exemption-duration", 0);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        Location playerLoc = player.getLocation();
        Material belowMat = playerLoc.getBlock().getRelative(BlockFace.DOWN).getType();

        if ((SkillConfigManager.getUseSetting(hero, this, "no-air-jump", true) && requiredMaterials.contains(belowMat)) || player.isInsideVehicle()) {
            player.sendMessage("You can't jump while mid-air or from inside a vehicle!");
            return SkillResult.FAIL;
        }

        broadcastExecuteText(hero);

        // Calculate jump values
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

        velocity.multiply(new Vector(hPower, 1, hPower));

        long ncpExemptionDuration = SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 2000, false);
        if (ncpExemptionDuration > 0) {
            // Let's bypass the nocheat issues...
            NCPUtils.applyExemptions(player, new NCPFunction() {
                @Override
                public void execute() {
                    jump(player, velocity);
                }
            }, Lists.newArrayList("MOVING"), ncpExemptionDuration);
        } else {
            jump(player, velocity);
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5F, 1.0F);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 25, 0, 0.1, 0, 0.5);

        return SkillResult.NORMAL;
    }

    private void jump(Player player, Vector velocity) {
        player.setVelocity(velocity);
        player.setFallDistance(-8f);
    }

    private static final Set<Material> requiredMaterials;
    static {
        requiredMaterials = new HashSet<>();
        requiredMaterials.add(Material.WATER);
        requiredMaterials.add(Material.LAVA);
        requiredMaterials.add(Material.AIR);
        requiredMaterials.add(Material.BIRCH_LEAVES);
        requiredMaterials.add(Material.DARK_OAK_LEAVES);
        requiredMaterials.add(Material.JUNGLE_LEAVES);
        requiredMaterials.add(Material.OAK_LEAVES);
        requiredMaterials.add(Material.ACACIA_LEAVES);
        requiredMaterials.add(Material.SPRUCE_LEAVES);
        requiredMaterials.add(Material.SOUL_SAND);
    }
}