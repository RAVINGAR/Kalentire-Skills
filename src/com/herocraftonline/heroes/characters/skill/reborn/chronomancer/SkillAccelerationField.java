package com.herocraftonline.heroes.characters.skill.reborn.chronomancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.PeriodicManaDrainEffect;
import com.herocraftonline.heroes.characters.effects.common.WalkSpeedIncreaseEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.CylinderEffect;
import de.slikey.effectlib.effect.HelixEffect;
import de.slikey.effectlib.effect.TraceEffect;
import de.slikey.effectlib.effect.VortexEffect;
import de.slikey.effectlib.util.DynamicLocation;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Collection;

public class SkillAccelerationField extends ActiveSkill {

    private static final float DEFAULT_MINECRAFT_MOVEMENT_SPEED = 0.2f;

    private String applyText;
    private String expireText;

    public SkillAccelerationField(Heroes plugin) {
        super(plugin, "AccelerationField");
        setDescription("You tap into the web of time around you in a $1 radius, accelerating anyone and anything possible for $2 seconds.");
        setUsage("/skill accelerationfield");
        setArgumentRange(0, 0);
        setIdentifiers("skill accelerationfield");
        setToggleableEffectName("AccelerationField");
        setTypes(SkillType.MULTI_GRESSIVE, SkillType.MOVEMENT_INCREASING, SkillType.AREA_OF_EFFECT);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 16, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        return getDescription().replace("$1", radius + "").replace("$2", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.RADIUS.node(), 16);
        config.set("percent-speed-increase", 0.35);
        config.set("projectile-velocity-multiplier", 1.35);
        config.set("pulse-period", 250);
        config.set("mana-drain-per-pulse", 6);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is accelerating time!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is no longer accelerating time.");
        config.set(SkillSetting.DELAY.node(), 1500);
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "    " + ChatComponents.GENERIC_SKILL + "%hero% is accelerating time!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "    " + ChatComponents.GENERIC_SKILL + "%hero% is no longer accelerating time.").replace("%hero%", "$1");
        setUseText(null);
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        hero.removeEffect(hero.getEffect("DecelerationField"));

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 20, false);
        int pulsePeriod = SkillConfigManager.getUseSetting(hero, this, "pulse-period", 250, false);
        int manaDrainPerPulse = SkillConfigManager.getUseSetting(hero, this, "mana-drain-per-pulse", 6, false);
        double percentSpeedIncrease = SkillConfigManager.getUseSetting(hero, this, "percent-speed-increase", 0.35, false);
        double projectileVMulti = SkillConfigManager.getUseSetting(hero, this, "projectile-velocity-multiplier", 1.35, false);

        AcceleratedFieldEmitterEffect emitterEffect = new AcceleratedFieldEmitterEffect(this, player, pulsePeriod, manaDrainPerPulse, radius, percentSpeedIncrease, projectileVMulti);
        hero.addEffect(emitterEffect);

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 2.0F, 2.0F);
        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    public class AcceleratedFieldEmitterEffect extends PeriodicManaDrainEffect {

        private final EffectManager effectManager;
        private final int radius;
        private final int heightRadius;
        private final int offsetHeight;
        private final double flatSpeedIncrease;
        private final double projVMulti;

        AcceleratedFieldEmitterEffect(Skill skill, Player applier, int period, int manaDrainPerTick, int radius, double percentIncrease, double projVMulti) {
            super(skill, "AccelerationField", applier, period, manaDrainPerTick, applyText, expireText);

            this.effectManager = new EffectManager(plugin);
            this.radius = radius;
            this.heightRadius = 10; //(int) (radius * 0.25);
            this.offsetHeight = 5; //(int) ((radius * 0.25) / 2);
            this.flatSpeedIncrease = Util.convertPercentageToPlayerMovementSpeedValue(percentIncrease);
            this.projVMulti = projVMulti;

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            Player player = hero.getPlayer();
            int durationTicks = 20 * 60 * 60;    // An hour. We'll most likely terminate it early and I don't imagine other people will ever bother to try and maintain it that long.

            CylinderEffect effect = new CylinderEffect(effectManager);
            DynamicLocation dynamicLoc = new DynamicLocation(player);
            effect.setDynamicOrigin(dynamicLoc);
            effect.disappearWithOriginEntity = true;
            effect.height = heightRadius;
            effect.radius = radius;
            effect.period = 1;
            effect.iterations = durationTicks;

            effect.particles = 150;
            effect.particle = Particle.SPELL_MOB;
            effect.color = Color.TEAL;
            effect.solid = false;
            effect.enableRotation = false;

            effect.asynchronous = true;
            effectManager.start(effect);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            effectManager.dispose();
        }

        @Override
        public void tickMonster(Monster monster) { }

        @Override
        public void tickHero(Hero hero) {
            accelerateField(hero);
        }

        private void accelerateField(Hero hero) {
            Player player = hero.getPlayer();

            Location currentLoc = player.getLocation();
            int tempDuration = (int) (getPeriod() + 250);

            Collection<Entity> nearbyEnts = currentLoc.getWorld().getNearbyEntities(currentLoc, radius, heightRadius, radius);
            for (Entity ent : nearbyEnts) {
                if (ent instanceof Projectile) {
                    accelerateProjectile((Projectile) ent, projVMulti);
                } else if (ent instanceof LivingEntity) {
                    CharacterTemplate ctTarget = plugin.getCharacterManager().getCharacter((LivingEntity) ent);
                    if (ctTarget == null)
                        continue;
                    if (ctTarget.hasEffect("TemporallyWarded"))
                        continue;

                    ctTarget.removeEffect(ctTarget.getEffect("AcceleratedTime"));
                    ctTarget.addEffect(new AcceleratedTimeEffect(skill, player, tempDuration, flatSpeedIncrease, projVMulti));
                }
            }
        }
    }

    public class AcceleratedTimeEffect extends WalkSpeedIncreaseEffect {
        final double projVMulti;

        AcceleratedTimeEffect(Skill skill, Player applier, int duration, double speedIncrease, double projVMulti) {
            super(skill, "AcceleratedTime", applier, duration, speedIncrease, null, null);
            this.projVMulti = projVMulti;

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);

            addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) (getDuration() / 50), 3));
        }
    }

    private void accelerateProjectile(Projectile proj, double multi) {
        if (proj.hasMetadata("AcceleratedTime"))
            return;
        Vector multipliedVel = proj.getVelocity().multiply(multi);
        proj.setVelocity(multipliedVel);
        //proj.setGlowing(true);
        proj.setMetadata("AcceleratedTime", new FixedMetadataValue(plugin, true));
    }

    public class SkillListener implements Listener {

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onProjLaunch(ProjectileLaunchEvent event) {
            if (event.getEntity().getShooter() == null)
                return;
            if (!(event.getEntity().getShooter() instanceof LivingEntity))
                return;
            if (event.getEntity().hasMetadata("AcceleratedTime"))
                return;

            LivingEntity shooter = (LivingEntity) event.getEntity().getShooter();
            CharacterTemplate ctShooter = plugin.getCharacterManager().getCharacter(shooter);
            if (ctShooter == null || !ctShooter.hasEffect("AcceleratedTime"))
                return;

            AcceleratedTimeEffect effect = (AcceleratedTimeEffect) ctShooter.getEffect("AcceleratedTime");
            if (effect == null)
                return;

            accelerateProjectile(event.getEntity(), effect.projVMulti);
        }
    }
}
