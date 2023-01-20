package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedLocationSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.libs.slikey.effectlib.effect.HelixEffect;
import com.herocraftonline.heroes.libs.slikey.effectlib.util.DynamicLocation;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Collection;

public class SkillGravityFlux extends TargettedLocationSkill {

    public SkillGravityFlux(final Heroes plugin) {
        super(plugin, "GravityFlux");
        setDescription("Warp the space in a $1 block radius around a target location, reversing gravity for all of those that are nearby. "
                + "Lasts for $2 seconds. Affects both allies and enemies. Use with caution!");
        setArgumentRange(0, 0);
        setUsage("/skill gravityflux");
        setIdentifiers("skill gravityflux");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.MULTI_GRESSIVE, SkillType.NO_SELF_TARGETTING, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(final Hero hero) {
        final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);
        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(radius))
                .replace("$2", Util.decFormat.format(duration / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 14.0);
        config.set(ALLOW_TARGET_AIR_BLOCK_NODE, true);
        config.set(TRY_GET_SOLID_BELOW_BLOCK_NODE, false);
        config.set(MAXIMUM_FIND_SOLID_BELOW_BLOCK_HEIGHT_NODE, 0.0);
        config.set(SkillSetting.RADIUS.node(), 6.0);
        config.set(SkillSetting.DURATION.node(), 7000);
        config.set("sound-period", 2000);
        config.set("levitation-amplifier", 0);
        return config;
    }

    @Override
    public SkillResult use(final Hero hero, final Location targetLoc, final String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5.0, false);
        final int amplifier = SkillConfigManager.getUseSetting(hero, this, "levitation-amplifier", 0, false);
        final long soundPeriod = SkillConfigManager.getUseSetting(hero, this, "sound-period", 2000, false);

        final Collection<Entity> nearbyEnts = targetLoc.getWorld().getNearbyEntities(targetLoc, radius, radius, radius);
        for (final Entity ent : nearbyEnts) {
            if (!(ent instanceof LivingEntity)) {
                continue;
            }

            final LivingEntity target = (LivingEntity) ent;
            if (!hero.isAlliedTo(target) && !damageCheck(player, target)) {
                continue;
            }

            final CharacterTemplate ctTarget = plugin.getCharacterManager().getCharacter(target);
            if (ctTarget == null) {
                continue;
            }

            ctTarget.addEffect(new HaultGravityEffect(this, player, soundPeriod, duration, amplifier));
        }

        final FireworkEffect firework = FireworkEffect.builder()
                .flicker(false)
                .trail(false)
                .withColor(Color.PURPLE)
                .withColor(Color.PURPLE)
                .withColor(Color.BLACK)
                .withFade(Color.BLACK)
                .with(FireworkEffect.Type.BURST)
                .build();
        VisualEffect.playInstantFirework(firework, targetLoc);

        return SkillResult.NORMAL;
    }

    public static class HaultGravityEffect extends PeriodicExpirableEffect {

        HaultGravityEffect(final Skill skill, final Player applier, final long period, final long duration, final int amplifier) {
            super(skill, "HaultedGravity", applier, period, duration, null, null);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.HARMFUL);
            types.add(EffectType.MAGIC);

            addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, (int) (duration / 50), amplifier));
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);

            applyVisuals(hero.getPlayer());
        }

        @Override
        public void applyToMonster(final Monster monster) {
            super.applyToMonster(monster);

            applyVisuals(monster.getEntity());
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

        private void applyVisuals(final LivingEntity target) {
            final World world = target.getWorld();
            final Location loc = target.getLocation();
            final int durationTicks = (int) this.getDuration() / 50;

            final HelixEffect visualEffect = new HelixEffect(effectLib);

            final DynamicLocation dynamicLoc = new DynamicLocation(target);
            dynamicLoc.addOffset(new Vector(0, -target.getEyeHeight(), 0));
            visualEffect.setDynamicOrigin(dynamicLoc);
            visualEffect.disappearWithOriginEntity = true;

            visualEffect.radius = 1.5F;
            visualEffect.iterations = durationTicks / visualEffect.period;

            visualEffect.color = Color.PURPLE;
            visualEffect.particle = Particle.REDSTONE;
            visualEffect.particles = 5;

            effectLib.start(visualEffect);
        }
    }
}
