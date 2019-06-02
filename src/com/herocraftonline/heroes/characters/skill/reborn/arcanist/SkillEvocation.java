package com.herocraftonline.heroes.characters.skill.reborn.arcanist;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainManaEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.CylinderEffect;
import de.slikey.effectlib.util.DynamicLocation;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;

public class SkillEvocation extends ActiveSkill {

    private static Color MANA_BLUE = Color.fromRGB(0, 191, 255);

    private String applyText;
    private String expireText;
    private String effectName = "Evocating";

    public SkillEvocation(Heroes plugin) {
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
    public String getDescription(Hero hero) {
        double regenMultiplier = SkillConfigManager.getUseSetting(hero, this, "regen-multiplier", 4.0, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DELAY, 4000, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(regenMultiplier * 100))
                .replace("$2", Util.decFormat.format((double) duration / 1000));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
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

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is evocating mana!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is no longer evocating.").replace("%hero%", "$1");
    }

    @Override
    public void onWarmup(Hero hero) {
        super.onWarmup(hero);
        Player player = hero.getPlayer();

        hero.addEffect(new ChannelingEffect(this, player));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.0F);
        player.getWorld().spawnParticle(Particle.WATER_SPLASH, player.getLocation(), 65, 0, 0.9, 0, 0.1);
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        hero.removeEffect(hero.getEffect(effectName));
        return SkillResult.NORMAL;
    }

    private class ChannelingEffect extends PeriodicEffect {
        public ChannelingEffect(Skill skill, Player applier) {
            super(skill, "Channeling-" + skill.getName(), applier, 100, null, null);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            int regainPeriod = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.PERIOD, 1000, false);
            hero.addEffect(new EvocationEffect(skill, applier, regainPeriod));
        }

        @Override
        public void tickHero(Hero hero) {
            super.tickHero(hero);

            if (hero.getDelayedSkill() != null && hero.getDelayedSkill().getSkill().equals(skill))
                return;

            // We were interrupted or finished casting.
            hero.removeEffect(this);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            long cooldownMillis = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.COOLDOWN, 1000, false);
            long currentTime = System.currentTimeMillis();

            Long currentCd = hero.getCooldown(skill.getName());
            long defaultCd = currentTime + cooldownMillis;
            if (currentCd == null || currentCd < defaultCd) {
                hero.setCooldown(skill.getName(), System.currentTimeMillis() + cooldownMillis);
            }

            hero.removeEffect(hero.getEffect(effectName));
        }
    }

    private class EvocationEffect extends PeriodicEffect {
        private EffectManager effectManager;
        private CylinderEffect visualEffect;

        private double regenMultiplier;
        private double perSecondModifier;
        private double hPower;
        private double vPower;
        private double radius;

        public EvocationEffect(Skill skill, Player applier, long regainPeriod) {
            super(skill, effectName, applier, regainPeriod, applyText, expireText);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            this.regenMultiplier = SkillConfigManager.getUseSetting(hero, skill, "regen-multiplier", 4.0, false);
            this.perSecondModifier = (1000.0 / getPeriod());
            this.hPower = SkillConfigManager.getUseSetting(hero, skill, "horizontal-power", 0.75, false);
            this.vPower = SkillConfigManager.getUseSetting(hero, skill, "vertical-power", 0.3, false);
            this.radius = SkillConfigManager.getScaledUseSettingDouble(hero, skill, SkillSetting.RADIUS, false);

            applyVisuals(hero.getPlayer());
        }

        @Override
        public void tickHero(Hero hero) {
            regainMana(hero);
            performKnockback(hero);
        }

        private void regainMana(Hero hero) {
            int manaIncreaseAmount = (int) (hero.getManaRegen() * regenMultiplier * perSecondModifier);   // Recalculate every tick for better compatibility with other skills..
            HeroRegainManaEvent manaEvent = new HeroRegainManaEvent(hero, manaIncreaseAmount, skill);
            plugin.getServer().getPluginManager().callEvent(manaEvent);
            if (!manaEvent.isCancelled()) {
                hero.setMana(manaEvent.getDelta() + hero.getMana());
                if (hero.isVerboseMana())
                    hero.getPlayer().sendMessage(ChatComponents.Bars.mana(hero.getMana(), hero.getMaxMana(), true));
            }
        }

        private void performKnockback(Hero hero) {
            Player player = hero.getPlayer();

            List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
            for (Entity entity : entities) {
                if (!(entity instanceof LivingEntity))
                    continue;
                if (!damageCheck(player, (LivingEntity) entity))
                    continue;

                final LivingEntity target = (LivingEntity) entity;
                double individualHPower = hPower;
                double individualVPower = vPower;

                // Do our knockback
                Location playerLoc = player.getLocation();
                Location targetLoc = target.getLocation();

                double xDir = targetLoc.getX() - playerLoc.getX();
                double zDir = targetLoc.getZ() - playerLoc.getZ();
                double magnitude = Math.sqrt(xDir * xDir + zDir * zDir);

                xDir = xDir / magnitude * individualHPower;
                zDir = zDir / magnitude * individualHPower;

                final Vector velocity = new Vector(xDir, individualVPower, zDir);
                target.setVelocity(velocity);
            }
        }

        private void applyVisuals(LivingEntity target) {
            final World world = target.getWorld();
            final Location loc = target.getLocation();
            final int durationTicks = 5 * 60 * 20;
            final int displayPeriod = 2;

            this.effectManager = new EffectManager(plugin);
            this.visualEffect = new CylinderEffect(effectManager);

            DynamicLocation dynamicLoc = new DynamicLocation(target);
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

            effectManager.start(visualEffect);
            effectManager.disposeOnTermination();
        }

        @Override
        public void tickMonster(Monster monster) { }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            if (this.effectManager != null)
                this.effectManager.dispose();
        }
    }
}