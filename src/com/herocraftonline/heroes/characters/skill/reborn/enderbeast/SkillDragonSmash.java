package com.herocraftonline.heroes.characters.skill.reborn.enderbeast;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
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

    private List<FallingBlock> fallingBlocks = new ArrayList<FallingBlock>();

    public SkillDragonSmash(Heroes plugin) {
        super(plugin, "DragonSmash");
        setDescription("You launch into the air and then smash down into the ground. Enemies in a $1 radius of the landing are dealt $2 damage.");
        setUsage("/skill dragonsmash");
        setArgumentRange(0, 0);
        setIdentifiers("skill dragonsmash");
        setTypes(SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.VELOCITY_INCREASING);

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 100.0, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.0, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        return getDescription().replace("$1", radius + "").replace("$2", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE.node(), 80.0);
        node.set("damage-per-block-height", 10.0);
        node.set("maximum-total-damage-increase-for-blocks", 80);
        node.set(SkillSetting.RADIUS.node(), 5);
        node.set("upwards-velocity", 1.0);
        node.set("downwards-velocity", 1.0);
        node.set("transform-jump-velocity-difference", 1.0);
        node.set("delay-ticks-before-drop", 10);
        node.set("target-horizontal-knockback", 1.0);
        node.set("target-vertical-knockback", 1.0);
        return node;
    }

    @Override
    public SkillResult use(final Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        double vTransformPower = SkillConfigManager.getUseSetting(hero, this, "transform-jump-velocity-difference", 1.0, false);
        if (!hero.hasEffect("EnderBeastTransformed"))
            vTransformPower = 0.0;  // Negate it if we aren't transformed.

        final double vPowerUp = SkillConfigManager.getUseSetting(hero, this, "upwards-velocity", 1.0, false) + vTransformPower;
        final double vPowerDown = SkillConfigManager.getUseSetting(hero, this, "downwards-velocity", 1.0, false) + vTransformPower;
        final int delayTicksBeforeDrop = SkillConfigManager.getUseSetting(hero, this, "delay-ticks-before-drop", 10, false);

        broadcastExecuteText(hero);

        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 0.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.5f, 0.5f);

        Vector currentVelocity = player.getVelocity();
        player.setVelocity(new Vector(currentVelocity.getX(), currentVelocity.getY() + vPowerUp, currentVelocity.getZ()));

        BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
        scheduler.runTaskLaterAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                player.setFallDistance(-512);
                player.setVelocity(new Vector(0, -vPowerDown, 0));

                DragonSmashUpdateTask updateTask = new DragonSmashUpdateTask(scheduler, hero);
                updateTask.setTaskId(scheduler.scheduleSyncRepeatingTask(plugin, updateTask, 1L, 1L));
            }
        }, delayTicksBeforeDrop);
        return SkillResult.NORMAL;
    }

    private class DragonSmashUpdateTask implements Runnable {
        private final Hero hero;
        private final BukkitScheduler scheduler;
        private final double topYValue;
        private int taskId;

        DragonSmashUpdateTask(BukkitScheduler scheduler, Hero hero) {
            this.scheduler = scheduler;
            this.hero = hero;
            this.topYValue = hero.getPlayer().getLocation().getY();
        }

        public void setTaskId(int id) {
            this.taskId = id;
        }

        @Override
        public void run() {
            if (hero.getPlayer().isOnGround()) {
                if (taskId != 0 && scheduler.isCurrentlyRunning(taskId)) {
                    scheduler.cancelTask(taskId);
                    taskId = 0;
                    smash(hero, topYValue);
                } else {
                    Heroes.log(Level.WARNING, "SkillDragonSmash: Bad Coder alert! Somebody is not cancelling a bukkit task properly. This is gonna cause some big lag eventually.");
                }
            }
        }
    }

    private void smash(final Hero hero, double topYValue) {
        final Player player = hero.getPlayer();
        final Location loc = player.getLocation().getBlock().getLocation().clone(); // We want the block loc, not the actual player loc.
        final SkillDragonSmash skill = this;

        final double baseDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 80.0, false);
        final double damagePerBlockHeight = SkillConfigManager.getUseSetting(hero, this, "damage-per-block-height", 10.0, false);
        final double maxDamageGain = SkillConfigManager.getUseSetting(hero, this, "maximum-total-damage-increase-for-block", 80.0, false);
        final double hPower = SkillConfigManager.getUseSetting(hero, this, "target-horizontal-knockback", 0.5, false);
        final double vPower = SkillConfigManager.getUseSetting(hero, this, "target-vertical-knockback", 0.5, false);
        final int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

        Location playerLoc = player.getLocation();
        int yDistance = (int) (topYValue - loc.getY());

        double damageGain = yDistance * damagePerBlockHeight;
        if (damageGain > maxDamageGain)
            damageGain = maxDamageGain;

        final double damage = baseDamage + damageGain;

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

        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_HURT, 1f, 1f);
        new BukkitRunnable() {
            int i = 1;

            @Override
            public void run() {
                if (i > radius) {
                    this.cancel();
                    return;
                }

                for (Location l : Util.getCircleLocationList(loc, radius, radius, false, false, -2)) {
                    Block block = l.getBlock();
                    Block aboveBlock = block.getRelative(BlockFace.UP);
                    if (!block.getType().isSolid() || !aboveBlock.isEmpty())
                        continue;

                    FallingBlock fb = loc.getWorld().spawnFallingBlock(block.getLocation().clone().add(0, 1.25f, 0), block.getType(), block.getData());
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
