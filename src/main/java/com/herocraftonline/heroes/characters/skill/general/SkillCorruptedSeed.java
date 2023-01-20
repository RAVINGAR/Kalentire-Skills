package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.SilenceEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.libs.slikey.effectlib.effect.SphereEffect;
import com.herocraftonline.heroes.libs.slikey.effectlib.util.DynamicLocation;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.List;

public class SkillCorruptedSeed extends TargettedSkill {

    private static final Color VOID_PURPLE = Color.fromRGB(75, 0, 130);
    private static final Color FEL_GREEN = Color.fromRGB(19, 255, 41);
    private final String toggleableEffectName = "CorruptedSeedEffect";
    private String applyText;
    private String expireText;

    public SkillCorruptedSeed(final Heroes plugin) {
        super(plugin, "CorruptedSeed");
        setDescription("Plant a corrupted seed in yourself or an ally which lasts $1 second(s). Recasting this ability or letting the effect expire will explode the seed, dealing $2 damage and silencing nearby enemies for $3 second(s). Those who hold the seed will be drained of $4 health every half a second, if the holder dies the seed will decay.");
        setUsage("/skill corruptedseed");
        setArgumentRange(0, 0);
        setIdentifiers("skill corruptedseed");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.SILENCEABLE, SkillType.BUFFING);
        setToggleableEffectName(toggleableEffectName);
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% has planted a corrupted seed on %target%!").replace("%hero%", "$2").replace("$hero$", "$2").replace("%target%", "$1").replace("$target$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%target%`s corrupted seed has exploded!").replace("%target%", "$1").replace("$target$", "$1");
        setUseText(null);
    }

    @Override
    public SkillResult use(final Hero hero, final LivingEntity target, final String[] strings) {
        final Player player = hero.getPlayer();

        if (hero.isAlliedTo(target)) {
            final CharacterTemplate targetCharacter = player.equals(target) ? hero : plugin.getCharacterManager().getCharacter(target);

            if (targetCharacter.hasEffect(toggleableEffectName)) {
                final CorruptedSeedEffect effect = (CorruptedSeedEffect) targetCharacter.getEffect(toggleableEffectName);
                if (effect.getApplier().getUniqueId().equals(hero.getEntity().getUniqueId())) {
                    targetCharacter.removeEffect(effect);
                } else {
                    hero.getPlayer().sendMessage(ChatColor.GRAY + "That target already has a corrupted seed!");
                    return SkillResult.INVALID_TARGET_NO_MSG;
                }
            } else {
                final double healthDrainTick = SkillConfigManager.getUseSetting(hero, this, "health-drain-tick", 20.0D, false);
                final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 8000, false);
                final int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 3, false);
                final int silenceDuration = SkillConfigManager.getUseSetting(hero, this, "silence-duration", 3000, false);
                final double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, 20, false);
                targetCharacter.addEffect(new CorruptedSeedEffect(this, hero, healthDrainTick, damage, duration, radius, silenceDuration));
            }

            return SkillResult.NORMAL;
        }
        return SkillResult.INVALID_TARGET_NO_MSG;
    }

    @Override
    public String getDescription(final Hero hero) {
        final int healthDrainTick = SkillConfigManager.getUseSetting(hero, this, "health-per-tick", 20, false);
        final int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, 8000, false);
        final long silenceDuration = SkillConfigManager.getUseSetting(hero, this, "silence-duration", 3000, false);
        final double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, 15, false);

        return getDescription()
                .replace("$1", "" + duration / 1000L)
                .replace("$2", "" + damage)
                .replace("$3", "" + silenceDuration / 1000L)
                .replace("$4", Util.decFormat.format(healthDrainTick));
    }


    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has planted a corrupted seed on %target%!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target%`s corrupted seed has exploded!");
        config.set("health-per-tick", 20.0D);
        config.set(SkillSetting.DAMAGE.node(), 20);
        config.set(SkillSetting.DURATION.node(), 5000);
        config.set(SkillSetting.RADIUS.node(), 3);
        config.set("silence-duration", 3000);
        return config;
    }

    private class CorruptedSeedEffect extends PeriodicExpirableEffect {

        private final double healthDrainTick;
        private final double radius;
        private final long silenceDuration;
        private final double damage;
        private final Hero heroApplier;
        private boolean shouldExplode = true;

        public CorruptedSeedEffect(final Skill skill, final Hero applier, final double healthDrainTick, final double damage, final long duration, final int radius, final int silenceDuration) {
            super(skill, toggleableEffectName, applier.getPlayer(), 500L, duration, applyText, null);
            this.healthDrainTick = healthDrainTick;
            this.heroApplier = applier;
            this.radius = radius;
            this.silenceDuration = silenceDuration;
            this.damage = damage;

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.DARK);
            types.add(EffectType.HARMFUL);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);

            applyBaseSeedVisuals(hero.getPlayer());
            applyFlameSeedVisuals(hero.getPlayer());
        }

        @Override
        public void tickMonster(final Monster monster) {
            tickCharacter(monster);
        }

        @Override
        public void tickHero(final Hero hero) {
            tickCharacter(hero);
        }

        private void tickCharacter(final CharacterTemplate character) {
            final LivingEntity entity = character.getEntity();
            final double newHealth = entity.getHealth() - healthDrainTick;
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_GENERIC_HURT, 0.5F, 10);
            if (newHealth < 1) {
                shouldExplode = false;
                character.removeEffect(this);
            } else {
                entity.setHealth(newHealth);
            }
        }

        @Override
        public void removeFromMonster(final Monster monster) {
            super.removeFromMonster(monster);
            removeFromCharacter(monster);
            if (shouldExplode) {
                this.broadcast(monster.getEntity().getLocation(), expireText, CustomNameManager.getName(monster), this.getApplier().getName());
            }

        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            removeFromCharacter(hero);

            if (shouldExplode) {
                final Player player = hero.getPlayer();
                if (!hero.hasEffectType(EffectType.SILENT_ACTIONS)) {
                    this.broadcast(player.getLocation(), expireText, this.getApplier().getName());
                } else {
                    player.sendMessage(expireText.replace("$1", this.getApplier().getName()));
                }
            }
        }

        private void removeFromCharacter(final CharacterTemplate character) {
            final LivingEntity entity = character.getEntity();


            if (shouldExplode) {
                particleExplodeEffect(entity, VOID_PURPLE);
                particleExplodeGreenEffect(entity, FEL_GREEN);
                final List<Entity> targets = entity.getNearbyEntities(radius, radius, radius);
                for (final Entity e : targets) {
                    if (!(e instanceof LivingEntity)) {
                        continue;
                    }
                    final LivingEntity target = (LivingEntity) e;
                    if (!damageCheck(applier, target)) {
                        continue;
                    }
                    addSpellTarget(target, heroApplier);
                    damageEntity(target, applier, damage, EntityDamageEvent.DamageCause.MAGIC, 0.0f);
                    final CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
                    targetCT.addEffect(new SilenceEffect(skill, applier, silenceDuration));
                }
            } else {
                particleExplodeEffect(entity, Color.GRAY);
            }
        }

        private void particleExplodeEffect(final LivingEntity target, final Color color) {
            final SphereEffect visualEffect = new SphereEffect(effectLib);
            //faster
            final int explosionDuration = 1500;
            final int durationTicks = explosionDuration / 50;
            //more particles
            final int displayPeriod = 2;
            final double baseRadius = 0.5;
            final double perTickModifier = (double) durationTicks / (double) displayPeriod;
            final double radiusGain = (radius + 1 - baseRadius) / perTickModifier;

            final DynamicLocation dynamicLoc = new DynamicLocation(target);
            visualEffect.setDynamicOrigin(dynamicLoc);
            visualEffect.disappearWithOriginEntity = true;

            visualEffect.radius = baseRadius;
            visualEffect.radiusIncrease = radiusGain;
            visualEffect.period = displayPeriod;
            visualEffect.iterations = durationTicks / displayPeriod;
            visualEffect.color = color;

            visualEffect.particle = Particle.REDSTONE;
            visualEffect.particleCount = 2;
            effectLib.start(visualEffect);
        }

        private void particleExplodeGreenEffect(final LivingEntity target, final Color color) {
            final SphereEffect visualEffect = new SphereEffect(effectLib);
            final int explosionDuration = 1500;
            final int durationTicks = explosionDuration / 50;
            final int displayPeriod = 2;
            final double baseRadius = 0.5;
            final double perTickModifier = (double) durationTicks / (double) displayPeriod;
            final double radiusGain = (radius + 1 - baseRadius) / perTickModifier;

            final DynamicLocation dynamicLoc = new DynamicLocation(target);
            visualEffect.setDynamicOrigin(dynamicLoc);
            visualEffect.disappearWithOriginEntity = true;

            visualEffect.radius = baseRadius;
            visualEffect.radiusIncrease = radiusGain;
            visualEffect.period = displayPeriod;
            visualEffect.iterations = durationTicks / displayPeriod;
            visualEffect.color = color;

            visualEffect.particle = Particle.REDSTONE;
            visualEffect.particleCount = 2;
            effectLib.start(visualEffect);
        }

        private void applyBaseSeedVisuals(final LivingEntity target) {
            final int durationTicks = (int) this.getDuration() / 50;
            final int displayPeriod = 2;

            final SphereEffect visualEffect = new SphereEffect(effectLib);

            final DynamicLocation dynamicLoc = new DynamicLocation(target);
            visualEffect.setDynamicOrigin(dynamicLoc);
            visualEffect.disappearWithOriginEntity = true;

            visualEffect.radius = 0.3F;
            visualEffect.color = VOID_PURPLE;
            visualEffect.particle = Particle.REDSTONE;
            visualEffect.particles = 25;
            dynamicLoc.addOffset(new Vector(0, 0.8, 0));

            visualEffect.period = displayPeriod;
            visualEffect.iterations = durationTicks / displayPeriod;

            effectLib.start(visualEffect);
        }

        private void applyFlameSeedVisuals(final LivingEntity target) {
            final int durationTicks = (int) this.getDuration() / 50;
            final int displayPeriod = 2;

            final SphereEffect visualEffect = new SphereEffect(effectLib);
            final DynamicLocation dynamicLoc = new DynamicLocation(target);
            visualEffect.setDynamicOrigin(dynamicLoc);
            visualEffect.disappearWithOriginEntity = true;

            visualEffect.radius = 0.4F;
            visualEffect.color = FEL_GREEN;
            visualEffect.particle = Particle.REDSTONE;
            visualEffect.particles = 35;
            dynamicLoc.addOffset(new Vector(0, 0.8, 0));

            visualEffect.period = displayPeriod;
            visualEffect.iterations = durationTicks / displayPeriod;

            effectLib.start(visualEffect);
        }
    }
}

