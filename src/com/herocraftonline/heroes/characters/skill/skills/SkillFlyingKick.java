package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
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
import com.herocraftonline.heroes.util.Util;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;

public class SkillFlyingKick extends TargettedSkill {

    private boolean ncpEnabled = false;

    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();

    public SkillFlyingKick(Heroes plugin) {
        super(plugin, "FlyingKick");
        setDescription("FlyingKick towards your target and deal $1 damage! Targetting distance for this ability is increased by your Agility.");
        setUsage("/skill flyingkick");
        setArgumentRange(0, 0);
        setIdentifiers("skill flyingkick");
        setTypes(SkillType.VELOCITY_INCREASING, SkillType.DAMAGING, SkillType.INTERRUPTING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.AGGRESSIVE);

        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null)
            ncpEnabled = true;
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.75, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        String formattedDamage = Util.decFormat.format(damage);

        return getDescription().replace("$1", formattedDamage);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 8);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_AGILITY.node(), 0.15);
        node.set(SkillSetting.DAMAGE.node(), 40);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 1.125);
        node.set("vertical-power", 1.0);
        node.set("horizontal-divider", 6);
        node.set("vertical-divider", 8);
        node.set("multiplier", 1.0);
        node.set("jump-delay", 0.3);
        node.set("ncp-exemption-duration", 2000);

        return node;
    }

    @Override
    public SkillResult use(final Hero hero, final LivingEntity target, String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        player.getWorld().playSound(player.getLocation(), Sound.HORSE_HIT, 18.0F, 0.4F);

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

        double vPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", 0.25, false);
        double vPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "vertical-power-increase-per-agility", 0.0075, false);
        vPower += (vPowerIncrease * hero.getAttributeValue(AttributeType.AGILITY));
        Vector pushUpVector = new Vector(0, vPower, 0);
        player.setVelocity(pushUpVector);

        final double horizontalDivider = SkillConfigManager.getUseSetting(hero, this, "horizontal-divider", 6, false);
        final double verticalDivider = SkillConfigManager.getUseSetting(hero, this, "vertical-divider", 8, false);
        final double multiplier = SkillConfigManager.getUseSetting(hero, this, "multiplier", 1.2, false);

        final double baseDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        final double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.75, false);

        double delay = SkillConfigManager.getUseSetting(hero, this, "jump-delay", 0.2, false);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                Location newPlayerLoc = player.getLocation();
                Location newTargetLoc = target.getLocation();

                double xDir = (newTargetLoc.getX() - newPlayerLoc.getX()) / horizontalDivider;
                double yDir = (newTargetLoc.getY() - newPlayerLoc.getY()) / verticalDivider;
                double zDir = (newTargetLoc.getZ() - newPlayerLoc.getZ()) / horizontalDivider;

                Vector vec = new Vector(xDir, yDir, zDir).multiply(multiplier);
                player.setVelocity(vec);
                player.setFallDistance(-8f);

                double damage = baseDamage + (damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

                addSpellTarget(target, hero);
                damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);
            }
        }, (long) (delay * 20));


        player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), org.bukkit.Effect.CRIT, 0, 0, 0, 0, 0, 1, 25, 16);
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
            NCPExemptionManager.exemptPermanently(player, CheckType.FIGHT);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.unexempt(player, CheckType.MOVING);
            NCPExemptionManager.unexempt(player, CheckType.FIGHT);
        }
    }
}