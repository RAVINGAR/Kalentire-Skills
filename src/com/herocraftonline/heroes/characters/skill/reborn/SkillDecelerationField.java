package com.herocraftonline.heroes.characters.skill.reborn;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.WalkSpeedDecreaseEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.MathUtils;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.CylinderEffect;
import de.slikey.effectlib.util.DynamicLocation;
import fr.neatmonster.nocheatplus.checks.moving.Velocity;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.HashMap;

public class SkillDecelerationField extends ActiveSkill {

    private static final float DEFAULT_MINECRAFT_MOVEMENT_SPEED = 0.2f;

    private String applyText;
    private String expireText;

    public SkillDecelerationField(Heroes plugin) {
        super(plugin, "DecelerationField");
        setDescription("You tap into the web of time around you in a $1 radius, decelerating anyone and anything possible for $2 seconds.");
        setUsage("/skill decelerationfield");
        setArgumentRange(0, 0);
        setIdentifiers("skill decelerationfield");
        setTypes(SkillType.MULTI_GRESSIVE, SkillType.MOVEMENT_SLOWING, SkillType.AREA_OF_EFFECT);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 20, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        return getDescription().replace("$1", radius + "").replace("$2", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.RADIUS.node(), 16);
        config.set("percent-speed-decrease", 0.35);
        config.set("projectile-velocity-multiplier", 0.5);
        config.set(SkillSetting.DURATION.node(), 5000);
        config.set("pulse-period", 250);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is decelerating time!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is no longer decelerating time.");
        config.set(SkillSetting.DELAY.node(), 1500);
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is decelerating time!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is no longer decelerating time.").replace("%hero%", "$1");
        setUseText(null);
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        hero.removeEffect(hero.getEffect("AccelerationField"));
        hero.removeEffect(hero.getEffect("DecelerationField"));

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 16, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        int pulsePeriod = SkillConfigManager.getUseSetting(hero, this, "pulse-period", 250, false);
        double percentDecrease = SkillConfigManager.getUseSetting(hero, this, "percent-speed-decrease", 0.35, false);
        double projectileVMulti = SkillConfigManager.getUseSetting(hero, this, "projectile-velocity-multiplier", 0.5, false);

        DeceleratedFieldEmitterEffect emitterEffect = new DeceleratedFieldEmitterEffect(this, player, pulsePeriod, duration, radius, percentDecrease, projectileVMulti);
        hero.addEffect(emitterEffect);

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 2.0F, 0.533F);
//        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.3F, 0.5F);
        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    private double toFlatSpeedModifier(double percent) {
        return DEFAULT_MINECRAFT_MOVEMENT_SPEED * percent;
    }

    public class DeceleratedFieldEmitterEffect extends PeriodicExpirableEffect {

        private final EffectManager effectManager;
        private final int radius;
        private final int heightRadius;
        private final int offsetHeight;
        private final double percentDecrease;
        private final double flatDecrease;
        private final double projVMulti;

        DeceleratedFieldEmitterEffect(Skill skill, Player applier, int period, int duration,
                                      int radius, double percentDecrease, double projVMulti) {
            super(skill, "DecelerationField", applier, period, duration, applyText, expireText);
            this.effectManager = new EffectManager(plugin);
            this.radius = radius;
            this.heightRadius = 10; //(int) (radius * 0.25);
            this.offsetHeight = 5; //(int) ((radius * 0.25) / 2);
            this.percentDecrease = percentDecrease;
            this.flatDecrease = toFlatSpeedModifier(percentDecrease);
            this.projVMulti = projVMulti;

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            Player player = hero.getPlayer();
            int durationTicks = (int) (getDuration() / 50);

            CylinderEffect effect = new CylinderEffect(effectManager);
            DynamicLocation dynamicLoc = new DynamicLocation(player);
//            dynamicLoc.addOffset(new Vector(0, offsetHeight, 0));
            effect.setDynamicOrigin(dynamicLoc);
            effect.disappearWithOriginEntity = true;
            effect.height = heightRadius * 2;
            effect.radius = radius;
            effect.period = 1;
            effect.iterations = durationTicks;

            effect.particles = 150;
            effect.particle = Particle.SPELL_MOB;
            effect.color = Color.YELLOW;
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
            decelerateField(hero);
        }

        private void decelerateField(Hero hero) {
            Player player = hero.getPlayer();

            Location currentLoc = player.getLocation();
            int tempDuration = (int) (getPeriod() + 250);

            Collection<Entity> nearbyEnts = currentLoc.getWorld().getNearbyEntities(currentLoc, radius, heightRadius, radius);
            for (Entity ent : nearbyEnts) {
                if (ent instanceof Projectile) {
                    decelerateProjectile((Projectile) ent, projVMulti);
                } else if (ent instanceof LivingEntity) {
                    LivingEntity lEnt = (LivingEntity) ent;
                    CharacterTemplate ctTarget = plugin.getCharacterManager().getCharacter(lEnt);
                    if (ctTarget == null)
                        continue;
                    if (ctTarget.hasEffect("TemporallyWarded"))
                        continue;

                    if (ctTarget instanceof Hero) {
                        if (!damageCheck(player, lEnt) && ctTarget != hero && (hero.getParty() == null || hero.getParty().isPartyMember((Hero) ctTarget)))
                            continue;
                    } else {
                        if (!damageCheck(player, lEnt) && ctTarget != hero)
                            continue;
                    }

                    ctTarget.removeEffect(ctTarget.getEffect("DeceleratedTime"));
                    ctTarget.addEffect(new DeceleratedTimeEffect(skill, player, tempDuration, flatDecrease, percentDecrease, projVMulti));
                }
            }
        }
    }

    public class DeceleratedTimeEffect extends WalkSpeedDecreaseEffect {
        final double percentDecrease;
        final double projVMulti;
        boolean slowedInAirAlready;

        DeceleratedTimeEffect(Skill skill, Player applier, int duration, double flatDecrease, double percentDecrease, double projVMulti) {
            super(skill, "DeceleratedTime", applier, duration, flatDecrease, null, null);
            this.percentDecrease = percentDecrease;
            this.projVMulti = projVMulti;

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.MAGIC);
//            addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, (int) (duration / 50), 25));
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
        }
    }

    private void decelerateProjectile(Projectile proj, double multi) {
        if (proj.hasMetadata("DeceleratedTime"))
            return;
        Vector multipliedVel = proj.getVelocity().multiply(multi);
        proj.setVelocity(multipliedVel);
        //proj.setGlowing(true);
        proj.setMetadata("DeceleratedTime", new FixedMetadataValue(plugin, true));
    }

    public class SkillListener implements Listener {

        // Move listeners are super not cool. Do not copy this elsewhere unless you REALLY know what you are doing.
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onPlayerMove(PlayerMoveEvent event) {
            final Player player = event.getPlayer();
            if (!player.isSprinting() || player.isFlying() || player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE)
                return;

            if (player.isOnGround()) {
                if (player.hasMetadata("DeceleratedJump"))
                    player.removeMetadata("DeceleratedJump", plugin);
                return;
            } else if (event.getFrom().getY() == event.getTo().getY() || player.hasMetadata("DeceleratedJump")) {
                return;
            }

            Hero hero = plugin.getCharacterManager().getHero(player);
            if (!hero.hasEffect("DeceleratedTime"))
                return;
            DeceleratedTimeEffect effect = (DeceleratedTimeEffect) hero.getEffect("DeceleratedTime");
            if (effect == null)
                return;

            player.setMetadata("DeceleratedJump", new FixedMetadataValue(plugin, true));

//            float hellIfIKnow = player.getLocation().getYaw() * 0.017453292F;
//            Vector newVelocity = player.getVelocity().clone();
//            newVelocity.add(new Vector((Math.sin(hellIfIKnow) * 0.2D), -0.25D, -(Math.cos(hellIfIKnow) * 0.2D)));
//            player.setVelocity(newVelocity);
            float mitigatedFallDist = player.getFallDistance() - 0.5F;
            player.setVelocity(player.getVelocity().multiply(1 - effect.percentDecrease).subtract(new Vector(0, 0.15D, 0)));
            player.setFallDistance(mitigatedFallDist);

//            player.setVelocity(newVelocity);
//            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
//                public void run() {
//                    float mitigatedFallDist = player.getFallDistance() - 0.5F;
//                    player.setVelocity(player.getVelocity().multiply(1 - effect.percentDecrease));
//                    player.setFallDistance(mitigatedFallDist);
//                }
//            }, 1);
        }

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onProjLaunch(ProjectileLaunchEvent event) {
            if (event.getEntity().getShooter() == null)
                return;
            if (!(event.getEntity().getShooter() instanceof LivingEntity))
                return;
            if (event.getEntity().hasMetadata("DeceleratedTime"))
                return;

            LivingEntity shooter = (LivingEntity) event.getEntity().getShooter();
            CharacterTemplate ctShooter = plugin.getCharacterManager().getCharacter(shooter);
            if (ctShooter == null || !ctShooter.hasEffect("DeceleratedTime"))
                return;

            DeceleratedTimeEffect effect = (DeceleratedTimeEffect) ctShooter.getEffect("DeceleratedTime");
            if (effect == null)
                return;

            decelerateProjectile(event.getEntity(), effect.projVMulti);
        }
    }
}
