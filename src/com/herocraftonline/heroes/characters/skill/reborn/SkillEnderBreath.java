package com.herocraftonline.heroes.characters.skill.reborn;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseGroundEffect;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.Effect;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.EffectType;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.*;

public class SkillEnderBreath extends SkillBaseGroundEffect {

    private static final Random random = new Random(System.currentTimeMillis());

    private Map<Snowball, Long> activeProjectiles = new LinkedHashMap<Snowball, Long>(100) {
        private static final long serialVersionUID = 3329526013158603250L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<Snowball, Long> eldest) {
            return (size() > 60 || eldest.getValue() + 5000 <= System.currentTimeMillis());
        }
    };

    public SkillEnderBreath(Heroes plugin) {
        super(plugin, "EnderBreath");
        setDescription("Launch a ball of Ender Flame at your opponent. "
                + "The projectile explodes on hit, spreading dragon breath $4 blocks to the side and $5 blocks up and down (cylinder). "
                + "Enemies within the breath are dealt $1 damage every $2 seconds for $3 seconds and"
                + "if you are transformed, they suffer chaotic ender teleports. $96 $97 $98 $99");
        setUsage("/skill enderbreath");
        setArgumentRange(0, 0);
        setIdentifiers("skill enderbreath");
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_FIRE, SkillType.ABILITY_PROPERTY_MAGICAL,
                SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT);

        Bukkit.getServer().getPluginManager().registerEvents(new ProjectileListener(), plugin);
    }

    public String getDescription(Hero hero) {
        final int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 4, false);
        int height = SkillConfigManager.getUseSetting(hero, this, HEIGHT_NODE, 2, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);
        final long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 200, false);
        final double damageTick = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 50d, false);

        int warmup = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DELAY, 0, false);
        int stamina = SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA, 0, false);
        int mana = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 0, false);
        long cooldown = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 0, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(damageTick))
                .replace("$2", Util.decFormat.format((double) period / 1000))
                .replace("$3", Util.decFormat.format((double) duration / 1000))
                .replace("$4", Util.decFormat.format(radius))
                .replace("$5", Util.decFormat.format(height))
                .replace("$96", warmup > 0 ? "Cast Time: " + warmup : "")
                .replace("$97", stamina > 0 ? "Stamina: " + stamina : "")
                .replace("$98", mana > 0 ? "Mana: " + mana : "")
                .replace("$99", cooldown > 0 ? "C: " + Util.decFormat.format((double) cooldown / 1000) : "");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.RADIUS.node(), 4);
        node.set(HEIGHT_NODE, 2);
        node.set(SkillSetting.DURATION.node(), 6000);
        node.set(SkillSetting.PERIOD.node(), 200);
        node.set(SkillSetting.DAMAGE_TICK.node(), 50d);
        node.set("velocity-multiplier", 2.0);
        node.set("ticks-lived", 3);
        return node;
    }

    public SkillResult use(final Hero hero, String[] args) {
        if (isAreaGroundEffectApplied(hero))
            return SkillResult.INVALID_TARGET_NO_MSG;

        final Player player = hero.getPlayer();

        final Snowball projectile = player.launchProjectile(Snowball.class);
        activeProjectiles.put(projectile, System.currentTimeMillis());
        projectile.setShooter(player);

        int ticksLived = SkillConfigManager.getUseSetting(hero, this, "ticks-lived", 20, false);
        double mult = SkillConfigManager.getUseSetting(hero, this, "velocity-multiplier", 1.5, false);
        projectile.setVelocity(projectile.getVelocity().normalize().multiply(mult));
//        projectile.setIsIncendiary(true);
//        projectile.setYield(0.0F);

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                if (!projectile.isDead()) {
                    explodeSnowballIntoAoEEffect(projectile);
                    activeProjectiles.remove(projectile);
                }
            }
        }, ticksLived);

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    private void explodeSnowballIntoAoEEffect(Snowball projectile) {
        Player player = (Player) projectile.getShooter();
        Hero hero = plugin.getCharacterManager().getHero(player);

        final int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 4, false);
        int height = SkillConfigManager.getUseSetting(hero, this, HEIGHT_NODE, 2, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);
        final long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 200, false);
        final double damageTick = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 50d, false);

        Location centerLocation = projectile.getLocation();
        int teleportRadius = (int)(radius * 0.75);
        List<Location> locationsInCircle = Util.getCircleLocationList(centerLocation, teleportRadius, 1, false, false, 1);

        DragonFlameAoEEffect groundEffect = new DragonFlameAoEEffect(damageTick, radius, height, locationsInCircle);
        applyAreaGroundEffectEffect(hero, period, duration, centerLocation, radius, height, groundEffect);
    }

    public class ProjectileListener implements Listener {

        ProjectileListener() {}

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onEntityExploide(EntityExplodeEvent event) {
            if (!(event.getEntity() instanceof Snowball))
                return;

            final Snowball projectile = (Snowball) event.getEntity();
            if ((!(projectile.getShooter() instanceof Player)))
                return;

            if (!(activeProjectiles.containsKey(projectile)))
                return;

            explodeSnowballIntoAoEEffect(projectile);
            activeProjectiles.remove(projectile);
            event.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onProjectileHit(ProjectileHitEvent event) {
            if (!(event.getEntity() instanceof Snowball))
                return;

            final Snowball projectile = (Snowball) event.getEntity();
            if ((!(projectile.getShooter() instanceof Player)))
                return;

            if (!(activeProjectiles.containsKey(projectile)))
                return;

            explodeSnowballIntoAoEEffect(projectile);
            activeProjectiles.remove(projectile);
        }

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageEvent event) {
            if (!(event instanceof EntityDamageByEntityEvent) || !(event.getEntity() instanceof LivingEntity)) {
                return;
            }

            EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
            Entity projectile = subEvent.getDamager();
            if (!(projectile instanceof Snowball) || !activeProjectiles.containsKey(projectile)) {
                return;
            }

            activeProjectiles.remove(projectile);
            event.setCancelled(true);
        }
    }

    private class DragonFlameAoEEffect implements GroundEffectActions {
        private final float pitchMin = -180;
        private final float pitchMax = 180;
        private final float yawMin = -180;
        private final float yawMax = 180;

        private final double damageTick;
        private final int radius;
        private final int height;
        private final List<Location> locationsInRadius;

        DragonFlameAoEEffect(double damageTick, int radius, int height, List<Location> locationsInRadius) {
            this.damageTick = damageTick;

            this.radius = radius;
            this.height = height;
            this.locationsInRadius = locationsInRadius;
        }

        @Override
        public void groundEffectTargetAction(Hero hero, final LivingEntity target, final AreaGroundEffectEffect groundEffect) {
            Player player = hero.getPlayer();
            if (!damageCheck(player, target))
                return;


            //double diminshedDamage = ApplyAoEDiminishingReturns()
            addSpellTarget(target, hero);
            damageEntity(target, player, damageTick / 2d, DamageCause.FIRE, false);
            damageEntity(target, player, damageTick / 2d, DamageCause.MAGIC, false);

            if (hero.hasEffect("Transformed")) {
                int randomLocIndex = random.nextInt(locationsInRadius.size() - 1);

                Location newLocation = locationsInRadius.get(randomLocIndex).clone();
                World targetWorld = newLocation.getWorld();

                target.teleport(newLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
                targetWorld.playEffect(newLocation, org.bukkit.Effect.ENDER_SIGNAL, 3);
                targetWorld.spawnParticle(Particle.REDSTONE, player.getLocation(), 45, 0.6, 1, 0.6, 0.2, new Particle.DustOptions(Color.FUCHSIA, 1));
                targetWorld.playSound(newLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8F, 1.0F);
            }
        }

        @Override
        public void groundEffectTickAction(Hero hero, AreaGroundEffectEffect effect) {
            final Player player = hero.getPlayer();

            EffectManager em = new EffectManager(plugin);
            Effect visualEffect = new Effect(em) {
                Particle particle = Particle.DRAGON_BREATH;
                final double randomMin = -0.15;
                final double randomMax = 0.15;

                @Override
                public void onRun() {
                    for (double z = -radius; z <= radius; z += 0.33) {
                        for (double x = -radius; x <= radius; x += 0.33) {
                            if (x * x + z * z <= radius * radius) {
                                double randomX = x + getRandomInRange(randomMin, randomMax);
                                double randomZ = z + getRandomInRange(randomMin, randomMax);
                                display(particle, getLocation().clone().add(randomX, 0, randomZ));
                            }
                        }
                    }
                }
            };

//            visualEffect.type = EffectType.REPEATING;
//            visualEffect.period = 10;
//            visualEffect.iterations = 8;

            Location location = effect.getLocation().clone();
            visualEffect.asynchronous = true;
            visualEffect.iterations = 1;
            visualEffect.type = EffectType.INSTANT;
            visualEffect.setLocation(location);
//            visualEffect.color = Color.BLACK;

            visualEffect.start();
            em.disposeOnTermination();

            player.getWorld().playSound(location, Sound.ENTITY_GENERIC_BURN, 0.15f, 0.0001f);
        }
    }

    private double getRandomInRange(double minValue, double maxValue) {
        return minValue + random.nextDouble() * ((maxValue - minValue) + 1);
    }

    private float getRandomInRange(float minValue, float maxValue) {
        return minValue + random.nextFloat() * ((maxValue - minValue) + 1);
    }

    private double ApplyAoEDiminishingReturns(double damage, int numberOfTargets)
    {
        return ApplyAoEDiminishingReturns(damage, numberOfTargets, 3, 0.15, 0.75);
    }

    private double ApplyAoEDiminishingReturns(double damage, int numberOfTargets, int maxTargetsBeforeDiminish, double diminishPercent, double maxDiminishPercent)
    {
        if (numberOfTargets > maxTargetsBeforeDiminish) {
            double totalDiminishPercent = (diminishPercent * numberOfTargets);
            if (totalDiminishPercent > maxDiminishPercent)
                totalDiminishPercent = maxDiminishPercent;
            return totalDiminishPercent / damage * 100;
        } else {
            return damage;
        }
    }
}
