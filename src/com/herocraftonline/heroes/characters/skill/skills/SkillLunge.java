package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;

public class SkillLunge extends TargettedSkill {

    private boolean ncpEnabled = false;

    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();

    public SkillLunge(Heroes plugin) {
        super(plugin, "Lunge");
        setDescription("Lunge towards your target! Targetting distance for this ability is increased by your Strength.");
        setUsage("/skill lunge");
        setArgumentRange(0, 0);
        setIdentifiers("skill lunge");
        setTypes(SkillType.VELOCITY_INCREASING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.AGGRESSIVE);

        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null)
            ncpEnabled = true;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(6));
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_STRENGTH.node(), Double.valueOf(0.15));
        node.set("horizontal-power", Double.valueOf(1.0));
        node.set("horizontal-power-increase-per-strength", Double.valueOf(0.0));
        node.set("vertical-power", Double.valueOf(0.4));
        node.set("ncp-exemption-duration", Integer.valueOf(1500));
        node.set("lunge-delay", Double.valueOf(0.2));

        return node;
    }

    @Override
    public SkillResult use(Hero hero, final LivingEntity target, String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        player.getWorld().playSound(player.getLocation(), Sound.HURT, 0.8F, 0.4F);
        
        // Let's bypass the nocheat issues...
        if (ncpEnabled) {
            if (!player.isOp()) {
                long duration = SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 1500, false);
                if (duration > 0) {
                    NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(this, player, duration);
                    hero.addEffect(ncpExemptEffect);
                }
            }
        }

        Location playerLoc = player.getLocation();
        Location targetLoc = target.getLocation();

        double tempVPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", Double.valueOf(0.4), false);
        final double vPower = tempVPower;

        Vector pushUpVector = new Vector(0, vPower, 0);
        player.setVelocity(pushUpVector);

        final double xDir = (targetLoc.getX() - playerLoc.getX()) / 3;
        final double zDir = (targetLoc.getZ() - playerLoc.getZ()) / 3;

        double tempHPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", Double.valueOf(0.5), false);
        double hPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "horizontal-power-increase-per-strength", Double.valueOf(0.0), false);
        tempHPower += hPowerIncrease * hero.getAttributeValue(AttributeType.STRENGTH);
        final double hPower = tempHPower;

        // push them "up" first. THEN lunge towards the target
        double delay = SkillConfigManager.getUseSetting(hero, this, "lunge-delay", 0.2, false);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                // Push them away
                Vector pushVector = new Vector(xDir, 0, zDir).normalize().multiply(hPower).setY(vPower);
                player.setVelocity(pushVector);
            }
        }, (long) (delay * 20));

        return SkillResult.NORMAL;
    }

    private class NCPExemptionEffect extends ExpirableEffect {

        public NCPExemptionEffect(Skill skill, Player applier, long duration) {
            super(skill, "NCPExemptionEffect_MOVING", applier, duration);
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
}