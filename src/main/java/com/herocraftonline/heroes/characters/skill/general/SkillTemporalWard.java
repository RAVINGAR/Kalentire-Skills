package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.libs.slikey.effectlib.effect.SphereEffect;
import com.herocraftonline.heroes.libs.slikey.effectlib.util.DynamicLocation;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SkillTemporalWard extends TargettedSkill {

    public SkillTemporalWard(final Heroes plugin) {
        super(plugin, "TemporalWard");
        setDescription("Project your target from the burdens of time for $1 second(s).");
        setArgumentRange(0, 0);
        setUsage("/skill temporalward");
        setIdentifiers("skill temporalward");
        setTypes(SkillType.ABILITY_PROPERTY_TEMPORAL, SkillType.MULTI_GRESSIVE, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(final Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);

        final String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$2", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DURATION.node(), 5000);
        config.set(SkillSetting.USE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has protected %target% from the effects of time!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is once again susceptible to time.");
        return config;
    }

    @Override
    public SkillResult use(final Hero hero, final LivingEntity target, final String[] args) {
        final Player player = hero.getPlayer();

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);

        final CharacterTemplate ctTarget = plugin.getCharacterManager().getCharacter(target);
        //addSpellTarget(target, hero); // dont think I need to do this for this skill
        ctTarget.addEffect(new TemporalWardEffect(this, player, duration));

        broadcastExecuteText(hero, target);

        return SkillResult.NORMAL;
    }

    // Periodic effect is simply for sound / visuals, static so that ShatterTime can use it.
    public static class TemporalWardEffect extends PeriodicExpirableEffect {

        TemporalWardEffect(final Skill skill, final Player applier, final long duration) {
            super(skill, "TemporallyWarded", applier, 1500, duration, null, "$1 is once again susceptible to time.");

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.MAGIC);
            types.add(EffectType.BENEFICIAL);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);

            removeTimeBasedEffects(hero);
            applyVisuals(hero.getPlayer());
        }

        @Override
        public void applyToMonster(final Monster monster) {
            super.applyToMonster(monster);

            removeTimeBasedEffects(monster);
            applyVisuals(monster.getEntity());
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
        }

        @Override
        public void tickMonster(final Monster monster) {
            final LivingEntity ent = monster.getEntity();
            ent.getWorld().playSound(ent.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 1.4F, 2F);
        }

        @Override
        public void tickHero(final Hero hero) {
            final Player player = hero.getPlayer();
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 1.4F, 2F);
        }

        private void removeTimeBasedEffects(final CharacterTemplate ctTarget) {
            final List<ExpirableEffect> effectsToRemove = new ArrayList<>();
            for (final Effect effect : ctTarget.getEffects()) {
                if (!(effect instanceof ExpirableEffect)) {
                    continue;
                }
                if (effect == this) {
                    continue;
                }
                if (!effect.getName().contains("Time") || effect.getName().equals("DecelerationField") || effect.getName().equals("AccelerationField")) // TODO: Add "And has EffecType(Temporal) later.
                {
                    continue;
                }

                effectsToRemove.add((ExpirableEffect) effect);
            }

            // Separate loop for concurrent list modification reasons.
            for (final ExpirableEffect exEffect : effectsToRemove) {
                ctTarget.removeEffect(exEffect);
            }
        }

        private void applyVisuals(final LivingEntity target) {
            final World world = target.getWorld();
            final Location loc = target.getLocation();
            final int durationTicks = (int) this.getDuration() / 50;
            final int displayPeriod = 3;

            final SphereEffect visualEffect = new SphereEffect(effectLib);

            final DynamicLocation dynamicLoc = new DynamicLocation(target);
            visualEffect.setDynamicOrigin(dynamicLoc);
            visualEffect.disappearWithOriginEntity = true;

            visualEffect.radius = 2;
            visualEffect.period = displayPeriod;
            visualEffect.iterations = durationTicks / displayPeriod;

            visualEffect.color = Color.fromBGR(255, 192, 203);
            visualEffect.particle = Particle.REDSTONE;
            visualEffect.particleCount = 3;

            effectLib.start(visualEffect);
        }
    }
}
