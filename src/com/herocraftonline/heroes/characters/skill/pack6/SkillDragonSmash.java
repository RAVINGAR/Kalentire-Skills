package com.herocraftonline.heroes.characters.skill.pack6;

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
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SkillDragonSmash extends ActiveSkill implements Listener {

    List<Hero> activeHeroes = new ArrayList<>();
    List<FallingBlock> fallingBlocks = new ArrayList<>();

    public SkillDragonSmash(Heroes plugin) {
        super(plugin, "DragonSmash");
        setDescription("You launch into the air, and smash into the ground. Enemies in a $1 radius of the landing are dealt $2 damage.");
        setUsage("/skill dragonsmash");
        setArgumentRange(0, 0);
        setIdentifiers("skill dragonsmash");
        setTypes(SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.VELOCITY_INCREASING, SkillType.SILENCEABLE);

        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new DragonSmashUpdateTask(), 0, 1);
    }

    @Override
    public String getDescription(Hero hero) {
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 100, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        return getDescription().replace("$1", radius + "").replace("$2", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), 100);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 1);

        node.set(SkillSetting.RADIUS.node(), 5);

        node.set("user-launch", 0.5);
        node.set("user-launch-per-dexterity", 0.1);
        node.set("user-drop", -0.5);
        node.set("user-drop-per-dexterity", -0.1);

        node.set("target-launch", 0.5);

        return node;
    }

    @Override
    public SkillResult use(final Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        double tempUpVelocity = SkillConfigManager.getUseSetting(hero, this, "user-launch", 0.5, false);
        double upVelocityIncrease = SkillConfigManager.getUseSetting(hero, this, "user-launch-per-dexterity", 0.1, false);
        final double upVelocity = tempUpVelocity + (upVelocityIncrease * hero.getAttributeValue(AttributeType.DEXTERITY));

        double tempDownVelocity = SkillConfigManager.getUseSetting(hero, this, "user-drop", -0.5, false);
        double downVelocityIncrease = SkillConfigManager.getUseSetting(hero, this, "user-drop-per-dexterity", -0.1, false);
        final double downVelocity = tempDownVelocity + (downVelocityIncrease * hero.getAttributeValue(AttributeType.DEXTERITY));

        broadcastExecuteText(hero);

        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_LAUNCH, 2, 1);
        player.setVelocity(new Vector(0, upVelocity, 0));
        final int taskId = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                //player.getWorld().spigot().playEffect(player.getLocation(), Effect.CLOUD);
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 1, 0, 0, 0, 1);
            }
        }, 0, 1).getTaskId();
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                Bukkit.getScheduler().cancelTask(taskId);
                player.setVelocity(new Vector(0, downVelocity, 0));
                player.setFallDistance(-512);
                activeHeroes.add(hero);
            }
        }, 25);
        return SkillResult.NORMAL;
    }

    private class DragonSmashUpdateTask implements Runnable {
        @Override
        public void run() {
            Iterator<Hero> heroes = activeHeroes.iterator();
            while(heroes.hasNext()) {
                Hero hero = heroes.next();
                if (hero.getPlayer().isOnGround()) {
                    heroes.remove();
                    playBoomEffect(hero);
                }
            }
        }
    }

    private void playBoomEffect(final Hero hero) {

        final Player player = hero.getPlayer();
        final Location loc = player.getLocation();
        final SkillDragonSmash skill = this;
        final double targetVelocity = SkillConfigManager.getUseSetting(hero, this, "target-launch", 0.5, false);
        final int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

        final List<Entity> hitEnemies = new ArrayList<>();

        double tempDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 100, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1, false);
        final double damage = tempDamage + (damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2, 1);
        new BukkitRunnable() {
            int i = 1;

            @Override
            public void run() {
                if (i > radius) {
                    cancel();
                }
                for (Block b : getBlocksInRadius(loc.clone().add(0, -1, 0), i, true)) {
                    if (b.getLocation().getBlockY() == loc.getBlockY() - 1) {
                        //TODO potentially make this section a Util.transparentBlocks
                        if (b.getType() != Material.AIR
                                //FIXME Will deal with this later, also may want to use nms physics for this as there is an easy method (will look into later).
                                //&& b.getType() != Material.SIGN_POST
                                && b.getType() != Material.CHEST
                                //&& b.getType() != Material.STONE_PLATE
                                //&& b.getType() != Material.WOOD_PLATE
                                && b.getType() != Material.WALL_SIGN
                                //&& b.getType() != Material.WALL_BANNER
                                //&& b.getType() != Material.STANDING_BANNER
                                //&& b.getType() != Material.CROPS
                                //&& b.getType() != Material.LONG_GRASS
                                //&& b.getType() != Material.SAPLING
                                && b.getType() != Material.DEAD_BUSH
                                //&& b.getType() != Material.RED_ROSE
                                && b.getType() != Material.RED_MUSHROOM
                                && b.getType() != Material.BROWN_MUSHROOM
                                && b.getType() != Material.TORCH
                                && b.getType() != Material.LADDER
                                && b.getType() != Material.VINE
//                                && b.getType() != Material.DOUBLE_PLANT
//                                && b.getType() != Material.PORTAL
                                && b.getType() != Material.CACTUS
                                && b.getType() != Material.WATER
//                                && b.getType() != Material.STATIONARY_WATER
                                && b.getType() != Material.LAVA
//                                && b.getType() != Material.STATIONARY_LAVA
                                && b.getType().isSolid() // Was an NMS call for 1.8 Spigot, this may not be as accurate
                                && b.getType().getId() != 43
                                && b.getType().getId() != 44
                                && Util.transparentBlocks.contains(b.getRelative(BlockFace.UP).getType())) {
                            FallingBlock fb = loc.getWorld().spawnFallingBlock(b.getLocation().clone().add(0, 1.1f, 0), b.getType(), b.getData());
                            fb.setVelocity(new Vector(0, 0.3f, 0));
                            fb.setDropItem(false);
                            fallingBlocks.add(fb);
                            for (Entity ent : fb.getNearbyEntities(1, 1, 1)) {
                                if (ent instanceof LivingEntity && ent != player && !hitEnemies.contains(ent) && damageCheck(player, (LivingEntity) ent)) {
                                    LivingEntity lEnt = (LivingEntity) ent;

                                    plugin.getDamageManager().addSpellTarget(lEnt, hero, skill);
                                    damageEntity(lEnt, player, damage);

                                    ent.setVelocity(new Vector(0, targetVelocity, 0));
                                    ent.setFallDistance(-512);

                                    hitEnemies.add(ent);
                                }
                            }
                        }
                    }
                }
                i++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    @EventHandler
    public void onBlockChangeState(EntityChangeBlockEvent event) {
        if (fallingBlocks.contains(event.getEntity())) {
            event.setCancelled(true);
            fallingBlocks.remove(event.getEntity());
            FallingBlock fb = (FallingBlock) event.getEntity();
            //fb.getWorld().spigot().playEffect(fb.getLocation(), Effect.TILE_BREAK, fb.getBlockId(), fb.getBlockData(), 0, 0, 0, 0.4f, 50, 128);
            fb.getWorld().spawnParticle(Particle.BLOCK_CRACK, fb.getLocation(), 50, 0, 0, 0, 0.4, fb.getBlockData());
            fb.getWorld().playSound(fb.getLocation(), Sound.BLOCK_STONE_STEP, 1, 1);
            event.getEntity().remove();
        }
    }

    // Safety measure, likely unnecessary
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent e) {

        if(e.getPlugin() != plugin) {
            return;
        }

        Iterator<FallingBlock> iter = fallingBlocks.iterator();
        while(iter.hasNext()) {
            FallingBlock block = iter.next();
            block.remove();

            iter.remove();
        }
    }

    public static List<Block> getBlocksInRadius(Location location, int radius, boolean hollow) {
        List<Block> blocks = new ArrayList<>();

        int bX = location.getBlockX();
        int bY = location.getBlockY();
        int bZ = location.getBlockZ();

        for (int x = bX - radius; x <= bX + radius; x++) {
            for (int y = bY - radius; y <= bY + radius; y++) {
                for (int z = bZ - radius; z <= bZ + radius; z++) {

                    double distance = ((bX - x) * (bX - x) + (bY - y) * (bY - y) + (bZ - z) * (bZ - z));

                    if (distance < radius * radius
                            && !(hollow && distance < ((radius - 1) * (radius - 1)))) {
                        Location l = new Location(location.getWorld(), x, y, z);
                        if (l.getBlock().getType() != Material.BARRIER)
                            blocks.add(l.getBlock());
                    }
                }

            }
        }

        return blocks;
    }
}
