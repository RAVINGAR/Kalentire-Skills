package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.GeometryUtil;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class SkillSoulreaper extends ActiveSkill {

    public SkillSoulreaper(Heroes plugin) {
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

    public String getDescription(Hero hero) {
        final double range = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 7, true);
        final double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 10, true) +
                (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.5, true) * hero.getAttributeValue(AttributeType.INTELLECT));
        final int damageTicks = SkillConfigManager.getUseSetting(hero, this, "ticks-between-damage", 5, true);
        final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 3.5, true);
        final int tickDuration = SkillConfigManager.getUseSetting(hero, this, "hover-ticks", 60, true);

        float secondInterval = damageTicks / 20.0f;
        float secondDuration = tickDuration / 20.0f;

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

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 7);
        node.set(SkillSetting.DAMAGE.node(), 10);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.5);
        node.set("ticks-between-damage", 5);
        node.set(SkillSetting.RADIUS.node(), 3.5);
        node.set("meters-travelled-per-second", 2);
        node.set("hover-ticks", 60);

        return node;
    }

    public SkillResult use(final Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        final double range = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 7, true);
        final double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 10, true) +
                (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.5, true) * hero.getAttributeValue(AttributeType.INTELLECT));
        final int damageTicks = SkillConfigManager.getUseSetting(hero, this, "ticks-between-damage", 5, true);
        final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 3.5, true);
        final float velocity = SkillConfigManager.getUseSetting(hero, this, "meters-travelled-per-second", 1, true);
        final int tickDuration = SkillConfigManager.getUseSetting(hero, this, "hover-ticks", 60, true);

        final Location spawnLoc = player.getEyeLocation().clone();
        final Location sickle = player.getEyeLocation().clone();

        final Vector directionVector = sickle.getDirection().normalize().divide(new Vector(20, 20, 20));
        player.sendMessage("    " + ChatComponents.GENERIC_SKILL + directionVector.getX() + " / " + directionVector.getY() + " / " + directionVector.getZ());

        new BukkitRunnable() {
            double distTraveled = 0.0D;
            boolean maxRange = false;
            int ticks = 0;
            int fxIndex = 0;

            int nextDamageTicks = 0;

            public void run() {
                List<Location> circle = GeometryUtil.circle(sickle, 16, radius);
                Location l = circle.get(fxIndex++);
                    //l.getWorld().spigot().playEffect(l, Effect.WITCH_MAGIC, 0, 0, 0.05f, 0.05f, 0.05f, 0.0f, 3, 128);
                l.getWorld().spawnParticle(Particle.SPELL_WITCH, l, 3, 0.05, 0.05, 0.05, 0, true);
                if (fxIndex >= circle.size()) fxIndex = 0;
                if (maxRange == false) { // old fashioned boolean check
                    sickle.add(directionVector);
                    distTraveled = spawnLoc.distance(sickle);
                    if (distTraveled >= range ||
                            sickle.getWorld().getBlockAt(sickle.clone().add(directionVector.multiply(radius))).getType()
                                    .isSolid()) maxRange = true;
                } else {
                    ticks++;
                    player.sendMessage("    " + ChatComponents.GENERIC_SKILL + ticks);
                    if (ticks >= tickDuration) cancel();
                }

                if (nextDamageTicks == 0) {
                    nextDamageTicks = damageTicks;

                    sickle.getWorld().playSound(sickle, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 1.0f); // i have no clue what this sounds like
                    Snowball test = (Snowball) sickle.getWorld().spawnEntity(sickle, EntityType.SNOWBALL);
                    //ghost(test);
//                    test.getWorld().spigot().playEffect(test.getLocation(), Effect.WITCH_MAGIC, 0, 0, 0.2f, 0.2f, 0.2f, 0.0f,
//                            25, 128);
                    test.getWorld().spawnParticle(Particle.SPELL_WITCH, test.getLocation(), 25, 0.2, 0.2, 0.2, 0, true);
                    for (Entity e : test.getNearbyEntities(radius, radius / 2, radius)) {
                        if (!(e instanceof LivingEntity)) continue;
                        LivingEntity ent = (LivingEntity) e;
                        if (!damageCheck(player, ent)) continue;
                        addSpellTarget(ent, hero);
                        damageEntity(ent, player, damage, EntityDamageEvent.DamageCause.MAGIC, false);
                        ent.setNoDamageTicks(damageTicks - 1); // so they can be affected again
                    }
                    test.remove();
                } else nextDamageTicks--;
            }
        }.runTaskTimer(plugin, 0, 1);

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }
}
