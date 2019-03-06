package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.StunEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;

import static com.herocraftonline.heroes.util.GeometryUtil.circle;


public class SkillCombustion extends ActiveSkill {
    private HashMap<SmallFireball, Player> fireballs = new HashMap<SmallFireball, Player>();

    public SkillCombustion(Heroes plugin) {
        super(plugin, "Combustion");
        setDescription("You unleash a wave of destruction, hurling 18 fireballs in a ring around you. Targets hit will be dealt $1 damage and stunned for 1.5 second(s).");
        setUsage("/skill combustion");
        setArgumentRange(0, 0);
        setIdentifiers("skill combustion");
        setTypes(SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_FIRE);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE.node(), 20);
        node.set(SkillSetting.DAMAGE_INCREASE.node(), 0.75);
        node.set("velocity-multiplier", 1.5);
        node.set("fire-ticks", 100);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        final double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 20, false) + ((SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0.75, false) * hero.getHeroLevel(this)));

        Player player = hero.getPlayer();
        Location launchLoc = player.getLocation().add(0, 0.5, 0);
        //ArrayList<Location> launchLocs = circle(player.getLocation().add(0, 0.5, 0), (int) 1.5, 1);
        ArrayList<Location> ring = circle(player.getLocation().add(0, 0.5, 0), 18, 1);
        for (Location destination : ring) {
            //Location destination = ring.get(1);
            double dX = launchLoc.getX() - destination.getX();
            double dY = launchLoc.getY() - destination.getY();
            double dZ = launchLoc.getZ() - destination.getZ();
            double yaw = Math.atan2(dZ, dX);
            double pitch = Math.atan2(Math.sqrt(dZ * dZ + dX * dX), dY) + Math.PI;
            double X = Math.sin(pitch) * Math.cos(yaw);
            double Y = Math.sin(pitch) * Math.sin(yaw);
            double Z = Math.cos(pitch);

            final Vector velocity = new Vector(X, Z, Y);
            // Thank you Google for this. I couldn't have gotten the pitch/yaw part XD

            SmallFireball fireball = player.launchProjectile(SmallFireball.class);

            fireball.setVelocity(velocity);
            fireball.setIsIncendiary(false);

            fireballs.put(fireball, player);

            final SmallFireball f = fireball;
            final Player p = player;
            final Hero h = hero;

            new BukkitRunnable() // velocity check, 4 times
            {
                private int effectTicks = 0;

                public void run() {
                    if (f.isDead()) cancel();
                    if (!fireballs.containsKey(f)) {
                        f.remove();
                        cancel();
                    }

                    if (effectTicks < 4) {
                        f.setVelocity(velocity);
                        effectTicks++;
                    } else {
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0, 5);

            final Skill skill = this;

            new BukkitRunnable() // fireball exist timer
            {
                private int effectTicks = 0;

                public void run() {
                    if (f.isDead()) cancel();
                    if (!fireballs.containsKey(f)) cancel();

                    if (effectTicks < 1) {
                        effectTicks++;
                    } else {
                        f.remove();
                        fireballs.remove(f);
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 1, 20);

        }
        //player.getWorld().spigot().playEffect(player.getLocation().add(0, 0.5, 0), Effect.FLAME, 0, 0, 0.5F, 0.2F, 0.5F, 0.6F, 50, 16);
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 0.5, 0), 50, 0.5, 0.2, 0.5, 0.6);
        //player.getWorld().spigot().playEffect(player.getLocation(), Effect.LAVA_POP, 0, 0, 0.5F, 0.2F, 0.5F, 0.6F, 25, 16);
        player.getWorld().spawnParticle(Particle.LAVA, player.getLocation(), 25, 0.5, 0.2, 0.5, 0.6);
        //player.getWorld().spigot().playEffect(player.getLocation(), Effect.LARGE_SMOKE, 0, 0, 0.5F, 0.2F, 0.5F, 0.6F, 25, 16);
        player.getWorld().spawnParticle(Particle.SMOKE_LARGE, player.getLocation(), 25, 0.5, 0.2, 0.5, 0.6);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.3F, 1.0F);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_TNT_PRIMED, 1.0F, 0.6F);
        player.getWorld().playEffect(player.getLocation(), Effect.BLAZE_SHOOT, 2);
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {

        private final Skill skill;
        private final ArrayList<LivingEntity> damagedEntities = new ArrayList<LivingEntity>();

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled() || !(event instanceof EntityDamageByEntityEvent) || !(event.getEntity() instanceof LivingEntity)) {
                return;
            }

            EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
            Entity projectile = subEvent.getDamager();
            if (!(projectile instanceof SmallFireball) || !fireballs.containsKey(projectile) || subEvent.getEntity() instanceof SmallFireball) {
                return;
            }
            if (fireballs.containsKey(projectile) && event.getEntity() == fireballs.get(projectile)) {
                event.setCancelled(true);
                return;
            }
            SmallFireball fireball = (SmallFireball) projectile;
            LivingEntity entity = (LivingEntity) subEvent.getEntity();
            Entity dmger = (Entity) ((SmallFireball) projectile).getShooter();
            if (dmger instanceof Player) {
                Hero hero = plugin.getCharacterManager().getHero((Player) dmger);

                if (!damageCheck((Player) dmger, entity)) {
                    event.setCancelled(true);
                    return;
                }

                event.setCancelled(true);

                plugin.getCharacterManager().getCharacter(entity).addEffect(new StunEffect(skill, hero.getPlayer(), 1500));
                if (damagedEntities.contains(entity)) {
                    addSpellTarget(entity, hero);
                    damagedEntities.add(entity);
                    double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 20, false);
                    damage += (double) (SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE, 0.75, false) * hero.getHeroLevel(skill));
                    damageEntity(entity, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.MAGIC);
                    event.setCancelled(true);
                }
            }
        }

        @EventHandler
        public void noFireballFire(BlockIgniteEvent event) {
            if (event.getCause().equals(IgniteCause.FIREBALL)) event.setCancelled(true);
        }

        @EventHandler
        public void fireballHit(ProjectileHitEvent event) {
            if (!(event.getEntity() instanceof SmallFireball)) return;
            SmallFireball fireball = (SmallFireball) event.getEntity();
            if (!fireballs.containsKey(fireball)) return;
            else {
                //fireball.getWorld().spigot().playEffect(fireball.getLocation().add(0, 0.3, 0), Effect.LAVA_POP, 0, 0, 0.5F, 0.2F, 0.5F, 0.0F, 25, 16);
                fireball.getWorld().spawnParticle(Particle.LAVA, fireball.getLocation().add(0, 0.3, 0), 25, 0.5, 0.2, 0.5, 0);
                //fireball.getWorld().spigot().playEffect(fireball.getLocation().add(0, 0.3, 0), Effect.EXPLOSION_LARGE, 0, 0, 0.5F, 0.2F, 0.5F, 0.0F, 5, 16);
                fireball.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, fireball.getLocation().add(0, 0.3, 0), 5, 0.5, 0.2, 0.5, 0);
                fireball.getWorld().playSound(fireball.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.0F);
                fireballs.remove(fireball);
                fireball.remove();
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 20, false);
        damage += (int) (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0.75, false) * hero.getHeroLevel(this));
        return getDescription().replace("$1", damage + "");
    }
}
