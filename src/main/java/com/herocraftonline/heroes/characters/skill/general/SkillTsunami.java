package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.libs.slikey.effectlib.Effect;
import com.herocraftonline.heroes.libs.slikey.effectlib.EffectManager;
import com.herocraftonline.heroes.libs.slikey.effectlib.EffectType;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SkillTsunami extends ActiveSkill {

    public SkillTsunami(final Heroes plugin) {
        super(plugin, "Tsunami");
        setDescription("You launch a Tsunami forward for $1 seconds, dealing $2 damage to $3enemies it hits.");
        setUsage("/skill tsunami");
        setIdentifiers("skill tsunami");
        setArgumentRange(0, 0);
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_WATER, SkillType.AGGRESSIVE);
    }

    public static double randomDouble(final double min, final double max) {
        return Math.random() < 0.5 ? ((1 - Math.random()) * (max - min) + min) : (Math.random() * (max - min) + min);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), 200.0);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.0);
        node.set(SkillSetting.DURATION.node(), 10);
        node.set(SkillSetting.DURATION_INCREASE_PER_WISDOM.node(), 1);
        node.set("knockup", true);
        node.set("knockup-velocity", 1.0);
        node.set("knockup-multiplier", 2.0);

        return node;
    }

    @Override
    public String getDescription(final Hero hero) {
        final int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        final double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        final boolean knockup = SkillConfigManager.getUseSetting(hero, this, "knockup", true);

        return getDescription().replace("$1", Util.decFormat.format(duration / 20))
                .replace("$2", Util.decFormat.format(damage))
                .replace("$3", knockup ? "and knocking up " : "");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] strings) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        // Get configs
        final int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, 10, false);
        final double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, 200.0, false);

        final boolean knockup = SkillConfigManager.getUseSetting(hero, this, "knockup", true);
        final double knockupVelocity = SkillConfigManager.getUseSetting(hero, this, "knockup-velocity", 1.0, false);
        final double knockupMultiplier = SkillConfigManager.getUseSetting(hero, this, "knockup-multiplier", 2.0, false);

        // Set vector for movement of the effect
        final Vector v = player.getLocation().getDirection().normalize().multiply(0.3);
        v.setY(0);
        final Location loc = player.getLocation().subtract(0, 1, 0).add(v);

        // Initialize and configure the Effect
        final TsunamiEffect te = new TsunamiEffect(effectLib);
        te.setLocation(loc);
        te.asynchronous = true;

        te.redDustColor = Color.BLUE;
        te.addVector = v;
        te.iterations = duration;

        te.caster = hero;
        te.casterPlayer = player;

        te.damage = damage;
        te.knockup = knockup;
        te.knockupVelocity = knockupVelocity;
        te.knockupMultiplier = knockupMultiplier;

        // Start the Effect
        effectLib.start(te);

        return SkillResult.NORMAL;
    }

    private class TsunamiEffect extends Effect {

        // Effect settings
        public final Particle explosionParticle = Particle.EXPLOSION_NORMAL;
        public final Particle waterDripParticle = Particle.DRIP_WATER;
        public final Particle redDustParticle = Particle.REDSTONE;
        // Skill damage settings
        private final List<LivingEntity> hitTargets;
        public Color redDustColor = null;
        public Vector addVector = new Vector(0, 0, 0);
        public Hero caster;
        public Player casterPlayer;
        public double damage;
        public boolean knockup;
        public double knockupVelocity;
        public double knockupMultiplier;

        public TsunamiEffect(final EffectManager manager) {
            super(manager);
            type = EffectType.REPEATING;
            period = 1;
            iterations = 40; // = 2 sec.

            hitTargets = new ArrayList<>();
            caster = null;
            casterPlayer = null;

            // Default settings in case a caster is set but no values are
            damage = 400;
            knockup = true;
            knockupVelocity = 1;
            knockupMultiplier = 2;
        }

        @Override
        public void onRun() {
            final Location loc = getLocation();

            if (loc.getBlock().getType() != Material.AIR
                    && loc.getBlock().getType().isSolid()) // Was an NMS call for 1.8 Spigot, this may not be as accurate
            {
                loc.add(0, 1, 0);
            }
            if (Util.transparentBlocks.contains(loc.clone().subtract(0, 1, 0).getBlock().getType())) {
                loc.add(0, -1, 0);
            }
            final Location loc1 = loc.clone().add(randomDouble(-1.5, 1.5), randomDouble(0, .5) - 0.75, randomDouble(-1.5, 1.5));
            final Location loc2 = loc.clone().add(randomDouble(-1.5, 1.5), randomDouble(1.3, 1.8) - 0.75, randomDouble(-1.5, 1.5));
            for (int i = 0; i < 5; i++) {
                display(explosionParticle, loc1, 0, 1);
                display(waterDripParticle, loc2, 0, 2);
            }
            for (int a = 0; a < 100; a++) {
                display(redDustParticle, loc.clone().add(randomDouble(-1.5, 1.5), randomDouble(1, 1.6) - 0.75, randomDouble(-1.5, 1.5)), redDustColor);
            }

            // If no caster or player are set, the effect will still play, because why not. Still won't move anywhere with 0 config, but minor issues
            if (caster != null && casterPlayer != null) {
                final Collection<Entity> nearbyEntities = loc.getWorld().getNearbyEntities(loc1, 2, 2, 2);

                // Move back to main thread to do damage
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    for (final Entity ent : nearbyEntities) {
                        if (ent instanceof LivingEntity && ent != casterPlayer && !hitTargets.contains(ent)) {
                            final LivingEntity lEnt = (LivingEntity) ent;
                            if (!damageCheck(casterPlayer, lEnt)) {
                                continue;
                            }
                            addSpellTarget(lEnt, caster);
                            damageEntity(lEnt, casterPlayer, damage);

                            if (knockup) {
                                lEnt.setVelocity(new Vector(0, knockupVelocity, 0).add(addVector.clone().multiply(knockupMultiplier)));
                                lEnt.setFallDistance(-512);
                            }

                            hitTargets.add(lEnt);
                        }
                    }
                }, 1);
            }

            loc.add(addVector);
        }
    }

}
