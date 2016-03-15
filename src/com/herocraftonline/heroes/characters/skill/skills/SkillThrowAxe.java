package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.DisarmEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

import java.util.*;
import java.util.logging.Level;

public class SkillThrowAxe extends ActiveSkill implements Listener {
    List<ThrownAxe> axes;

    public SkillThrowAxe(Heroes plugin) {
        super(plugin, "ThrowAxe");
        setDescription("Throw your axe, which deals $1 physical damage to all targets it hits. It returns after being thrown.");
        setUsage("/skill throwaxe");
        setArgumentRange(0, 0);
        setIdentifiers("skill throwaxe");
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.ARMOR_PIERCING, SkillType.AGGRESSIVE);

        axes = new ArrayList<>();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new ThrowAxeAxeTask(), 0, 1);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("weapons", Util.axes);
        node.set(SkillSetting.DAMAGE.node(), 50);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 0.75);

        node.set("axe-throw-multiplier", 0.5);
        node.set("axe-throw-multiplier-per-dexterity", 0.1);

        node.set("launch-target", true);
        node.set("launch-target-multiplier", 1.4);
        node.set("launch-target-up-additive", 1.0);

        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.75, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        return getDescription().replace("$1", damage + "");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        ItemStack item = player.getItemInHand();
        Material itemType = item.getType();
        int itemSlot = player.getInventory().getHeldItemSlot();

        if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.axes).contains(item.getType().name())) {
            Messaging.send(hero.getPlayer(), "You cannot use this skill with that weapon!");
            return SkillResult.FAIL;
        }

        broadcastExecuteText(hero);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        double axeThrowMultiplier = SkillConfigManager.getUseSetting(hero, this, "axe-throw-multiplier", 0.9, false);
        double axeThrowMultiplierIncrease = SkillConfigManager.getUseSetting(hero, this, "axe-throw-multiplier-per-dexterity", 0.1, false);
        axeThrowMultiplier += axeThrowMultiplierIncrease * hero.getAttributeValue(AttributeType.DEXTERITY);

        final Item dropItem = player.getWorld().dropItem(player.getEyeLocation(), new ItemStack(itemType, 1, (short) (itemType.getMaxDurability() - 1)));
        dropItem.setPickupDelay(0);
        dropItem.setVelocity(player.getEyeLocation().getDirection().multiply(axeThrowMultiplier));

        Vector hitVector;
        boolean launchTarget = SkillConfigManager.getUseSetting(hero, this, "launch-target", true);
        if (launchTarget) {
            double multiplier = SkillConfigManager.getUseSetting(hero, this, "launch-target-multiplier", 1.4, false);
            double upAdditive = SkillConfigManager.getUseSetting(hero, this, "launch-target-up-additive", 1.0, false);
            hitVector = player.getEyeLocation().getDirection().multiply(multiplier).add(new Vector(0, upAdditive, 0));
        }
        else
            hitVector = null;


        hero.addEffect(new ThrowAxeDisarmEffect(this, player, 3000));

        axes.add(new ThrownAxe(dropItem, hero, damage, hitVector));
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                dropItem.setVelocity(player.getEyeLocation().toVector().subtract(dropItem.getLocation().toVector()).multiply(0.2).add(new Vector(0, 0, 0)));
                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override
                    public void run() {
                        if (dropItem.isValid()) {
                            // No need to remove disarm here, it has its own expiry timer
                            dropItem.remove();
                        }
                    }
                }, 40); // 2 secs after pullback, force remove axe
            }
        }, 20); // 1 sec after thrown, pull it back

        player.getWorld().playSound(player.getLocation(), Sound.SHOOT_ARROW, 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }

    private class ThrowAxeAxeTask implements Runnable {

        public void run() {
            Iterator<ThrownAxe> axeIter = axes.iterator();
            while(axeIter.hasNext()) {
                ThrownAxe axe = axeIter.next();
                Item axeItem = axe.getItem();

                if(!axeItem.isValid()) { // The delayed task above handles axe timeout and removal, so we just need to kill it from the list after that
                    axeIter.remove();
                    continue;
                }

                Hero hero = axe.getOwner();
                Player player = hero.getPlayer();

                for(Entity entity : axeItem.getNearbyEntities(1, 1, 1)) {
                    if(!(entity instanceof  LivingEntity))
                        continue;

                    LivingEntity target = (LivingEntity) entity;
                    if(axe.getHitTargets().contains(target) || !damageCheck(player, target))
                        continue;

                    addSpellTarget(target, hero);
                    damageEntity(target, player, axe.getHitDamage(), DamageCause.ENTITY_ATTACK);

                    player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), org.bukkit.Effect.CRIT, 0, 0, 0, 0, 0, 1, 25, 16);
                    player.getWorld().playEffect(player.getLocation(), org.bukkit.Effect.BLAZE_SHOOT, 3);

                    Vector targetThrowVelocity = axe.getTargetThrowVelocity();
                    if(targetThrowVelocity != null) {
                        entity.setVelocity(targetThrowVelocity);
                        entity.setFallDistance(-512);
                    }

                    axe.getHitTargets().add(target);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemPickup(PlayerPickupItemEvent event) {
        Item item = event.getItem();
        for(ThrownAxe axe : axes) {

            if(axe.getItem().equals(item)) {
                event.setCancelled(true);
                Hero hero = axe.getOwner();

                if(event.getPlayer() == hero.getPlayer() && item.getTicksLived() > 5){

                    if(hero.hasEffect("Disarm")) {
                        DisarmEffect effect = (DisarmEffect) hero.getEffect("Disarm");

                        if(effect instanceof ThrowAxeDisarmEffect) {
                            effect.expire();
                        }
                    }
                    /*if(hero.hasEffect("ThrowAxeDisarmEffect")) {
                        Effect effect = hero.getEffect("ThrowAxeDisarmEffect");
                        if(effect instanceof ThrowAxeDisarmEffect) {
                            ((ThrowAxeDisarmEffect) effect).expire();
                        }
                    }*/

                    item.remove();
                }
            }
        }
    }

    public class ThrowAxeDisarmEffect extends DisarmEffect {
        public ThrowAxeDisarmEffect(Skill skill, Player applier, long duration) {
            super(skill, applier, duration);
            types.remove(EffectType.HARMFUL);
        }
    }


    /*
    // Pseudo-Disarm effect because we can't use the actual Disarm effect
    public class ThrowAxeDisarmEffect extends ExpirableEffect {

        private ItemStack axeItem;
        private int axeSlot;

        public ThrowAxeDisarmEffect(Skill skill, Player applier, long duration, ItemStack item, int slot) {
            super(skill, "ThrowAxeDisarmEffect", applier, duration);
            // types.add(EffectType.DISARM); // So that they can't be disarmed while this goes on

            axeItem = item;
            axeSlot = slot;
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Inventory inv = hero.getPlayer().getInventory();

            if(inv.getItem(axeSlot).equals(axeItem)) { // Normal
                inv.setItem(axeSlot, new ItemStack(Material.AIR));
            }
            else if(inv.contains(axeItem)) { // Panic slightly and adjust
                axeSlot = inv.first(axeItem);
                inv.setItem(axeSlot, new ItemStack(Material.AIR));
            }
            else { // Panic a lot and stop this madness
                axeItem = null;
                axeSlot = -1;
                Heroes.log(Level.WARNING, "SkillThrowAxe failed to find an axe for player " + hero.getPlayer().getName() + "! This is a problem!");
                expire();
            }
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            if(axeItem == null || axeSlot == -1)
                return;

            Player player = hero.getPlayer();
            Inventory inv = player.getInventory();

            if(inv.getItem(axeSlot).getType() == Material.AIR) {
                inv.setItem(axeSlot, axeItem);
            }
            else if(!inv.addItem(axeItem).isEmpty()) { // Only one item, so if the returning map is non-empty it failed to return
                player.getWorld().dropItem(player.getLocation(), axeItem);
            }
        }
    }*/

    // Safety measure, likely unnecessary
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent e) {

        if(e.getPlugin() != plugin) {
            return;
        }

        for(ThrownAxe axe : axes) {
            axe.getItem().remove();
        }
        axes.clear();
    }

    public class ThrownAxe {
        private Item item;
        private Hero owner;
        private double hitDamage;
        private Vector targetThrowVelocity;
        private List<LivingEntity> hitTargets;

        public ThrownAxe(Item axe, Hero thrower, double damage, Vector throwVelocity) {
            item = axe;
            owner = thrower;
            hitDamage = damage;
            targetThrowVelocity = throwVelocity;
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

        public Vector getTargetThrowVelocity() {
            return targetThrowVelocity;
        }

        public List<LivingEntity> getHitTargets() {
            return  hitTargets;
        }
    }
}
