package com.herocraftonline.heroes.characters.skill.reborn.badpyromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.CombustEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class SkillFireStream extends ActiveSkill {

    private Map<Snowball, Long> projectiles = new LinkedHashMap<Snowball, Long>(100) {
        private static final long serialVersionUID = 2329013558608752L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<Snowball, Long> eldest) {
            return (size() > 300 || eldest.getValue() + 5000 <= System.currentTimeMillis());
        }
    };

    public SkillFireStream(Heroes plugin) {
        super(plugin, "FireStream");
        setDescription("You shoot a ball of fire that deals $1 damage and lights whatever it hits on fire");
        setUsage("/skill firestream");
        setArgumentRange(0, 0);
        setIdentifiers("skill firestream");
        setTypes(SkillType.ABILITY_PROPERTY_FIRE, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 80, false);

        return getDescription().replace("$1", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 15);
        config.set("fire-ticks-on-hit", 20);
        config.set("total-projectile-count", 20);
        config.set("projectiles-per-launch", 3);
        config.set("velocity-multiplier", 0.75);
        config.set("launch-delay-server-ticks", 3);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        double mult = SkillConfigManager.getUseSetting(hero, this, "velocity-multiplier", 0.75, false);
        int numFireballs = SkillConfigManager.getUseSetting(hero, this, "total-projectile-count", 20, false);
        int projectilesPerLaunch = SkillConfigManager.getUseSetting(hero, this, "projectiles-per-launch", 2, false);
        int launchDelay = SkillConfigManager.getUseSetting(hero, this, "launch-delay-server-ticks", 3, false);

        final double randomMin = -0.1;
        final double randomMax = 0.1;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1F, 0.533F);

        int totalDelayedLaunchLoops = numFireballs / projectilesPerLaunch;
        for (int launchLoopCount = 0; launchLoopCount < totalDelayedLaunchLoops; launchLoopCount++) {

            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                public void run() {

                    for(int launchedThisLoop = 0; launchedThisLoop < projectilesPerLaunch; launchedThisLoop++) {
                        Snowball projectile = player.launchProjectile(Snowball.class);

                        Vector newVelocity = projectile.getVelocity()
                                .normalize()
                                .add(new Vector(ThreadLocalRandom.current().nextDouble(randomMin, randomMax), 0, ThreadLocalRandom.current().nextDouble(randomMin, randomMax)))
                                .multiply(mult);
                        projectile.setVelocity(newVelocity);

                        projectile.setFireTicks(100);
                        projectiles.put(projectile, System.currentTimeMillis());
                        projectile.setShooter(player);
                    }

                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.3F, 0.6F);
                }
            }, launchLoopCount * launchDelay);
        }

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

//    private double getRandomInRange(double minValue, double maxValue) {
//        return minValue + random.nextDouble() * ((maxValue - minValue) + 1);
//    }
//
//    private float getRandomInRange(float minValue, float maxValue) {
//        return minValue + random.nextFloat() * ((maxValue - minValue) + 1);
//    }
//    private static ConeEffect createCone(EffectManager em, Particle particle, double height, double radius) {
//
//        final int PARTICLES_PER_HEIGHT = 10;
//        final int PARTICLES_PER_RADIUS = 10;
//        final double ANGULAR_VELOCITY_PI_DIVISOR_PER_RADIUS = 10;
//
//        ConeEffect vFireEffect = new ConeEffect(em) {
//            @Override
//            public void onRun() {
//                Location location = this.getLocation();
//
//                for (int x = 0; x < this.particles; ++x) {
//                    if (this.step > this.particlesCone) {
//                        this.step = 0;
//                    }
//
//                    if (this.randomize && this.step == 0) {
//                        this.rotation = RandomUtils.getRandomAngle();
//                    }
//
//                    double angle = (double) this.step * this.angularVelocity + this.rotation;
//                    float radius = (float) this.step * this.radiusGrow;
//                    float length = (float) this.step * this.lengthGrow;
//                    Vector v = new Vector(Math.cos(angle) * (double) radius, (double) length, Math.sin(angle) * (double) radius);
//                    VectorUtils.rotateAroundAxisX(v, (double) ((location.getPitch() + 90.0F) * 0.017453292F));
//                    VectorUtils.rotateAroundAxisY(v, (double) (-location.getYaw() * 0.017453292F));
//                    location.add(v);
//                    this.display(this.particle, location);
//                    location.subtract(v);
//                    ++this.step;
//                }
//            }
//        };
//
//        vFireEffect.particle = particle;
//        vFireEffect.angularVelocity = Math.PI / (radius * ANGULAR_VELOCITY_PI_DIVISOR_PER_RADIUS);
//        vFireEffect.particles = 1 + (int) (height * PARTICLES_PER_HEIGHT) + (int) (radius * PARTICLES_PER_RADIUS);
//        vFireEffect.particlesCone = effect.particles;
//        vFireEffect.lengthGrow =  (float) height / effect.particles;
//        vFireEffect.radiusGrow = (float) radius / effect.particles;
//        vFireEffect.asynchronous = true;
//        vFireEffect.type = EffectType.INSTANT;
//        vFireEffect.iterations = 1;
//
//        return vFireEffect;
//    }

    public class SkillEntityListener implements Listener {

        private final Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onProjectileHit(ProjectileHitEvent event) {
            if (!(event.getEntity() instanceof Snowball))
                return;

            final Snowball projectile = (Snowball) event.getEntity();
            if ((!(projectile.getShooter() instanceof Player)) || !projectiles.containsKey(projectile))
                return;

            if (event.getHitBlock() != null) {
                Block hitBlock = event.getHitBlock();
                final Block fireBlock1 = hitBlock.getRelative(event.getHitBlockFace());
                if (fireBlock1.isEmpty())
                    fireBlock1.setType(Material.FIRE);

                final Block fireBlock2 = hitBlock.getRelative(BlockFace.UP);
                if (fireBlock2.isEmpty())
                    fireBlock2.setType(Material.FIRE);

                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        if (fireBlock1.getType() == Material.FIRE)
                            fireBlock1.setType(Material.AIR);
                        if (fireBlock2.getType() == Material.FIRE)
                            fireBlock2.setType(Material.AIR);
                    }
                }, 20 * 5);
            }

            if (event.getHitEntity() == null) {
                projectiles.remove(projectile);
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageByEntityEvent event) {
            if (!(event.getEntity() instanceof LivingEntity)) {
                return;
            }

            Entity projectile = event.getDamager();
            if (!(projectile instanceof Snowball) || !projectiles.containsKey(projectile)) {
                return;
            }

            projectiles.remove(projectile);
            event.setCancelled(true);

            LivingEntity targetLE = (LivingEntity) event.getEntity();
            ProjectileSource source = ((Projectile) event.getDamager()).getShooter();
            if (!(source instanceof Player))
                return;

            Player dmger = (Player) source;
            if (!damageCheck((Player) dmger, targetLE))
                return;

            Hero hero = plugin.getCharacterManager().getHero((Player) dmger);

            targetLE.setFireTicks(SkillConfigManager.getUseSetting(hero, skill, "fire-ticks-on-hit", 20, false));
            plugin.getCharacterManager().getCharacter(targetLE).addEffect(new CombustEffect(skill, (Player) dmger));

            double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 80, false);
            addSpellTarget(targetLE, hero);
            damageEntity(targetLE, hero.getPlayer(), damage, DamageCause.FIRE);

            //targetLE.getWorld().spigot().playEffect(targetLE.getLocation().add(0, 0.5F, 0), Effect.FLAME, 0, 0, 0.2F, 0.2F, 0.2F, 0.1F, 50, 16);
            targetLE.getWorld().spawnParticle(Particle.FLAME, targetLE.getLocation(), 50, 0.2F, 0.7F, 0.2F, 16);
            targetLE.getWorld().playSound(targetLE.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 7.0F, 1.0F);
        }
    }
}