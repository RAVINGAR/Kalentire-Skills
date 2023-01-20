package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.GeometryUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public class SkillSoulreaper extends ActiveSkill {

    public SkillSoulreaper(final Heroes plugin) {
        super(plugin, "Soulreaper");
        setDescription("Channeling the souls of the slain, you cast forth a glaive of souls. The glaive flies up " +
                "to $1 meter(s1) away and remains in a fixed location upon hitting an obstacle or at its maximum range. " +
                "While in being, the glaive deals $2 damage every $3 second(s2) to all targets within its radius of " +
                "$4 meter(s3). Once at a fixed location, the sickle remains in place for $5 second(s4).");
        setUsage("/skill soulreaper");
        setIdentifiers("skill soulreaper");
        setArgumentRange(0, 0);
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_DARK, SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT);
    }

    @Override
    public String getDescription(final Hero hero) {
        final double range = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 7, true);
        final double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 10, true) +
                (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.5, true) * hero.getAttributeValue(AttributeType.INTELLECT));
        final int damageTicks = SkillConfigManager.getUseSetting(hero, this, "ticks-between-damage", 5, true);
        final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 3.5, true);
        final int tickDuration = SkillConfigManager.getUseSetting(hero, this, "hover-ticks", 60, true);

        final float secondInterval = damageTicks / 20.0f;
        final float secondDuration = tickDuration / 20.0f;

        return getDescription().replace("$1", range + "")
                .replace("$2", damage + "")
                .replace("$3", "")
                .replace("$4", radius + "")
                .replace("$5", secondDuration + "")
                .replace("(s1)", range == 1.0D ? "" : "s")
                .replace("(s2)", secondInterval == 1.0f ? "" : "s")
                .replace("(s3)", radius == 1.0D ? "" : "s")
                .replace("(s4)", secondDuration == 1.0f ? "" : "s");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 7);
        node.set(SkillSetting.DAMAGE.node(), 10);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.5);
        node.set("ticks-between-damage", 5);
        node.set(SkillSetting.RADIUS.node(), 3.5);
        node.set("meters-travelled-per-second", 2);
        node.set("hover-ticks", 60);

        return node;
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        final double range = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 7, true);
        final double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 10, true) +
                (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.5, true) * hero.getAttributeValue(AttributeType.INTELLECT));
        final int damageTicks = SkillConfigManager.getUseSetting(hero, this, "ticks-between-damage", 5, true);
        final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 3.5, true);
        final float velocity = SkillConfigManager.getUseSetting(hero, this, "meters-travelled-per-second", 1, true);
        final int tickDuration = SkillConfigManager.getUseSetting(hero, this, "hover-ticks", 60, true);

        final Location spawnLoc = player.getEyeLocation().clone();
        final Location sickleEye = player.getEyeLocation().clone();

        final Vector directionVector = sickleEye.getDirection().normalize().divide(new Vector(10, 10, 10));

        new BukkitRunnable() {
            double distTraveled = 0.0D;
            boolean maxRange = false;
            int ticks = 0;
            int fxIndex = 0;

            int nextDamageTicks = 0;

            Location sickle = sickleEye;

            @Override
            public void run() {
                final List<Location> circle = GeometryUtil.circle(sickle, 16, radius);
                final Location l = circle.get(fxIndex++);
                //l.getWorld().spigot().playEffect(l, Effect.WITCH_MAGIC, 0, 0, 0.05f, 0.05f, 0.05f, 0.0f, 3, 128);
                final World world = sickle.getWorld();
                world.spawnParticle(Particle.SPELL_WITCH, l, 3, 0.05, 0.05, 0.05, 0);
                world.spawnParticle(Particle.ELECTRIC_SPARK, l, 12, 0.05, 0.05, 0.05, 0);
                if (fxIndex >= circle.size()) {
                    fxIndex = 0;
                }
                if (!maxRange) { // old fashioned boolean check
                    sickle.add(directionVector);
                    distTraveled = spawnLoc.distance(sickle);
                    if (distTraveled >= range) {
                        // check if in air
                        final Location foundLoc = sickle.clone().add(directionVector.multiply(radius));
                        int i = 0;
                        while (world.getBlockAt(foundLoc).getType().isAir() && i++ < 8) {
                            foundLoc.subtract(0, 1, 0);
                        }
                        sickle = foundLoc;
                        maxRange = true;
                    } else if (world.getBlockAt(sickle.clone().add(directionVector.multiply(radius))).getType().isSolid()) {
                        maxRange = true;
                    }
                } else {
                    ticks++;
                    if (ticks >= tickDuration) {
                        cancel();
                    }
                }

                if (nextDamageTicks == 0) {
                    nextDamageTicks = damageTicks;

                    sickle.getWorld().playSound(sickle, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 0.9f);
                    world.spawnParticle(Particle.SPELL_WITCH, sickle, 25, 0.2, 0.2, 0.2, 0);
                    for (final Entity e : world.getNearbyEntities(sickle, radius, radius, radius)) {
                        if (!(e instanceof LivingEntity)) {
                            continue;
                        }
                        final LivingEntity ent = (LivingEntity) e;
                        if (!damageCheck(player, ent)) {
                            continue;
                        }
                        addSpellTarget(ent, hero);
                        damageEntity(ent, player, damage, EntityDamageEvent.DamageCause.MAGIC, 0.0f);
                        ent.setNoDamageTicks(damageTicks - 1); // so they can be affected again
                    }
                } else {
                    nextDamageTicks--;
                }
            }
        }.runTaskTimer(plugin, 0, 1);

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }
}
