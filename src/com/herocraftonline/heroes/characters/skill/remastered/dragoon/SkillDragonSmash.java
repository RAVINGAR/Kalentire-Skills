package com.herocraftonline.heroes.characters.skill.remastered.dragoon;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.SafeFallEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.GeometryUtil;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

public class SkillDragonSmash extends ActiveSkill implements Listener {

    private static String launchedToggleableDragonSmashEffectName = "DragonSmashLaunched";
    private static String droppingDragonSmashEffectName = "DragonSmashDrop";
    private List<FallingBlock> fallingBlocks = new ArrayList<FallingBlock>();

    public SkillDragonSmash(Heroes plugin) {
        super(plugin, "DragonSmash");
        setDescription("Launch high into the air, if recast, crash back to the ground knocking back all " +
                "enemies within a $1 block radius and dealing $2 damage to each.");
        setUsage("/skill dragonsmash");
        setArgumentRange(0, 0);
        setIdentifiers("skill dragonsmash");
        setTypes(SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.VELOCITY_INCREASING);

        setToggleableEffectName(launchedToggleableDragonSmashEffectName);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double radius = SkillConfigManager.getUseSettingDouble(hero, this, SkillSetting.RADIUS, false);
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(radius))
                .replace("$2", Util.decFormat.format(damage));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE.node(), 60.0);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 1);
        node.set(SkillSetting.RADIUS.node(), 5.0);
        node.set("upwards-velocity", 1.25);
        node.set("downwards-velocity", 2.5);
        node.set("stop-jump-delay-ticks", 10);
        node.set("allow-drop-for-ms", 5000);
        node.set("safefall-duration", 5000);
        node.set("target-horizontal-knockback", 0.5);
        node.set("target-vertical-knockback", 1.0);
        return node;
    }

    // Run on recast after jump and not yet hit ground -> do Drop
    @Override
    protected void onSkillEffectToggle(Hero hero) {
        if (hero.hasEffect(launchedToggleableDragonSmashEffectName)) {
            doDrop(hero);
        }
        super.onSkillEffectToggle(hero); // default just removes the toggleable effect (if they have it)
    }

    @Override
    public SkillResult use(final Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        if (hero.hasEffect(droppingDragonSmashEffectName))
            return SkillResult.INVALID_TARGET_NO_MSG; // Drop only once

        broadcastExecuteText(hero);
        final int stopJumpDelayTicks = SkillConfigManager.getUseSettingInt(hero, this, "stop-jump-delay-ticks",  false);

        final double vPowerUp = SkillConfigManager.getUseSettingDouble(hero, this, "upwards-velocity", false);
        int allowDropForMilliseconds = SkillConfigManager.getUseSettingInt(hero, this, "allow-drop-for-ms", false);
        hero.addEffect(new LaunchedDragonSmashEffect(this, player, allowDropForMilliseconds));

        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 2, 1);

        // Add SafeFall
        int safeFallDuration = SkillConfigManager.getUseSettingInt(hero, this,"safefall-duration", false);
        hero.addEffect(new SafeFallEffect(this,"DragonSmashSafeFall", player, safeFallDuration, null, null));

        // Do Jump
        Vector currentVelocity = player.getVelocity();
        player.setVelocity(new Vector(currentVelocity.getX(), currentVelocity.getY() + vPowerUp, currentVelocity.getZ()));
        runPeriodicCloudEffects(player, stopJumpDelayTicks);

        Bukkit.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                // Only negate velocity (to stop jump and hence should start falling) and protect from the fall?
                if (!hero.hasEffect(droppingDragonSmashEffectName)) {
                    // seems to protect from fall damage, since damage would be based off this?
                    // Edit: This does not protect from fall, though wish it worked like that
                    player.setFallDistance(-512F);

                    // negate previous jump velocity, will free fall now
                    final Vector v = player.getVelocity();
                    player.setVelocity(new Vector(v.getX(), 0, v.getZ()));
                }
            }
        }, stopJumpDelayTicks);
        return SkillResult.NORMAL;
    }

    public void runPeriodicCloudEffects(Player player, int ticksToRunFor) {
        new BukkitRunnable() {
            private final int lastTick = ticksToRunFor;
            private int ticks = 0;

            @Override
            public void run() {
                if (ticks >= lastTick) {
                    cancel();
                    return;
                }

                //player.getWorld().spigot().playEffect(player.getLocation(), Effect.CLOUD);
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 1, 0, 0, 0, 1);
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1L);
    }

    private class LaunchedDragonSmashEffect extends ExpirableEffect {
        LaunchedDragonSmashEffect(Skill skill, Player player, long duration) {
            super(skill, launchedToggleableDragonSmashEffectName, player, duration);
        }
    }

    private class DroppingDragonSmashEffect extends Effect {
        DroppingDragonSmashEffect(Skill skill, Player player) {
            super(skill, droppingDragonSmashEffectName, player);
        }
    }

    public void doDrop(Hero hero) {
        final Player player = hero.getPlayer();

        if (hero.getPlayer().isOnGround()) {
            // Already on ground, don't let them aoe or drop
            if (hero.hasEffect(launchedToggleableDragonSmashEffectName)) {
                hero.removeEffect(hero.getEffect(launchedToggleableDragonSmashEffectName));
            }
            return;
        }

        final double vPowerDown = SkillConfigManager.getUseSettingDouble(hero, this, "downwards-velocity", false);

        // seems to protect from fall damage, since damage would be based off this?
        // Edit: This does not protect from fall, though wish it worked like that
        player.setFallDistance(-512F);

        final Vector v = player.getVelocity();
        player.setVelocity(new Vector(v.getX(), -vPowerDown, v.getZ())); // drop with speed
        hero.addEffect(new DroppingDragonSmashEffect(this, player));

        BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
        DragonSmashUpdateTask updateTask = new DragonSmashUpdateTask(scheduler, hero);
        updateTask.setTaskId(scheduler.scheduleSyncRepeatingTask(plugin, updateTask, 1L, 1L));
    }

    // TODO add timeout for the task this has been handled, just in case
    private class DragonSmashUpdateTask implements Runnable {
        private final Hero hero;
        private final BukkitScheduler scheduler;
        private int taskId;

        DragonSmashUpdateTask(BukkitScheduler scheduler, Hero hero) {
            this.scheduler = scheduler;
            this.hero = hero;
        }

        public void setTaskId(int id) {
            this.taskId = id;
        }

        @Override
        public void run() {
            final Player player = hero.getPlayer();
            final boolean dead = player.isDead();
            // Note death handling as its a good idea and well for cases such as if they fell in the void, this could be running for a while
            if (dead || player.isOnGround()) {
                if (taskId != 0 && scheduler.isCurrentlyRunning(taskId)) {
                    scheduler.cancelTask(taskId);
                    taskId = 0;
                    if (dead) {
                        hero.removeEffect(hero.getEffect(droppingDragonSmashEffectName));
                    } else {
                        smash(hero);
                    }
                } else {
                    Heroes.log(Level.WARNING, "SkillDragonSmash: Bad Coder alert! Somebody is not cancelling a bukkit task properly. This is gonna cause some big lag eventually.");
                }
            }
        }
    }

    private void smash(final Hero hero) {
        final Player player = hero.getPlayer();
        final Location loc = player.getLocation().getBlock().getLocation().clone(); // We want the block loc, not the actual player loc.
        final SkillDragonSmash skill = this;

        final double damage = SkillConfigManager.getUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        final double hPower = SkillConfigManager.getUseSettingDouble(hero, this, "target-horizontal-knockback", false);
        final double vPower = SkillConfigManager.getUseSettingDouble(hero, this, "target-vertical-knockback", false);
        final double radius = SkillConfigManager.getUseSettingDouble(hero, this, SkillSetting.RADIUS, false);

        Location playerLoc = player.getLocation();

        List<Entity> entities = player.getNearbyEntities(radius, radius, radius);
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity))
                continue;
            LivingEntity target = (LivingEntity) entity;
            if (!damageCheck(player, target))
                continue;

            plugin.getDamageManager().addSpellTarget(target, hero, skill);
            damageEntity(target, player, damage);

            Location targetLoc = target.getLocation();

            double xDir = targetLoc.getX() - playerLoc.getX();
            double zDir = targetLoc.getZ() - playerLoc.getZ();
            double magnitude = Math.sqrt(xDir * xDir + zDir * zDir);

            xDir = xDir / magnitude * hPower;
            zDir = zDir / magnitude * hPower;

            final Vector velocity = new Vector(xDir, vPower, zDir);
            target.setVelocity(velocity);
        }

        final World world = player.getWorld();
        world.playSound(loc, Sound.ENTITY_GENERIC_HURT, 1f, 1f);
        new BukkitRunnable() {
            int i = 1;

            @Override
            public void run() {
                if (i > radius) {
                    hero.removeEffect(hero.getEffect(droppingDragonSmashEffectName));
                    this.cancel();
                    return;
                }

                for (Location l : GeometryUtil.getPerfectCircle(loc, (int) radius, (int) radius, false, false, -2)) {
                    Block block = l.getBlock();
                    Block aboveBlock = block.getRelative(BlockFace.UP);
                    if (!block.getType().isSolid() || !aboveBlock.isEmpty())
                        continue;

                    //FallingBlock fb = world.spawnFallingBlock(block.getLocation().clone().add(0, 1.25f, 0), block.getType(), block.getData());
                    FallingBlock fb = world.spawnFallingBlock(block.getLocation().clone().add(0, 1.25f, 0), block.getBlockData());
                    fb.setVelocity(new Vector(0, 0.3f, 0));
                    fb.setDropItem(false);
                    fallingBlocks.add(fb);
                }
                i++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    @EventHandler
    public void onBlockChangeState(EntityChangeBlockEvent event) {
        Entity ent = event.getEntity();
        if (!(ent instanceof FallingBlock))
            return;

        FallingBlock fb = (FallingBlock) event.getEntity();
        if (fallingBlocks.contains(fb)) {
            event.setCancelled(true);
            fallingBlocks.remove(fb);
            fb.getWorld().playSound(fb.getLocation(), Sound.BLOCK_STONE_STEP, 0.4f, 0.4f);
            fb.remove();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent e) {
        if (e.getPlugin() != plugin) {
            return;
        }

        Iterator<FallingBlock> iter = fallingBlocks.iterator();
        while (iter.hasNext()) {
            FallingBlock block = iter.next();
            block.remove();
            iter.remove();
        }
    }
}
