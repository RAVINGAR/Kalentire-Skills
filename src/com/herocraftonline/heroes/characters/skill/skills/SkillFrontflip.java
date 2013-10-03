package com.herocraftonline.heroes.characters.skill.skills;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;

public class SkillFrontflip extends ActiveSkill {

    private boolean ncpEnabled = false;

    public SkillFrontflip(Heroes plugin) {
        super(plugin, "Frontflip");
        setDescription("Frontflip forwards into the air. Distance traveled is based on your Agility.$1");
        setUsage("/skill frontflip");
        setArgumentRange(0, 0);
        setIdentifiers("skill frontflip");
        setTypes(SkillType.VELOCITY_INCREASING, SkillType.ABILITY_PROPERTY_PHYSICAL);

        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null) {
            ncpEnabled = true;
        }
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
        node.set("horizontal-power-increase-per-agility", Double.valueOf(0.01));
        node.set("vertical-power", Double.valueOf(0.5));
        node.set("vertical-power-increase-per-agility", Double.valueOf(0.00625));
        node.set("ncp-exemption-duration", Integer.valueOf(2000));

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        Location playerLoc = player.getLocation();
        Material belowMat = playerLoc.getBlock().getRelative(BlockFace.DOWN).getType();

        if ((SkillConfigManager.getUseSetting(hero, this, "no-air-frontflip", true) && noFrontflipMaterials.contains(belowMat)) || player.isInsideVehicle()) {
            Messaging.send(player, "You can't frontflip while mid-air or from inside a vehicle!");
            return SkillResult.FAIL;
        }

        broadcastExecuteText(hero);

        // Let's bypass the nocheat issues...
        if (ncpEnabled) {
            if (!player.isOp()) {
                long duration = SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 2000, false);
                if (duration > 0) {
                    NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(this, player, duration);
                    hero.addEffect(ncpExemptEffect);
                }
            }
        }

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

        int agility = hero.getAttributeValue(AttributeType.AGILITY);

        double vPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", Double.valueOf(0.5), false);
        double vPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "vertical-power-increase-per-agility", Double.valueOf(0.0125), false);
        vPower += agility * vPowerIncrease;

        if (vPower > 2.0)
            vPower = 2.0;

        if (weakenVelocity)
            vPower /= 2;

        Vector velocity = player.getVelocity().setY(vPower);

        Vector directionVector = player.getLocation().getDirection();
        directionVector.setY(0);
        directionVector.normalize();
        directionVector.multiply(multiplier);

        velocity.add(directionVector);
        double hPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", Double.valueOf(0.5), false);
        double hPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "horizontal-power-increase-per-agility", Double.valueOf(0.0125), false);
        hPower += agility * hPowerIncrease;

        if (weakenVelocity)
            hPower /= 2;

        velocity.multiply(new Vector(hPower, 1, hPower));

        // Frontflip!
        player.setVelocity(velocity);
        player.setFallDistance(-8f);

        player.getWorld().playSound(player.getLocation(), Sound.SKELETON_IDLE, 10.0F, 1.0F);

        if (hero.canUseSkill("Backflip")) {
            long cooldown = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 1000, false);
            hero.setCooldown("Backflip", System.currentTimeMillis() + cooldown);
        }

        return SkillResult.NORMAL;
    }

    private class NCPExemptionEffect extends ExpirableEffect {

        public NCPExemptionEffect(Skill skill, Player applier, long duration) {
            super(skill, "NCPExemptionEffect_MOVING", applier, duration, null, null);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();

            NCPExemptionManager.exemptPermanently(player, CheckType.MOVING);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();

            NCPExemptionManager.unexempt(player, CheckType.MOVING);

        }
    }

    private static final Set<Material> noFrontflipMaterials;
    static {
        noFrontflipMaterials = new HashSet<Material>();
        noFrontflipMaterials.add(Material.STATIONARY_WATER);
        noFrontflipMaterials.add(Material.STATIONARY_LAVA);
        noFrontflipMaterials.add(Material.WATER);
        noFrontflipMaterials.add(Material.LAVA);
        noFrontflipMaterials.add(Material.AIR);
        noFrontflipMaterials.add(Material.LEAVES);
        noFrontflipMaterials.add(Material.SOUL_SAND);
    }
}
