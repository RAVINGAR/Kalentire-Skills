package com.herocraftonline.heroes.characters.skill.skills;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;

public class SkillBackflip extends ActiveSkill {

    private boolean ncpEnabled = false;

    public SkillBackflip(Heroes plugin) {
        super(plugin, "Backflip");
        setDescription("Do a backflip into the air.");
        setUsage("/skill backflip");
        setArgumentRange(0, 0);
        setIdentifiers("skill backflip");
        setTypes(SkillType.MOVEMENT, SkillType.PHYSICAL, SkillType.HARMFUL, SkillType.DAMAGING);

        try {
            if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null) {
                ncpEnabled = true;
            }
        }
        catch (Exception e) {}
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("no-air-backflip", false);
        node.set("horizontal-power", Double.valueOf(1.0));
        node.set("vertical-power", Double.valueOf(1.0));
        node.set("ncp-exemption-duration", Integer.valueOf(1000));
        node.set("throw-shuriken", true);
        node.set("use-backflip-shuriken-values", false);
        node.set("num-shuriken", Integer.valueOf(3));
        node.set("degrees", Double.valueOf(15));
        node.set("interval", Double.valueOf(0.5));
        node.set("velocity-multiplier", Double.valueOf(3.0));

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        Material mat = player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType();
        if ((SkillConfigManager.getUseSetting(hero, this, "no-air-backflip", true) && nobackflipMaterials.contains(mat)) || player.isInsideVehicle()) {
            Messaging.send(player, "You can't backflip while mid-air or from inside a vehicle!");
            return SkillResult.FAIL;
        }

        // Let's bypass the nocheat issues...
        if (ncpEnabled) {
            if (!player.isOp()) {
                long duration = SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 1000, false);
                NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(this, duration);
                hero.addEffect(ncpExemptEffect);
            }
        }

        // Calculate backflip values
        float pitch = player.getEyeLocation().getPitch();
        if (pitch > 0) {
            pitch = -pitch;
        }
        float multiplier = (90f + pitch) / 50f;

        double vPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", 1.0, false);
        Vector velocity = player.getVelocity().setY(vPower);

        Vector directionVector = player.getLocation().getDirection();
        directionVector.setY(0);
        directionVector.normalize();
        directionVector.multiply(multiplier);

        velocity.add(directionVector);
        double hPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", 1.0, false);
        velocity.multiply(new Vector(-hPower, 1, -hPower));

        // Backflip!
        player.setVelocity(velocity);
        player.setFallDistance(-8f);

        // If they can use shuriken, let's make them throw a few after they backflip
        boolean throwShuriken = SkillConfigManager.getUseSetting(hero, this, "thow-shuriken", true);
        if (throwShuriken) {
            if (hero.canUseSkill("Shuriken")) {
                SkillShuriken shurikenSkill = (SkillShuriken) plugin.getSkillManager().getSkill("Shuriken");

                if (shurikenSkill != null) {
                    boolean useBackflipShurikenValues = SkillConfigManager.getUseSetting(hero, this, "use-backflip-shuriken-values", false);
                    if (useBackflipShurikenValues) {
                        int numShuriken = SkillConfigManager.getUseSetting(hero, this, "num-shuriken", 3, false);

                        double degrees = SkillConfigManager.getUseSetting(hero, this, "degrees", 10, false);
                        double interval = SkillConfigManager.getUseSetting(hero, this, "interval", 0.2, false);
                        double velocityMultiplier = SkillConfigManager.getUseSetting(hero, this, "velocity-multiplier", 3.0, false);

                        shurikenSkill.shurikenToss(player, numShuriken, degrees, interval, velocityMultiplier);
                    }
                    else
                        shurikenSkill.shurikenToss(player);
                }
            }
        }

        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.SKELETON_IDLE, 1.0F, 1.0F);
        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    private class NCPExemptionEffect extends ExpirableEffect {

        public NCPExemptionEffect(Skill skill, long duration) {
            super(skill, "NCPExemptionEffect", duration);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.exemptPermanently(player, CheckType.MOVING);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.unexempt(player, CheckType.MOVING);

        }
    }

    private static final Set<Material> nobackflipMaterials;
    static {
        nobackflipMaterials = new HashSet<Material>();
        nobackflipMaterials.add(Material.WATER);
        nobackflipMaterials.add(Material.AIR);
        nobackflipMaterials.add(Material.LAVA);
        nobackflipMaterials.add(Material.LEAVES);
        nobackflipMaterials.add(Material.SOUL_SAND);
    }
}
