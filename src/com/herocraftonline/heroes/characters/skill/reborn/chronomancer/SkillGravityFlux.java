package com.herocraftonline.heroes.characters.skill.reborn.chronomancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.nms.physics.NMSPhysics;
import com.herocraftonline.heroes.nms.physics.RayCastHit;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.HelixEffect;
import de.slikey.effectlib.effect.SphereEffect;
import de.slikey.effectlib.util.DynamicLocation;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SkillGravityFlux extends TargettedLocationSkill {

    public SkillGravityFlux(Heroes plugin) {
        super(plugin, "GravityFlux");
        setDescription("Warp the space in a $1 block radius around a target location, reversing gravity for all of those that are nearby. "
                + "Lasts for $2 seconds. Affects both allies and enemies. Use with caution!");
        setArgumentRange(0, 0);
        setUsage("/skill gravityflux");
        setIdentifiers("skill gravityflux");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.MULTI_GRESSIVE, SkillType.NO_SELF_TARGETTING, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        double radius = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.RADIUS, false);
        long duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(radius))
                .replace("$2", Util.decFormat.format(duration / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
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
    public SkillResult use(Hero hero, Location targetLoc, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        long duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        double radius = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.RADIUS, false);
        int amplifier = SkillConfigManager.getUseSetting(hero, this, "levitation-amplifier", 0, false);
        long soundPeriod = SkillConfigManager.getUseSetting(hero, this, "sound-period", 2000, false);

        Collection<Entity> nearbyEnts = targetLoc.getWorld().getNearbyEntities(targetLoc, radius, radius, radius);
        for (Entity ent : nearbyEnts) {
            if (!(ent instanceof LivingEntity))
                continue;

            LivingEntity target = (LivingEntity) ent;
            if (!hero.isAlliedTo(target) && !damageCheck(player, target))
                continue;

            CharacterTemplate ctTarget = plugin.getCharacterManager().getCharacter(target);
            if (ctTarget == null)
                continue;

            ctTarget.addEffect(new HaultGravityEffect(this, player, soundPeriod, duration, amplifier));
        }

        FireworkEffect firework = FireworkEffect.builder()
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

    public class HaultGravityEffect extends PeriodicExpirableEffect {

        private EffectManager effectManager;

        HaultGravityEffect(Skill skill, Player applier, long period, long duration, int amplifier) {
            super(skill, "HaultedGravity", applier, period, duration, null, null);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.HARMFUL);
            types.add(EffectType.MAGIC);

            addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, (int) (duration / 50), amplifier));
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();

            player.setFallDistance(0);
            applyVisuals(player);
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);

            LivingEntity monsterLE = monster.getEntity();
            monsterLE.setFallDistance(0);
            applyVisuals(monsterLE);
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);

            if (this.effectManager != null)
                this.effectManager.dispose();
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            if (this.effectManager != null)
                this.effectManager.dispose();
        }

        @Override
        public void tickMonster(Monster monster) {
            LivingEntity ent = monster.getEntity();
//            ent.getWorld().playSound(ent.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 1.4F, 2F);
        }

        @Override
        public void tickHero(Hero hero) {
            Player player = hero.getPlayer();
//            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 1.4F, 2F);
        }

        private void applyVisuals(LivingEntity target) {
            final World world = target.getWorld();
            final Location loc = target.getLocation();
            final int durationTicks = (int) this.getDuration() / 50;

            if (this.effectManager != null)
                this.effectManager.dispose();

            this.effectManager = new EffectManager(plugin);
            HelixEffect visualEffect = new HelixEffect(effectManager);

            DynamicLocation dynamicLoc = new DynamicLocation(target);
            dynamicLoc.addOffset(new Vector(0, -target.getEyeHeight(), 0));
            visualEffect.setDynamicOrigin(dynamicLoc);
            visualEffect.disappearWithOriginEntity = true;

            visualEffect.radius = 1.5F;
            visualEffect.iterations = durationTicks / visualEffect.period;

            visualEffect.color = Color.PURPLE;
            visualEffect.particle = Particle.REDSTONE;
            visualEffect.particles = 5;

            effectManager.start(visualEffect);
            effectManager.disposeOnTermination();
        }
    }
}
