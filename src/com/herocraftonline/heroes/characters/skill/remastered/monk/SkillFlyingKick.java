package com.herocraftonline.heroes.characters.skill.remastered.monk;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.ncp.NCPFunction;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

public class SkillFlyingKick extends TargettedSkill {

    public SkillFlyingKick(Heroes plugin) {
        super(plugin, "FlyingKick");
        setDescription("FlyingKick towards your target and deal $1 physical damage!");
        setUsage("/skill flyingkick");
        setIdentifiers("skill flyingkick");
        setArgumentRange(0, 0);
        setTypes(SkillType.VELOCITY_INCREASING, SkillType.DAMAGING, SkillType.INTERRUPTING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);

        String formattedDamage = Util.decFormat.format(damage);

        return getDescription().replace("$1", formattedDamage);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 8);
        config.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_DEXTERITY.node(), 0.15);
        config.set(SkillSetting.DAMAGE.node(), 40.0);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 1.125);
        config.set("vertical-power", 1.0);
        config.set("horizontal-divider", 6);
        config.set("vertical-divider", 8);
        config.set("multiplier", 1.0);
        config.set("jump-delay", 0.3);
        config.set("ncp-exemption-duration", 2000);
        return config;
    }

    @Override
    public SkillResult use(final Hero hero, final LivingEntity target, String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0F, 0.5F);

        double vPower = SkillConfigManager.getScaledUseSettingDouble(hero, this, "vertical-power", false);
        final Vector pushUpVector = new Vector(0, vPower, 0);
        // Let's bypass the nocheat issues...
        NCPUtils.applyExemptions(player, new NCPFunction() {

            @Override
            public void execute() {
                player.setVelocity(pushUpVector);
            }
        }, Lists.newArrayList("MOVING"), SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 1500, false));

        final double horizontalDivider = SkillConfigManager.getUseSetting(hero, this, "horizontal-divider", 6, false);
        final double verticalDivider = SkillConfigManager.getUseSetting(hero, this, "vertical-divider", 8, false);
        final double multiplier = SkillConfigManager.getUseSetting(hero, this, "multiplier", 1.2, false);

        final double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);

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


                addSpellTarget(target, hero);
                damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);
            }
        }, (long) (delay * 20));

        //player.getWorld().spigot().playEffect(player.getLocation(), Effect.CLOUD, 0, 0, 0, 0.1F, 0, 0.5F, 25, 12);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 25, 0, 0.1, 0, 0.5);
        //player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), org.bukkit.Effect.CRIT, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 0.5, 0), 25, 0, 0, 0, 1);
        return SkillResult.NORMAL;
    }
}