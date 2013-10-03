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
import com.herocraftonline.heroes.characters.skill.skills.SkillShuriken;
import com.herocraftonline.heroes.util.Messaging;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;

public class SkillBackflip extends ActiveSkill {

    private boolean ncpEnabled = false;

    public SkillBackflip(Heroes plugin) {
        super(plugin, "Backflip");
        setDescription("Backflip away from your enemies.$1Distance traveled is based on your Agility.$2");
        setUsage("/skill backflip");
        setArgumentRange(0, 0);
        setIdentifiers("skill backflip");
        setTypes(SkillType.VELOCITY_INCREASING, SkillType.ABILITY_PROPERTY_PROJECTILE, SkillType.AGGRESSIVE, SkillType.DAMAGING);

        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null) {
            ncpEnabled = true;
        }
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
        node.set("horizontal-power", Double.valueOf(0.5));
        node.set("horizontal-power-increase-per-agility", Double.valueOf(0.0125));
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

        if ((SkillConfigManager.getUseSetting(hero, this, "no-air-backflip", true) && nobackflipMaterials.contains(belowMat)) || player.isInsideVehicle()) {
            Messaging.send(player, "You can't backflip while mid-air or from inside a vehicle!");
            return SkillResult.FAIL;
        }

        broadcastExecuteText(hero);

        // Let's bypass the nocheat issues...
        if (ncpEnabled) {
            if (!player.isOp()) {
                long duration = SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 1000, false);
                if (duration > 0) {
                    NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(this, player, duration);
                    hero.addEffect(ncpExemptEffect);
                }
            }
        }

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

        velocity.multiply(new Vector(-hPower, 1, -hPower));

        // Backflip!
        player.setVelocity(velocity);
        player.setFallDistance(-8f);

        // If they can use shuriken, let's make them throw a few after they backflip
        boolean throwShuriken = SkillConfigManager.getUseSetting(hero, this, "thow-shuriken", true);
        if (throwShuriken) {
            if (hero.canUseSkill("Shuriken")) {
                SkillShuriken shurikenSkill = (SkillShuriken) plugin.getSkillManager().getSkill("Shuriken");

                if (shurikenSkill != null)
                    shurikenSkill.shurikenToss(player);
            }
        }

        player.getWorld().playSound(player.getLocation(), Sound.SKELETON_IDLE, 10.0F, 1.0F);

        if (hero.canUseSkill("Frontflip")) {
            long cooldown = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 1000, false);
            hero.setCooldown("Frontflip", System.currentTimeMillis() + cooldown);
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

    private static final Set<Material> nobackflipMaterials;
    static {
        nobackflipMaterials = new HashSet<Material>();
        nobackflipMaterials.add(Material.STATIONARY_WATER);
        nobackflipMaterials.add(Material.STATIONARY_LAVA);
        nobackflipMaterials.add(Material.WATER);
        nobackflipMaterials.add(Material.LAVA);
        nobackflipMaterials.add(Material.AIR);
        nobackflipMaterials.add(Material.LEAVES);
        nobackflipMaterials.add(Material.SOUL_SAND);
    }
}
