package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
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

public class SkillToss extends TargettedSkill {

    private boolean ncpEnabled = false;

    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();

    public SkillToss(Heroes plugin) {
        super(plugin, "Toss");
        setDescription("Grab your target and throw them in the opposite direction! Distance thrown is determined by your Strength.");
        setUsage("/skill toss");
        setArgumentRange(0, 0);
        setIdentifiers("skill toss");
        setTypes(SkillType.FORCE, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.INTERRUPTING, SkillType.MULTI_GRESSIVE, SkillType.NO_SELF_TARGETTING);

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

        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(5));
        node.set("horizontal-power", Double.valueOf(0.5));
        node.set("horizontal-power-increase-per-strength", Double.valueOf(0.025));
        node.set("vertical-power", Double.valueOf(0.5));
        node.set("vertical-power-increase-per-strength", Double.valueOf(0.0175));
        node.set("ncp-exemption-duration", 1500);
        node.set("toss-delay", Double.valueOf(0.2));

        return node;
    }

    @Override
    public SkillResult use(Hero hero, final LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        // Let's bypass the nocheat issues...
        if (ncpEnabled) {
            if (target instanceof Player) {
                Player targetPlayer = (Player) target;
                if (!targetPlayer.isOp()) {
                    long duration = SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 1500, false);
                    if (duration > 0) {
                        NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(this, targetPlayer, duration);
                        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
                        targetCT.addEffect(ncpExemptEffect);
                    }
                }
            }
        }

        Location originalLoc = player.getLocation();
        Location flippedLoc = new Location(originalLoc.getWorld(), originalLoc.getX(), originalLoc.getY(), originalLoc.getZ(), (originalLoc.getYaw() < 180 ? originalLoc.getYaw() - 180 : originalLoc.getYaw() + 180), originalLoc.getPitch());
        player.teleport(flippedLoc);

        Location playerLoc = player.getLocation();
        Location targetLoc = target.getLocation();

        double tempVPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", Double.valueOf(0.25), false);
        double vPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "vertical-power-increase-per-strength", Double.valueOf(0.0075), false);
        tempVPower += (vPowerIncrease * hero.getAttributeValue(AttributeType.STRENGTH));
        final double vPower = tempVPower;

        Vector pushUpVector = new Vector(0, vPower, 0);
        target.setVelocity(pushUpVector);

        final double xDir = playerLoc.getX() - targetLoc.getX();
        final double zDir = playerLoc.getZ() - targetLoc.getZ();

        double tempHPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", Double.valueOf(1.5), false);
        double hPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "horizontal-power-increase-per-strength", Double.valueOf(0.0375), false);
        tempHPower += (hPowerIncrease * hero.getAttributeValue(AttributeType.STRENGTH));
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
