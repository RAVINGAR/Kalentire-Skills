package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.Effect;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.EffectType;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SkillTsunami extends ActiveSkill {

    public SkillTsunami(Heroes plugin) {
        super(plugin, "Tsunami");
        setDescription("You launch a Tsunami forward for $1 seconds, dealing $2 damage to $3enemies it hits.");
        setUsage("/skill tsunami");
        setArgumentRange(0, 0);
        setIdentifiers("skill tsunami");
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_WATER, SkillType.AGGRESSIVE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

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
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_WISDOM, 1, false);
        duration += durationIncrease * hero.getAttributeValue(AttributeType.WISDOM);
        String formattedDuration = Util.decFormat.format(duration / 20);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 200.0, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        boolean knockup = SkillConfigManager.getUseSetting(hero, this, "knockup", true);

        return getDescription().replace("$1", formattedDuration).replace("$2", damage + "").replace("$3", knockup ? "and knocking up " : "");
    }

    @Override
    public SkillResult use(final Hero hero, String[] strings) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        // Get configs
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_WISDOM, 1, false);
        duration += durationIncrease * hero.getAttributeValue(AttributeType.WISDOM);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 200.0, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        boolean knockup = SkillConfigManager.getUseSetting(hero, this, "knockup", true);
        double knockupVelocity = SkillConfigManager.getUseSetting(hero, this, "knockup-velocity", 1.0, false);
        double knockupMultiplier = SkillConfigManager.getUseSetting(hero, this, "knockup-multiplier", 2.0, false);

        // Set vector for movement of the effect
        final Vector v = player.getLocation().getDirection().normalize().multiply(0.3);
        v.setY(0);
        final Location loc = player.getLocation().subtract(0, 1, 0).add(v);

        // Initialize the Effect Manager
        EffectManager em = new EffectManager(plugin);

        // Initialize and configure the Effect
        TsunamiEffect te = new TsunamiEffect(em);
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
        te.start();
        em.disposeOnTermination();
        
        return SkillResult.NORMAL;
    }

    private class TsunamiEffect extends Effect {

        // Effect settings
        public ParticleEffect explosionParticle = ParticleEffect.EXPLOSION_NORMAL;
        public ParticleEffect waterDripParticle = ParticleEffect.DRIP_WATER;
        public ParticleEffect redDustParticle = ParticleEffect.REDSTONE;
        public Color redDustColor = null;

        public Vector addVector = new Vector(0, 0, 0);

        // Skill damage settings
        private List<LivingEntity> hitTargets;
        public Hero caster;
        public Player casterPlayer;
        public double damage;
        public boolean knockup;
        public double knockupVelocity;
        public double knockupMultiplier;

        public TsunamiEffect(EffectManager manager) {
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
            Location loc = getLocation();

            if (loc.getBlock().getType() != Material.AIR
                    && loc.getBlock().getType().isSolid()) // Was an NMS call for 1.8 Spigot, this may not be as accurate
                loc.add(0, 1, 0);
            if (Util.transparentBlocks.contains(loc.clone().subtract(0, 1, 0).getBlock().getType()))
                loc.add(0, -1, 0);
            Location loc1 = loc.clone().add(randomDouble(-1.5, 1.5), randomDouble(0, .5) - 0.75, randomDouble(-1.5, 1.5));
            Location loc2 = loc.clone().add(randomDouble(-1.5, 1.5), randomDouble(1.3, 1.8) - 0.75, randomDouble(-1.5, 1.5));
            for (int i = 0; i < 5; i++) {
                display(explosionParticle, loc1, 0, 1);
                display(waterDripParticle, loc2, 0, 2);
            }
            for (int a = 0; a < 100; a++)
                display(redDustParticle, loc.clone().add(randomDouble(-1.5, 1.5), randomDouble(1, 1.6) - 0.75, randomDouble(-1.5, 1.5)), redDustColor);

            // If no caster or player are set, the effect will still play, because why not. Still won't move anywhere with 0 config, but minor issues
            if(caster != null && casterPlayer != null) {
                final Collection<Entity> nearbyEntities = loc.getWorld().getNearbyEntities(loc1, 1, 1, 1);

                // Move back to main thread to do damage
                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override
                    public void run() {
                        for (Entity ent : nearbyEntities) {
                            if (ent instanceof LivingEntity && ent != casterPlayer && !hitTargets.contains(ent)) {
                                LivingEntity lEnt = (LivingEntity) ent;

                                if (!damageCheck(casterPlayer, lEnt)) {
                                    continue;
                                }

                                addSpellTarget(lEnt, caster);
                                damageEntity(lEnt, casterPlayer, damage);

                                if(knockup) {
                                    lEnt.setVelocity(new Vector(0, knockupVelocity, 0).add(addVector.clone().multiply(knockupMultiplier)));
                                    lEnt.setFallDistance(-512);
                                }

                                hitTargets.add(lEnt);
                            }
                        }
                    }
                }, 1);
            }

            loc.add(addVector);
        }
    }

    public static double randomDouble(double min, double max) {
        return Math.random() < 0.5 ? ((1 - Math.random()) * (max - min) + min) : (Math.random() * (max - min) + min);
    }

}
