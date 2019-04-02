package com.herocraftonline.heroes.characters.skill.reborn.pyromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.BurningEffect;
import com.herocraftonline.heroes.characters.skill.*;

import com.herocraftonline.heroes.util.Util;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

public class SkillFirebolt extends ActiveSkill {

    private Map<Snowball, Long> fireballs = new LinkedHashMap<Snowball, Long>(100) {
        private static final long serialVersionUID = 4329526013158603250L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<Snowball, Long> eldest) {
            return (size() > 60 || eldest.getValue() + 5000 <= System.currentTimeMillis());
        }
    };

    public SkillFirebolt(Heroes plugin) {
        super(plugin, "Firebolt");
        setDescription("You shoot a bolt of fire that ignites anything it hits. "
                + "The firebolt deals $1 damage and will ignite them, burning them for $2 fire tick damage over the next $3 second(s).");
        setUsage("/skill firebolt");
        setArgumentRange(0, 0);
        setIdentifiers("skill firebolt");
        setTypes(SkillType.ABILITY_PROPERTY_FIRE, SkillType.ABILITY_PROPERTY_PROJECTILE, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 80, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        int burnDuration = SkillConfigManager.getUseSetting(hero, this, "burn-duration", 2000, false);
        double burnMultipliaer = SkillConfigManager.getUseSetting(hero, this, "burn-damage-multiplier", 2.0, false);
        double totalBurnDamage = plugin.getDamageManager().calculateFireTickDamage((int) (burnDuration / 50), burnMultipliaer);

        String formattedDamage = Util.decFormat.format(damage);
        String formattedBurnDamage = Util.decFormat.format(totalBurnDamage);
        String formattedBurnDuration = Util.decFormat.format(burnDuration / 1000);
        return getDescription().replace("$1", formattedDamage).replace("$2", formattedBurnDamage).replace("$3", formattedBurnDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 80);
        config.set("projectile-velocity", 1.5);
        config.set("burn-duration", 2000);
        config.set("burn-damage-multiplier", 2.0);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        double projVel = SkillConfigManager.getUseSetting(hero, this, "projectile-velocity", 1.5, false);

        Snowball projectile = player.launchProjectile(Snowball.class);
        projectile.setVelocity(player.getLocation().getDirection().normalize().multiply(projVel).subtract(new Vector(0, 0.025, 0)));
        projectile.setFireTicks(100);

        fireballs.put(projectile, System.currentTimeMillis());
        projectile.setShooter(player);

        BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
//        VelocityDropRunnable runnable = new VelocityDropRunnable(scheduler, projectile);
//        runnable.setTaskId(scheduler.scheduleSyncRepeatingTask(plugin, runnable, 0L, 1L));

        broadcastExecuteText(hero);

        player.getWorld().playEffect(player.getLocation(), Effect.BLAZE_SHOOT, 64, 2);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 1);

        return SkillResult.NORMAL;
    }

    // Not used atm.
    private class VelocityDropRunnable implements Runnable {
        private final BukkitScheduler scheduler;
        private final Projectile projectile;

        private int taskId = 0;

        public VelocityDropRunnable(BukkitScheduler scheduler, Projectile projectile) {
            this.scheduler = scheduler;
            this.projectile = projectile;
        }

        public void setTaskId(int id) {
            this.taskId = id;
        }

        @Override
        public void run() {
            if (projectile.isDead()) {
                if (taskId != 0 && scheduler.isCurrentlyRunning(taskId)) {
                    scheduler.cancelTask(taskId);
                    taskId = 0;
                } else {
                    Heroes.log(Level.WARNING, "SkillFirebolt: Bad Coder alert! Somebody is not cancelling a bukkit task properly. This is gonna cause some big lag eventually.");
                }
            }

            projectile.setVelocity(projectile.getVelocity().subtract(new Vector(0, 0.025, 0)));
        }
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
                Util.setBlockOnFireIfAble(fireBlock);
            }

            if (event.getHitEntity() == null) {
                fireballs.remove(projectile);
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageByEntityEvent event) {
            Entity projectile = event.getDamager();
            if (!(projectile instanceof Snowball) || !fireballs.containsKey(projectile)) {
                return;
            }

            fireballs.remove(projectile);
            if (!(event.getEntity() instanceof LivingEntity))
                return;

            LivingEntity targetLE = (LivingEntity) event.getEntity();
            ProjectileSource dmgSource = ((Projectile) event.getDamager()).getShooter();
            if (!(dmgSource instanceof Player))
                return;

            Player player = (Player) dmgSource;
            Hero hero = plugin.getCharacterManager().getHero(player);

            if (!damageCheck(player, targetLE)) {
                event.setCancelled(true);
                return;
            }

            int burnDuration = SkillConfigManager.getUseSetting(hero, skill, "burn-duration", 2000, false);
            double burnMultipliaer = SkillConfigManager.getUseSetting(hero, skill, "burn-damage-multiplier", 2.0, false);
            double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 90, false);
            double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.0, false);
            damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

            addSpellTarget(targetLE, hero);
            damageEntity(targetLE, hero.getPlayer(), damage, DamageCause.MAGIC);

            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(targetLE);
            targetCT.addEffect(new BurningEffect(skill, player, burnDuration, burnMultipliaer));

            targetLE.getWorld().spawnParticle(Particle.FLAME, targetLE.getLocation(), 50, 0.2F, 0.7F, 0.2F, 16);
            targetLE.getWorld().playSound(targetLE.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 7.0F, 1.0F);

            event.setCancelled(true);
        }
    }
}