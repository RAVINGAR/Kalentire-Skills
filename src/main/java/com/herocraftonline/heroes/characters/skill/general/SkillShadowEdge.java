package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.RootEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Donal on 06/01/2017.
 */
public class SkillShadowEdge extends ActiveSkill implements Listener {
    private final List<ThrownAxe> axes;

    public SkillShadowEdge(final Heroes plugin) {
        super(plugin, "ShadowEdge");
        setDescription("Throw out a dagger, when it hits a target you teleport to it.");
        setUsage("/skill ShadowEdge");
        setArgumentRange(0, 0);
        setIdentifiers("skill ShadowEdge");
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.ARMOR_PIERCING, SkillType.AGGRESSIVE);

        axes = new ArrayList<>();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new ThrowAxeAxeTask(this), 0, 1);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set("weapons", Util.axes);
        node.set(SkillSetting.DAMAGE.node(), 50);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 0.75);

        node.set("axe-throw-multiplier", 0.5);
        node.set("axe-throw-multiplier-per-dexterity", 0.1);

        node.set("launch-target", true);
        node.set("launch-target-multiplier", 1.4);
        node.set("launch-target-up-additive", 1.0);
        node.set(SkillSetting.DAMAGE.node(), 45);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 1.375);
        node.set("weapons", Util.shovels);
        node.set("ncp-exemption-duration", 1000);
        node.set("duration", 2000);
        return node;
    }

    @Override
    public String getDescription(final Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        final double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.75, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        return getDescription().replace("$1", damage + "");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero);
        final Material itemType = Material.DIAMOND_SWORD;

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        final double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        double axeThrowMultiplier = SkillConfigManager.getUseSetting(hero, this, "axe-throw-multiplier", 0.9, false);
        final double axeThrowMultiplierIncrease = SkillConfigManager.getUseSetting(hero, this, "axe-throw-multiplier-per-dexterity", 0.1, false);
        axeThrowMultiplier += axeThrowMultiplierIncrease * hero.getAttributeValue(AttributeType.DEXTERITY);

        final Item dropItem = player.getWorld().dropItem(player.getEyeLocation(), new ItemStack(itemType, 1));
        dropItem.setPickupDelay(-1);
        dropItem.setVelocity(player.getEyeLocation().getDirection().normalize().multiply(axeThrowMultiplier));

        axes.add(new ThrownAxe(dropItem, hero, damage));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (dropItem.isValid()) {
                // No need to remove disarm here, it has its own expiry timer
                dropItem.remove();
            }
        }, 3); // 1 sec after thrown, pull it back
        // 5 is 20 blocks
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemPickup(final EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            final Player player = (Player) event.getEntity();
            final Item item = event.getItem();
            for (final ThrownAxe axe : axes) {

                if (axe.getItem().equals(item)) {
                    event.setCancelled(true);
                    final Hero hero = axe.getOwner();
                    if (player == hero.getPlayer() && item.getTicksLived() > 3) {
                        item.remove();
                    }
                }
            }
        }
    }

    // Safety measure, likely unnecessary
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(final PluginDisableEvent e) {

        if (e.getPlugin() != plugin) {
            return;
        }

        for (final ThrownAxe axe : axes) {
            axe.getItem().remove();
        }
        axes.clear();
    }

    public static class ThrownAxe {
        private final Item item;
        private final Hero owner;
        private final double hitDamage;
        //private Vector targetThrowVelocity;
        private final List<LivingEntity> hitTargets;

        //, Vector throwVelocity
        public ThrownAxe(final Item axe, final Hero thrower, final double damage) {
            item = axe;
            owner = thrower;
            hitDamage = damage;
            hitTargets = new ArrayList<>();
        }

        public Item getItem() {
            return item;
        }

        public Hero getOwner() {
            return owner;
        }

        public double getHitDamage() {
            return hitDamage;
        }

        public List<LivingEntity> getHitTargets() {
            return hitTargets;
        }
    }

    private class ThrowAxeAxeTask implements Runnable {

        private final Skill skill;

        public ThrowAxeAxeTask(final Skill skill) {
            this.skill = skill;
        }

        @Override
        public void run() {
            final Iterator<ThrownAxe> axeIter = axes.iterator();
            while (axeIter.hasNext()) {
                final ThrownAxe axe = axeIter.next();
                final Item axeItem = axe.getItem();

                if (!axeItem.isValid()) { // The delayed task above handles axe timeout and removal, so we just need to kill it from the list after that
                    axeIter.remove();
                    continue;
                }

                final Hero hero = axe.getOwner();
                final Player player = hero.getPlayer();
                for (final Entity entity : axeItem.getNearbyEntities(1, 1, 1)) {
                    if (!(entity instanceof LivingEntity)) {
                        continue;
                    }

                    final LivingEntity target = (LivingEntity) entity;
                    if (axe.getHitTargets().contains(target) || !damageCheck(player, target)) {
                        continue;
                    }

                    axe.getHitTargets().add(target);
                    final Location playerLoc = player.getLocation();
                    final Location targetLoc = target.getLocation().clone();
                    targetLoc.setPitch(0);      // Reset pitch so that we don't have to worry about it.

                    BlockIterator iter = null;
                    try {
                        final Vector direction = targetLoc.getDirection().multiply(-1);
                        final int blocksBehindTarget = 1;
                        iter = new BlockIterator(target.getWorld(), targetLoc.toVector(), direction, 0, blocksBehindTarget);
                    } catch (final IllegalStateException e) {
                        player.sendMessage("There was an error getting the ShadowEdge location!");
                        return;
                    }

                    Block prev = null;
                    Block b;
                    while (iter.hasNext()) {
                        b = iter.next();
                        //player.sendMessage("Looping through blocks. Current Block: " + b.getType().toString());      // DEBUG

                        // Validate blocks near destination
                        if (Util.transparentBlocks.contains(b.getType()) && (Util.transparentBlocks.contains(b.getRelative(BlockFace.UP).getType()) || Util.transparentBlocks.contains(b.getRelative(BlockFace.DOWN).getType()))) {
                            prev = b;
                        } else {
                            break;
                        }
                    }
                    if (prev != null) {
                        final Location targetTeleportLoc = prev.getLocation().clone();
                        targetTeleportLoc.add(new Vector(.5, 0, .5));

                        // Set the blink location yaw/pitch to that of the target
                        targetTeleportLoc.setPitch(0);
                        targetTeleportLoc.setYaw(targetLoc.getYaw());
                        player.teleport(targetTeleportLoc);
                        final CharacterTemplate targCT = plugin.getCharacterManager().getCharacter(target);
                        final long duration = SkillConfigManager.getUseSetting(hero, this.skill, SkillSetting.DURATION, 1000, false);
                        final RootEffect root = new RootEffect(this.skill, hero.getPlayer(), 1, duration);
                        targCT.addEffect(root);
                        //plugin.getCharacterManager().getCharacter(target).addEffect(new RootEffect(skill, hero.getPlayer(), 500));

                        //plugin.getCharacterManager().getCharacter(target).addEffect(new StunEffect(this, player, duration));
                        player.getWorld().playEffect(playerLoc, Effect.ENDER_SIGNAL, 3);
                        player.getWorld().playSound(playerLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8F, 1.0F);
                    }


                    addSpellTarget(target, hero);
                    damageEntity(target, player, axe.getHitDamage(), EntityDamageEvent.DamageCause.ENTITY_ATTACK);

                    //player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), org.bukkit.Effect.CRIT, 0, 0, 0, 0, 0, 1, 25, 16);
                    player.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 0.5, 0), 25, 0, 0, 0, 1);
                    player.getWorld().playEffect(player.getLocation(), org.bukkit.Effect.BLAZE_SHOOT, 3);

                    axeItem.remove();
                    return;
                }
            }
        }
    }
}

