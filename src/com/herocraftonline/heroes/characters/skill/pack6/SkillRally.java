package com.herocraftonline.heroes.characters.skill.pack6;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.ncp.NCPFunction;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import com.herocraftonline.heroes.util.CompatSound;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SkillRally extends TargettedSkill {

    public SkillRally(Heroes plugin) {
        super(plugin, "Rally");
        setDescription("Rally towards your target! Targeting distance for this ability is increased by your Strength.");
        setUsage("/skill rally");
        setArgumentRange(0, 0);
        setIdentifiers("skill rally");
        setTypes(SkillType.VELOCITY_INCREASING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.NO_SELF_TARGETTING,
                SkillType.BUFFING, SkillType.VELOCITY_INCREASING);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 8);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_STRENGTH.node(), 0.15);
        node.set("vertical-power", 1.0);
        node.set("horizontal-divider", 6);
        node.set("vertical-divider", 8);
        node.set("multiplier", 1.0);
        node.set("jump-delay", 0.3);
        node.set("ncp-exemption-duration", 2000);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, final LivingEntity target, String[] args) {
        final Player player = hero.getPlayer();
        if (!(target instanceof Player))  {
            return SkillResult.INVALID_TARGET;
        }

        broadcastExecuteText(hero, target);

        player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_HORSE_GALLOP.value(), 0.8F, 0.4F);
        
        double vPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", 0.25, false);
        double vPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "vertical-power-increase-per-strength", 0.0075, false);
        vPower += (vPowerIncrease * hero.getAttributeValue(AttributeType.STRENGTH));
        final Vector pushUpVector = new Vector(0, vPower, 0);

        // Let's bypass the nocheat issues...
        NCPUtils.applyExemptions(player, new NCPFunction() {
            
            @Override
            public void execute()
            {
                player.setVelocity(pushUpVector);
            }
        }, Lists.newArrayList("MOVING"), SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 1500, false));

        final double horizontalDivider = SkillConfigManager.getUseSetting(hero, this, "horizontal-divider", 6, false);
        final double verticalDivider = SkillConfigManager.getUseSetting(hero, this, "vertical-divider", 8, false);
        final double multiplier = SkillConfigManager.getUseSetting(hero, this, "multiplier", 1.2, false);

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
            }
        }, (long) (delay * 20));

        
        //player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), Effect.CLOUD, 0, 0, 0, 0, 0, 1, 100, 16);
        player.getWorld().spawnParticle(Particle.CLOUD, target.getLocation().add(0, 0.5, 0), 100, 0, 0, 0, 1);

        return SkillResult.NORMAL;
    }
}