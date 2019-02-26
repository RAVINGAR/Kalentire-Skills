package com.herocraftonline.heroes.characters.skill.reborn.chronomancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.*;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.common.WalkSpeedDecreaseEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.ShieldEffect;
import de.slikey.effectlib.effect.SphereEffect;
import de.slikey.effectlib.util.DynamicLocation;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.ArrayList;
import java.util.List;

public class SkillTemporalWard extends TargettedSkill {

    public SkillTemporalWard(Heroes plugin) {
        super(plugin, "TemporalWard");
        setDescription("Project your target from the burdens of time for $1 seconds.");
        setArgumentRange(0, 0);
        setUsage("/skill temporalward");
        setIdentifiers("skill temporalward");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.MULTI_GRESSIVE, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$2", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DURATION.node(), 5000);
        config.set(SkillSetting.USE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has protected %target% from the effects of time!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is once again susceptible to time.");
        return config;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);

        CharacterTemplate ctTarget = plugin.getCharacterManager().getCharacter(target);
        //addSpellTarget(target, hero); // dont think I need to do this for this skill
        ctTarget.addEffect(new TemporalWardEffect(this, player, duration));

        broadcastExecuteText(hero, target);

        return SkillResult.NORMAL;
    }

    // Periodic effect is simply for sound / visuals, static so that ShatterTime can use it.
    public static class TemporalWardEffect extends PeriodicExpirableEffect {

        TemporalWardEffect(Skill skill, Player applier, long duration) {
            super(skill, "TemporallyWarded", applier, 1500, duration, null, "$1 is once again susceptible to time.");

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.MAGIC);
            types.add(EffectType.BENEFICIAL);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            removeTimeBasedEffects(hero);
            applyVisuals(hero.getPlayer());
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);

            removeTimeBasedEffects(monster);
            applyVisuals(monster.getEntity());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
        }

        @Override
        public void tickMonster(Monster monster) {
            LivingEntity ent = monster.getEntity();
            ent.getWorld().playSound(ent.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 1.4F, 2F);
        }

        @Override
        public void tickHero(Hero hero) {
            Player player = hero.getPlayer();
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 1.4F, 2F);
        }

        private void removeTimeBasedEffects(CharacterTemplate ctTarget) {
            List<ExpirableEffect> effectsToRemove = new ArrayList<ExpirableEffect>();
            for (Effect effect : ctTarget.getEffects()) {
                if (!(effect instanceof ExpirableEffect))
                    continue;
                if (effect == this)
                    continue;
                if (!effect.getName().contains("Time") || effect.getName().equals("DecelerationField") || effect.getName().equals("AccelerationField")) // TODO: Add "And has EffecType(Temporal) later.
                    continue;

                effectsToRemove.add((ExpirableEffect) effect);
            }

            // Separate loop for concurrent list modification reasons.
            for (ExpirableEffect exEffect : effectsToRemove) {
                ctTarget.removeEffect(exEffect);
            }
        }

        private void applyVisuals(LivingEntity target) {
            final World world = target.getWorld();
            final Location loc = target.getLocation();
            final int delayTicks = (int) this.getDuration() / 50;
            final int displayPeriod = 3;

            EffectManager em = new EffectManager(plugin);
            SphereEffect visualEffect = new SphereEffect(em);

            DynamicLocation dynamicLoc = new DynamicLocation(target);
            visualEffect.setDynamicOrigin(dynamicLoc);
            visualEffect.disappearWithOriginEntity = true;

            visualEffect.radius = 2;
            visualEffect.period = displayPeriod;
            visualEffect.iterations = delayTicks / displayPeriod;

            visualEffect.color = Color.fromBGR(255, 192, 203);
            visualEffect.particle = Particle.REDSTONE;
            visualEffect.particleCount = 3;

            em.start(visualEffect);
            em.disposeOnTermination();
        }
    }
}
