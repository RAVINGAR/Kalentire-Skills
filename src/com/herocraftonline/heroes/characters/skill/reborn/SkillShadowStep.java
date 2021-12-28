package com.herocraftonline.heroes.characters.skill.reborn;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public class SkillShadowStep extends ActiveSkill {
    private final int _maxHitsBeforeDiminish = 3;
    private final double _diminishPercent = 0.15;
    private final double _maxDiminishPercent = 0.75;

    public SkillShadowStep(Heroes plugin) {
        super(plugin, "ShadowStep");
        setUsage("/skill shadowstep");
        setIdentifiers("skill shadowstep");
        setArgumentRange(0, 0);
        setDescription("You briefly transform, performing a tail whip, following by a lunge.");
        setTypes(SkillType.DAMAGING, SkillType.MOVEMENT_INCREASING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.AGGRESSIVE);
    }

    public String getDescription(Hero hero) {
        return getDescription();
//        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 35, true);
//        double bonusDmg = SkillConfigManager.getUseSetting(hero, this, "bonus-damage-per-level", 5, true);
//
//        return getDescription().replace("$1", damage + "").replace("$2", bonusDmg + "");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection cs = super.getDefaultConfig();
        cs.set(SkillSetting.DAMAGE.node(), 35.0);
        cs.set("damage-radius", 3.0);
        cs.set("bonus-damage-per-level", 5.0);
        cs.set("speed-boost-per-level", 0.000125);
        cs.set("speed-mult", 1.0001);
        //cs.set("horizontal-power", 0.5);
        //cs.set("vertical-power", 0.5);
        return cs;
    }

    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 35, true);
        double bonusDmg = SkillConfigManager.getUseSetting(hero, this, "bonus-damage-per-level", 5, true);

        final double speedMult = SkillConfigManager.getUseSetting(hero, this, "speed-mult", 1.0, true);
        double boost = hero.getHeroLevel() * SkillConfigManager.getUseSetting(hero, this, "speed-boost-per-level", 0.000125, true);
        double radius = SkillConfigManager.getUseSetting(hero, this, "damage-radius", 3.0, true);

        FlipPlayerAround(player);
        PerformDash(hero, player, damage, speedMult, boost, radius);

        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    private void FlipPlayerAround(Player player) {
        Location loc = player.getLocation();
        Location flipped = new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), (loc.getYaw() < 180 ? loc.getYaw() - 180 : loc.getYaw() + 180), loc.getPitch());
        player.teleport(flipped);
    }

    private void PerformDash(Hero hero, Player player, double damage, double speedMult, double boost, double radius) {
        final Vector velocity = player.getLocation().getDirection().clone().setY(0.0D).multiply(speedMult + boost);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 25, 0, 0.1, 0, 0.5);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0F, 1.0F);

        final ArrayList<LivingEntity> lungeEntitiesToHit = new ArrayList<>();
        new BukkitRunnable() {
            int ticks = 0;
            int maxTicks = 5;

            public void run() {
                if (ticks == maxTicks) {
                    player.setFallDistance(-3f);
                    cancel();
                    return;
                }
                player.setVelocity(velocity);

                for (Entity ent : player.getNearbyEntities(radius, radius, radius)) {
                    if (!(ent instanceof LivingEntity))
                        continue;
                    LivingEntity lEnt = (LivingEntity) ent;
                    if (!damageCheck(player, lEnt))
                        continue;
                    if (lungeEntitiesToHit.contains(lEnt))
                        continue;

                    int currentHitCount = lungeEntitiesToHit.size();
                    final double finalDamage = ApplyAoEDiminishingReturns(damage, currentHitCount);
                    addSpellTarget(lEnt, hero);
                    lungeEntitiesToHit.add(lEnt);
                    damageEntity(lEnt, player, finalDamage, EntityDamageEvent.DamageCause.ENTITY_ATTACK, false);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.0F, 1.0F);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private double ApplyAoEDiminishingReturns(double damage, int numberOfTargets) {
        return ApplyAoEDiminishingReturns(damage, numberOfTargets, 3, 0.15, 0.75);
    }

    private double ApplyAoEDiminishingReturns(double damage, int numberOfTargets, int maxTargetsBeforeDiminish, double diminishPercent, double maxDiminishPercent) {
        if (numberOfTargets > maxTargetsBeforeDiminish) {
            double totalDiminishPercent = (diminishPercent * numberOfTargets);
            if (totalDiminishPercent > maxDiminishPercent)
                totalDiminishPercent = maxDiminishPercent;
            return totalDiminishPercent / damage * 100;
        } else {
            return damage;
        }
    }
}
