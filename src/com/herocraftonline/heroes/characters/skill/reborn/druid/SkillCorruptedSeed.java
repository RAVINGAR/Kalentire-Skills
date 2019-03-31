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
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;

public class SkillCorruptedSeed extends TargettedSkill {

    private String toggleableEffectName = "SeedExplode";
    private String applyText;
    private String expireText;

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
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% has planted a corrupted seed!").replace("%hero%", "$1");
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
        long silenceDuration = SkillConfigManager.getUseSetting(hero, this, "silence-duation", 3000, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(healthPerSecond));
    }


    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has planted a corrupted seed!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "The corrupted seed has exploded silencing nearby enemies!");
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
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName());
            this.radius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS, 3, false);
            this.silenceDuration = SkillConfigManager.getUseSetting(hero, skill, "silence-duration", 3000, false);
        }

        @Override
        public void tickMonster(Monster monster) {

        }

        @Override
        public void tickHero(Hero hero) {
            Player player = hero.getPlayer();
            double newHealth = player.getHealth() - healthDrainTick;
            player.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, player.getLocation(), 25, 0.5, 1, 0.5, 0.1);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_HURT, 10.0F, 16);
            if (newHealth < 1)
                hero.removeEffect(this);

            else
                player.setHealth(newHealth);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();

            particleEffect(player);

            List<Entity> targets = hero.getPlayer().getNearbyEntities(radius, radius, radius);
            for (Entity entity : targets) {
                if (!(entity instanceof LivingEntity) || !damageCheck(player, (LivingEntity) entity))
                    continue;

                LivingEntity target = (LivingEntity) entity;
                CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
                targetCT.addEffect(new SilenceEffect(skill, applier, silenceDuration));
            }
        }

        private void particleEffect(LivingEntity target) {
            EffectManager em = new EffectManager(plugin);
            Effect visualEffect = new Effect(em) {
                Particle particle = Particle.DRAGON_BREATH;

                @Override
                public void onRun() {
                    for (double z = -radius; z <= radius; z += 0.33) {
                        for (double x = -radius; x <= radius; x += 0.33) {
                            if (x * x + z * z <= radius * radius) {
                                display(particle, getLocation().clone().add(x, 0, z));
                            }
                        }
                    }
                }
            };

            int silenceDurationTicks = (int)silenceDuration / 50;
            visualEffect.type = de.slikey.effectlib.EffectType.REPEATING;
            visualEffect.period = 10;
            visualEffect.iterations = silenceDurationTicks / visualEffect.period;

            Location location = target.getLocation().clone();
            visualEffect.asynchronous = true;
            visualEffect.setLocation(location);

            visualEffect.start();
            em.disposeOnTermination();

            target.getWorld().playSound(location, Sound.ENTITY_BLAZE_DEATH, 0.15f, 0.0001f);
        }
    }
}

