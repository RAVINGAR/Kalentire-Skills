package com.herocraftonline.heroes.characters.skill.reborn.badpyromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.CombustEffect;
import com.herocraftonline.heroes.characters.skill.*;

import org.bukkit.*;
import org.bukkit.Sound;
import org.bukkit.block.Block;
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

import java.util.LinkedHashMap;
import java.util.Map;

public class SkillFireball extends ActiveSkill {

    private Map<Snowball, Long> fireballs = new LinkedHashMap<Snowball, Long>(100) {
        private static final long serialVersionUID = 4329526013158603250L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<Snowball, Long> eldest) {
            return (size() > 60 || eldest.getValue() + 5000 <= System.currentTimeMillis());
        }
    };

    public SkillFireball(Heroes plugin) {
        super(plugin, "Fireball");
        setDescription("You shoot a ball of fire that deals $1 damage and lights whatever it hits on fire");
        setUsage("/skill fireball");
        setArgumentRange(0, 0);
        setIdentifiers("skill fireball");
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
        config.set(SkillSetting.DAMAGE.node(), 80);
        config.set("velocity", 1.5);
        config.set("fire-ticks", 40);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        double vel = SkillConfigManager.getUseSetting(hero, this, "velocity", 1.5, false);

        Snowball projectile = player.launchProjectile(Snowball.class);
        double originalLength = projectile.getVelocity().length();
        if (args.length == 1 && args[0].equals("1")) {
            Vector newVelocity = projectile.getVelocity();
            projectile.setVelocity(newVelocity);
        } else if (args.length == 1 && args[0].equals("2")) {
            Vector newVelocity = projectile.getVelocity()
                    .normalize()
                    .multiply(originalLength);
            projectile.setVelocity(newVelocity);
        } else if (args.length == 1 && args[0].equals("3")) {
            Vector newVelocity = projectile.getVelocity().multiply(vel);
            projectile.setVelocity(newVelocity);
        } else if (args.length == 1 && args[0].equals("4")) {
            Vector newVelocity = projectile.getVelocity()
                    .normalize()
                    .multiply(originalLength * vel);
            projectile.setVelocity(newVelocity);
        } else {
            Vector newVelocity = projectile.getVelocity()
                    .normalize()
                    .multiply(vel);
            projectile.setVelocity(newVelocity);
        }

        projectile.setFireTicks(100);
        fireballs.put(projectile, System.currentTimeMillis());
        projectile.setShooter(player);

        broadcastExecuteText(hero);

        player.getWorld().playEffect(player.getLocation(), Effect.BLAZE_SHOOT, 64, 2);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 1);

        return SkillResult.NORMAL;
    }

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
            if ((!(projectile.getShooter() instanceof Player)) || !fireballs.containsKey(projectile))
                return;

            if (event.getHitBlock() != null) {
                Block hitBlock = event.getHitBlock();

                final Block fireBlock = hitBlock.getRelative(event.getHitBlockFace());
                if (fireBlock.isEmpty())
                    fireBlock.setType(Material.FIRE);

                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        if (fireBlock.getType() == Material.FIRE)
                            fireBlock.setType(Material.AIR);
                    }
                }, 20 * 5);
            }

            if (event.getHitEntity() == null) {
                fireballs.remove(projectile);
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageByEntityEvent event) {
            if (!(event.getEntity() instanceof LivingEntity)) {
                return;
            }

            Entity projectile = event.getDamager();
            if (!(projectile instanceof Snowball) || !fireballs.containsKey(projectile)) {
                return;
            }

            fireballs.remove(projectile);
            event.setCancelled(true);

            LivingEntity targetLE = (LivingEntity) event.getEntity();
            ProjectileSource source = ((Projectile) event.getDamager()).getShooter();
            if (!(source instanceof Player))
                return;

            Player dmger = (Player) source;
            if (!damageCheck((Player) dmger, targetLE))
                return;

            Hero hero = plugin.getCharacterManager().getHero((Player) dmger);

            targetLE.setFireTicks(SkillConfigManager.getUseSetting(hero, skill, "fire-ticks", 40, false));
            plugin.getCharacterManager().getCharacter(targetLE).addEffect(new CombustEffect(skill, (Player) dmger));

            double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 80, false);
            addSpellTarget(targetLE, hero);
            damageEntity(targetLE, hero.getPlayer(), damage, DamageCause.MAGIC);

            targetLE.getWorld().spawnParticle(Particle.FLAME, targetLE.getLocation(), 50, 0.2F, 0.7F, 0.2F, 16);
            targetLE.getWorld().playSound(targetLE.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 7.0F, 1.0F);
        }
    }
}