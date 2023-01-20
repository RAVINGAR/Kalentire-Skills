package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainManaEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.libs.slikey.effectlib.effect.CylinderEffect;
import com.herocraftonline.heroes.libs.slikey.effectlib.util.DynamicLocation;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;

public class SkillEvocation extends ActiveSkill {
    private static final Color MANA_BLUE = Color.fromRGB(0, 191, 255);
    private final String effectName = "Evocating";
    private String applyText;
    private String expireText;

    public SkillEvocation(final Heroes plugin) {
        super(plugin, "Evocation");
        setDescription("Channeling: Increases your mana regeneration by $1% for up to $2 second(s). " +
                "While channeling you will knock nearby enemies away from you as you regain mana. " +
                "Channeling abilities are a constant cast: you will be slowed and be unable to cast other abilities while it is active.");
        setUsage("/skill evocation");
        setArgumentRange(0, 0);
        setIdentifiers("skill evocation");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.BUFFING, SkillType.MANA_INCREASING);
    }

    @Override
    public String getDescription(final Hero hero) {
        final double regenMultiplier = SkillConfigManager.getUseSetting(hero, this, "regen-multiplier", 4.0, false);
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DELAY, 4000, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(regenMultiplier * 100))
                .replace("$2", Util.decFormat.format((double) duration / 1000));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set("regen-multiplier", 4.0);
        config.set(SkillSetting.RADIUS.node(), 3.0);
        config.set(SkillSetting.PERIOD.node(), 1000);
        config.set(SkillSetting.DELAY.node(), 4000);
        config.set("horizontal-power", 0.75);
        config.set("vertical-power", 0.3);
        config.set(SkillSetting.INTERRUPT_TEXT.node(), "");
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is evocating mana!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is no longer evocating.");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is evocating mana!").replace("%hero%", "$1").replace("$hero$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is no longer evocating.").replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public void onWarmup(final Hero hero) {
        super.onWarmup(hero);
        final Player player = hero.getPlayer();

        hero.addEffect(new ChannelingEffect(this, player));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.0F);
        player.getWorld().spawnParticle(Particle.WATER_SPLASH, player.getLocation(), 65, 0, 0.9, 0, 0.1);
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        hero.removeEffect(hero.getEffect(effectName));
        return SkillResult.NORMAL;
    }

    private class ChannelingEffect extends PeriodicEffect {
        public ChannelingEffect(final Skill skill, final Player applier) {
            super(skill, "Channeling-" + skill.getName(), applier, 100, null, null);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);

            final int regainPeriod = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.PERIOD, 1000, false);
            hero.addEffect(new EvocationEffect(skill, applier, regainPeriod));
        }

        @Override
        public void tickHero(final Hero hero) {
            super.tickHero(hero);

            if (hero.getDelayedSkill() != null && hero.getDelayedSkill().getSkill().equals(skill)) {
                return;
            }

            // We were interrupted or finished casting.
            hero.removeEffect(this);
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);

            final long cooldownMillis = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.COOLDOWN, 1000, false);
            final long currentTime = System.currentTimeMillis();

            final Long currentCd = hero.getCooldown(skill.getName());
            final long defaultCd = currentTime + cooldownMillis;
            if (currentCd == null || currentCd < defaultCd) {
                hero.setCooldown(skill.getName(), System.currentTimeMillis() + cooldownMillis);
            }

            hero.removeEffect(hero.getEffect(effectName));
        }
    }

    private class EvocationEffect extends PeriodicEffect {
        private CylinderEffect visualEffect;

        private double regenMultiplier;
        private double perSecondModifier;
        private double hPower;
        private double vPower;
        private double radius;

        public EvocationEffect(final Skill skill, final Player applier, final long regainPeriod) {
            super(skill, effectName, applier, regainPeriod, applyText, expireText);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);

            this.regenMultiplier = SkillConfigManager.getUseSetting(hero, skill, "regen-multiplier", 4.0, false);
            this.perSecondModifier = (1000.0 / getPeriod());
            this.hPower = SkillConfigManager.getUseSetting(hero, skill, "horizontal-power", 0.75, false);
            this.vPower = SkillConfigManager.getUseSetting(hero, skill, "vertical-power", 0.3, false);
            this.radius = SkillConfigManager.getScaledUseSettingDouble(hero, skill, SkillSetting.RADIUS, false);

            applyVisuals(hero.getPlayer());
        }

        @Override
        public void tickHero(final Hero hero) {
            regainMana(hero);
            performKnockback(hero);
        }

        private void regainMana(final Hero hero) {
            final int manaIncreaseAmount = (int) (hero.getManaRegen() * regenMultiplier * perSecondModifier);   // Recalculate every tick for better compatibility with other skills..
            final HeroRegainManaEvent manaEvent = new HeroRegainManaEvent(hero, manaIncreaseAmount, skill);
            plugin.getServer().getPluginManager().callEvent(manaEvent);
            if (!manaEvent.isCancelled()) {
                hero.setMana(manaEvent.getDelta() + hero.getMana());
                if (hero.isVerboseMana()) {
                    hero.getPlayer().sendMessage(ChatComponents.Bars.mana(hero.getMana(), hero.getMaxMana(), true));
                }
            }
        }

        private void performKnockback(final Hero hero) {
            final Player player = hero.getPlayer();

            final List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
            for (final Entity entity : entities) {
                if (!(entity instanceof LivingEntity)) {
                    continue;
                }
                if (!damageCheck(player, (LivingEntity) entity)) {
                    continue;
                }

                final LivingEntity target = (LivingEntity) entity;
                final double individualHPower = hPower;
                final double individualVPower = vPower;

                // Do our knockback
                final Location playerLoc = player.getLocation();
                final Location targetLoc = target.getLocation();

                double xDir = targetLoc.getX() - playerLoc.getX();
                double zDir = targetLoc.getZ() - playerLoc.getZ();
                final double magnitude = Math.sqrt(xDir * xDir + zDir * zDir);

                xDir = xDir / magnitude * individualHPower;
                zDir = zDir / magnitude * individualHPower;

                final Vector velocity = new Vector(xDir, individualVPower, zDir);
                target.setVelocity(velocity);
            }
        }

        private void applyVisuals(final LivingEntity target) {
            final World world = target.getWorld();
            final Location loc = target.getLocation();
            final int durationTicks = 5 * 60 * 20;
            final int displayPeriod = 2;

            this.visualEffect = new CylinderEffect(effectLib);

            final DynamicLocation dynamicLoc = new DynamicLocation(target);
            visualEffect.setDynamicOrigin(dynamicLoc);
            visualEffect.disappearWithOriginEntity = true;
            visualEffect.particle = Particle.REDSTONE;
            visualEffect.color = MANA_BLUE;
            //visualEffect.particles = 50;
            visualEffect.radius = 1.75F;
            visualEffect.height = 5.0F;
            visualEffect.solid = false;
            visualEffect.enableRotation = true;
            visualEffect.angularVelocityX = 0;
            visualEffect.angularVelocityY = 2.5;
            visualEffect.angularVelocityZ = 0;
            visualEffect.period = displayPeriod;
            visualEffect.iterations = durationTicks / displayPeriod;

            effectLib.start(visualEffect);
        }

        @Override
        public void tickMonster(final Monster monster) {
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
        }
    }
}