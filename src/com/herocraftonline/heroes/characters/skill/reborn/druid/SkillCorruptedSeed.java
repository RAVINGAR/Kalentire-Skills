package com.herocraftonline.heroes.characters.skill.reborn.druid;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.SilenceEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.Effect;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.SphereEffect;
import de.slikey.effectlib.util.DynamicLocation;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SkillCorruptedSeed extends TargettedSkill {

    private String toggleableEffectName = "SeedExplode";
    private String applyText;
    private String expireText;
    private static Color VOID_PURPLE = Color.fromRGB(75, 0 , 130);
    private static Color FEL_GREEN = Color.fromRGB(19, 255 , 41);
    public SkillCorruptedSeed(Heroes plugin) {
        super(plugin, "CorruptedSeed");
        setDescription("Plant a corrupted seed in yourself or an ally, recasting this ability will explode the seed silencing nearby enemies. While the user is holding a seed they will take periodic damage");
        setUsage("/skill corruptedseed");
        setArgumentRange(0, 0);
        setIdentifiers("skill corruptedseed");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.SILENCEABLE, SkillType.BUFFING);
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% has planted a corrupted seed on %target%!").replace("%hero%", "$2").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%target%`s corrupted seed has exploded!").replace("%target%", "$1");
        setUseText(null);
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] strings) {
        Player player = hero.getPlayer();

        if (!(target instanceof Player)) {
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);

        if (targetHero.equals(hero) || targetHero.getParty().isPartyMember(hero)) {
            double healthDrainTick = SkillConfigManager.getUseSetting(hero, this, "health-drain=tick", 20.0D, false);
            int healthDrainPeriod = SkillConfigManager.getUseSetting(hero, this, "health-drain-period", 500, false);
            long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
            targetHero.addEffect(new CorruptedSeedEffect(this, player, healthDrainTick, healthDrainPeriod, duration));

            return SkillResult.NORMAL;
        }
        return SkillResult.INVALID_TARGET_NO_MSG;
    }


    @Override
    public String getDescription(Hero hero) {
        int healthDrainTick = SkillConfigManager.getUseSetting(hero, this, "health-drain-tick", 20, false);
        int healthDrainPeriod = SkillConfigManager.getUseSetting(hero, this, "health-drain-period", 500, false);
        double perSecondMultiplier = 1000d / healthDrainPeriod;
        double healthPerSecond = healthDrainTick * perSecondMultiplier;
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 3, false);
        long silenceDuration = SkillConfigManager.getUseSetting(hero, this, "silence-duration", 3000, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(healthPerSecond));
    }


    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has planted a corrupted seed on %target%!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target%`s corrupted seed has exploded!");
        config.set("health-drain-tick", 20.0D);
        config.set("health-drain-period", 500);
        config.set(SkillSetting.DURATION.node(), 5000);
        config.set(SkillSetting.RADIUS.node(), 3);
        config.set("silence-duration", 3000);
        return config;
    }

    private class CorruptedSeedEffect extends PeriodicExpirableEffect {

        private final double healthDrainTick;
        private double radius;
        private long silenceDuration;
        private EffectManager effectManager;
        private SphereEffect visualEffect;

        public CorruptedSeedEffect(Skill skill, Player applier, double healthDrainTick, long period, long duration) {
            super(skill, toggleableEffectName, applier, period, duration, applyText, expireText);

            this.healthDrainTick = healthDrainTick;
            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.DARK);
            types.add(EffectType.HARMFUL);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            this.radius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS, 3, false);
            this.silenceDuration = SkillConfigManager.getUseSetting(hero, skill, "silence-duration", 3000, false);
            applyBaseSeedVisuals(hero.getPlayer());
            applyFlameSeedVisuals(hero.getPlayer());
        }

        @Override
        public void tickMonster(Monster monster) {

        }

        @Override
        public void tickHero(Hero hero) {
            Player player = hero.getPlayer();
            double newHealth = player.getHealth() - healthDrainTick;
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_HURT, 1, 1);
            if (newHealth < 1)
                hero.removeEffect(this);

            else
                player.setHealth(newHealth);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();

            particleExplodeEffect(player, 1750, VOID_PURPLE);

            List<Entity> targets = hero.getPlayer().getNearbyEntities(radius, radius, radius);
            for (Entity entity : targets) {
                if (!(entity instanceof LivingEntity) || !damageCheck(player, (LivingEntity) entity))
                    continue;

                LivingEntity target = (LivingEntity) entity;
                CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
                targetCT.addEffect(new SilenceEffect(skill, applier, silenceDuration));
            }
            if (this.effectManager != null) {
                this.effectManager.dispose();
        }

        }

        @NotNull
        private void particleExplodeEffect(LivingEntity target, int explosionDuration, Color color) {
            EffectManager em = new EffectManager(plugin);
            SphereEffect visualEffect = new SphereEffect(em);

            DynamicLocation dynamicLoc = new DynamicLocation(target);
            visualEffect.setDynamicOrigin(dynamicLoc);
            visualEffect.disappearWithOriginEntity = true;

            final int delayTicks = explosionDuration / 50;
            final int displayPeriod = 3;
            visualEffect.radius = 0.5;
            visualEffect.radiusIncrease = +1.0;
            visualEffect.period = displayPeriod;
            visualEffect.iterations = delayTicks / displayPeriod;
            visualEffect.color = color;


            visualEffect.particle = Particle.REDSTONE;
            visualEffect.particleCount = 5;
            em.start(visualEffect);
            em.disposeOnTermination();

        }


        private void applyBaseSeedVisuals(LivingEntity target) {
            final World world = target.getWorld();
            final Location loc = target.getLocation();
            final int durationTicks = (int) this.getDuration() / 50;
            final int displayPeriod = 2;

            this.effectManager = new EffectManager(plugin);
            this.visualEffect = new SphereEffect(effectManager);

            DynamicLocation dynamicLoc = new DynamicLocation(target);
            visualEffect.setDynamicOrigin(dynamicLoc);
            visualEffect.disappearWithOriginEntity = true;

            visualEffect.radius = 0.3F;
            visualEffect.color = VOID_PURPLE;
            visualEffect.particle = Particle.REDSTONE;
            visualEffect.particles = 25;
            dynamicLoc.addOffset(new Vector(0, 0.8, 0));

            visualEffect.period = displayPeriod;
            visualEffect.iterations = durationTicks / displayPeriod;

            effectManager.start(visualEffect);
            effectManager.disposeOnTermination();
        }

        private void applyFlameSeedVisuals(LivingEntity target) {
            final World world = target.getWorld();
            final Location loc = target.getLocation();
            final int durationTicks = (int) this.getDuration() / 50;
            final int displayPeriod = 2;

            this.effectManager = new EffectManager(plugin);
            this.visualEffect = new SphereEffect(effectManager);

            DynamicLocation dynamicLoc = new DynamicLocation(target);
            visualEffect.setDynamicOrigin(dynamicLoc);
            visualEffect.disappearWithOriginEntity = true;

            visualEffect.radius = 0.4F;
            visualEffect.color = FEL_GREEN;
            visualEffect.particle = Particle.REDSTONE;
            visualEffect.particles = 35;
            dynamicLoc.addOffset(new Vector(0, 0.8, 0));

            visualEffect.period = displayPeriod;
            visualEffect.iterations = durationTicks / displayPeriod;

            effectManager.start(visualEffect);
            effectManager.disposeOnTermination();
        }




    }
}

