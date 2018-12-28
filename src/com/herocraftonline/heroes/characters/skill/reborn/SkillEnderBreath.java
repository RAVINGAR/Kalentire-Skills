package com.herocraftonline.heroes.characters.skill.reborn;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseGroundEffect;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.Effect;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.DragonEffect;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SkillEnderBreath extends SkillBaseGroundEffect {

    private Map<Snowball, Long> activeProjectiles = new LinkedHashMap<Snowball, Long>(100) {
        private static final long serialVersionUID = 3329526013158603250L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<Snowball, Long> eldest) {
            return (size() > 60 || eldest.getValue() + 5000 <= System.currentTimeMillis());
        }
    };
    
    public SkillEnderBreath(Heroes plugin) {
        super(plugin, "EnderBreath");
        setDescription("Breathe the flame of an Ender Dragon.");
        setUsage("/skill enderbreath");
        setArgumentRange(0, 0);
        setIdentifiers("skill enderbreath");
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_FIRE, SkillType.ABILITY_PROPERTY_DARK, SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(), plugin);
    }

    public String getDescription(Hero hero) {

//        int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 6, false);
//        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 90, false);
        return getDescription();//.replace("$1", distance + "").replace("$2", damage + "");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE.node(), 80);
        node.set(SkillSetting.DAMAGE_TICK.node(), 40);
        node.set(HEIGHT_NODE, 3);
        node.set(SkillSetting.RADIUS.node(), 6);
        node.set(SkillSetting.PERIOD.node(), 500);
        node.set(SkillSetting.DURATION.node(), 3000);
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
                    explodeFireballIntoAoEEffect(projectile);
                    activeProjectiles.remove(projectile);
                }
            }
        }, ticksLived);

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    private void explodeFireballIntoAoEEffect(Snowball projectile) {
        Player player = (Player) projectile.getShooter();
        Hero hero = plugin.getCharacterManager().getHero(player);

        int height = SkillConfigManager.getUseSetting(hero, this, HEIGHT_NODE, 3, false);
        final double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 80d, false);
        final double damageTick = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 40d, false);
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 6, false);
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 500, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
        
        applyAreaGroundEffectEffect(hero, period, duration, projectile.getLocation(), radius, height, new FlameAoEEffect(damage, damageTick, radius, height));
    }

    public class SkillEntityListener implements Listener {

        public SkillEntityListener() {}

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onProjectileHit(ProjectileHitEvent event) {
            if (!(event.getEntity() instanceof Snowball))
                return;

            final Snowball fireball = (Snowball) event.getEntity();
            if ((!(fireball.getShooter() instanceof Player)))
                return;

            if (!(activeProjectiles.containsKey(fireball)))
                return;

            explodeFireballIntoAoEEffect(fireball);
            activeProjectiles.remove(fireball);
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

    private class FlameAoEEffect implements GroundEffectActions {

        private final double damage;
        private final double damageTick;
        private final int radius;
        private final int height;

        public FlameAoEEffect(double damage, double damageTick, int radius, int height) {
            this.damage = damage;
            this.damageTick = damageTick;

            this.radius = radius;
            this.height = height;
        }

        @Override
        public void groundEffectTargetAction(Hero hero, final LivingEntity target, final AreaGroundEffectEffect groundEffect) {
            Player player = hero.getPlayer();
            if (!damageCheck(player, target))
                return;

            damageEntity(target, player, damageTick / 2d, DamageCause.FIRE, false);
            damageEntity(target, player, damageTick / 2d, DamageCause.MAGIC, false);
        }

        @Override
        public void groundEffectTickAction(Hero hero, AreaGroundEffectEffect effect) {
            final Player player = hero.getPlayer();
            EffectManager em = new EffectManager(plugin);
            DragonEffect e = new DragonEffect(em);
//            Effect e = new Effect(em) {
//
//                int particlesPerRadius = 3;
//                //Particle particle = Particle.DRAGON_BREATH;
//                Particle particle = Particle.REDSTONE;
//
//                @Override
//                public void onRun() {
//                    double inc = 1d / (particlesPerRadius * radius);
//
//                    for (double angle = 0; angle <= 2 * Math.PI; angle += inc) {
//                        Vector v = new Vector(Math.cos(angle), 0, Math.sin(angle)).multiply(radius);
//                        display(particle, getLocation().add(v));
//                        getLocation().subtract(v);
//                    }
//
//                    Location originalLocation = getLocation();
//                    Color originalColor = color;
//                    color = Color.RED;
//
//                    int particles = (int) (2 * radius * particlesPerRadius);
//                    Vector crossXLine = new Vector(-radius * 2, 0, 0).multiply(1d / particles);
//                    Vector crossZLine = new Vector(0, 0, -radius * 2).multiply(1d / particles);
//
//                    setLocation(new Vector(radius, 0, radius / 10).toLocation(getLocation().getWorld()).add(originalLocation));
//                    for (int l = 0; l < particles; l++, getLocation().add(crossXLine)) {
//                        display(particle, getLocation());
//                    }
//
//                    setLocation(new Vector(radius / 10, 0, radius).toLocation(getLocation().getWorld()).add(originalLocation));
//                    for (int l = 0; l < particles; l++, getLocation().add(crossZLine)) {
//                        display(particle, getLocation());
//                    }
//
//                    setLocation(new Vector(radius, 0, radius / -10).toLocation(getLocation().getWorld()).add(originalLocation));
//                    for (int l = 0; l < particles; l++, getLocation().add(crossXLine)) {
//                        display(particle, getLocation());
//                    }
//
//                    setLocation(new Vector(radius / -10, 0, radius).toLocation(getLocation().getWorld()).add(originalLocation));
//                    for (int l = 0; l < particles; l++, getLocation().add(crossZLine)) {
//                        display(particle, getLocation());
//                    }
//
//                    setLocation(originalLocation);
//                    color = originalColor;
//                }
//            };

            e.setLocation(effect.getLocation().clone());
            e.asynchronous = true;
            e.iterations = 1;
            e.type = de.slikey.effectlib.EffectType.INSTANT;
            e.color = Color.BLACK;

            e.start();
            em.disposeOnTermination();

            player.getWorld().playSound(effect.getLocation(), Sound.ENTITY_GENERIC_BURN, 0.15f, 0.0001f);
        }
    }

    private class DragonBreathAoEEffect extends PeriodicExpirableEffect {
        private final long _duration;
        private final int _distance;
        private final double _radiusSquared;
        private final int _delay;
        private final double _damage;

        public DragonBreathAoEEffect(Skill skill, Player applier, long period, long duration, int distance, double radius, int delay, double damage) {
            super(skill, "DragonBreathAoE", applier, period, duration);
            _duration = duration;
            _distance = distance;
            _radiusSquared = radius * radius;
            _delay = delay;
            _damage = damage;

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.PHYSICAL);
        }

        @Override
        public void tickMonster(Monster monster) {
        }

        @Override
        public void tickHero(Hero hero) {
            ShootBreath(hero);
        }

        private void ShootBreath(Hero hero) {
            final Player player = hero.getPlayer();
            Block tempBlock;
            BlockIterator iter = null;
            try {
                iter = new BlockIterator(player, _distance);
            } catch (IllegalStateException e) {
                return;
            }

            final List<Entity> nearbyEntities = player.getNearbyEntities(_distance * 2, _distance, _distance * 2);
            final List<Entity> hitEnemies = new ArrayList<>();

            player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 6.0F, 1);

            int numBlocks = 0;
            while (iter.hasNext()) {
                tempBlock = iter.next();
                Material tempBlockType = tempBlock.getType();
                if (!Util.transparentBlocks.contains(tempBlockType))
                    break;

                final Location targetLocation = tempBlock.getLocation().clone().add(new Vector(.5, 0, .5));
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {

                        //player.getWorld().spawnParticle(Particle.FLAME, targetLocation, 25, 0, 0, 0, 1);
                        player.getWorld().spawnParticle(Particle.FLAME, targetLocation, 10, 3, 3, 3, 1);
                        player.getWorld().spawnParticle(Particle.BLOCK_CRACK, targetLocation, 4, 0.3, 0.3, 0.3, 0.1, Bukkit.createBlockData(Material.MAGMA_BLOCK));

                        for (Entity entity : nearbyEntities) {
                            if (!(entity instanceof LivingEntity) || hitEnemies.contains(entity) || entity.getLocation().distanceSquared(targetLocation) > _radiusSquared)
                                continue;
                            LivingEntity target = (LivingEntity) entity;
                            if (!damageCheck(player, target))
                                continue;

                            addSpellTarget(target, hero);
                            damageEntity(target, player, _damage, DamageCause.FIRE);
                            hitEnemies.add(entity);
                        }
                    }
                }, numBlocks * _delay);

                numBlocks++;
            }
        }
    }

    private double ApplyAoEDiminishingReturns(double damage, double diminishPercent, double maxDiminishPercent, int maxTargetsBeforeDiminish, int numberOfTargets) {
        final int hitCountBeforeDiminishing = 3;
        final double aoeDiminishingReturnsPercent = 0.15;

        if (numberOfTargets > maxTargetsBeforeDiminish) {
            double totalDiminishPercent = (aoeDiminishingReturnsPercent * numberOfTargets);
            if (totalDiminishPercent > maxDiminishPercent)
                totalDiminishPercent = maxDiminishPercent;
            return totalDiminishPercent / damage * 100;
        } else {
            return damage;
        }
    }
}
