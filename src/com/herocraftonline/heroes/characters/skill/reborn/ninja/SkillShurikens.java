package com.herocraftonline.heroes.characters.skill.reborn.ninja;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillUseEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

import java.util.*;

public class SkillShurikens extends PassiveSkill {
    private static String cooldownEffectName = "ShurikenTossCooldown";

    private static List<String> defaultWeapons = new ArrayList<String>(Arrays.asList(Material.NETHER_STAR.name()));

    private Map<Arrow, Long> shurikens = new LinkedHashMap<Arrow, Long>(100) {
        private static final long serialVersionUID = 1L;

        protected boolean removeEldestEntry(Map.Entry<Arrow, Long> eldest) {
            return (size() > 60) || (eldest.getValue() + 5000L <= System.currentTimeMillis());
        }
    };

    public SkillShurikens(Heroes plugin) {
        super(plugin, "Shurikens");
        setDescription("Right click with a ($4) in hand to throw $1 $2 damage and can be thrown every $3 second(s). " +
                "Offhanding works too!");
        setArgumentRange(0, 0);
        setTypes(SkillType.AGGRESSIVE, SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_PROJECTILE, SkillType.UNBINDABLE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    public String getDescription(Hero hero) {

        String numShurikenText = "";
        int numShuriken = SkillConfigManager.getUseSetting(hero, this, "num-shuriken", 3, false);

        if (numShuriken > 1)
            numShurikenText = "up to " + numShuriken + " Shuriken at once! Each Shuriken deals";
        else
            numShurikenText = "a Shuriken! Shuriken deal";

        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        int cooldown = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 1000, false);
        List<String> weapons = SkillConfigManager.getUseSetting(hero, this, "toss-weapons", defaultWeapons);

        return getDescription()
                .replace("$1", numShurikenText)
                .replace("$2", Util.decFormat.format(damage))
                .replace("$3", Util.decFormat.format(cooldown / 1000.0))
                .replace("$4", String.join(", ", weapons));
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("toss-weapons", defaultWeapons);
        config.set(SkillSetting.DAMAGE.node(), 20.0);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_DEXTERITY.node(), 0.0);
        config.set(SkillSetting.STAMINA.node(), 100);
        config.set(SkillSetting.COOLDOWN.node(), 1000);
        config.set("num-shuriken", 3);
        config.set("degrees", 10.0);
        config.set("interval", 0.15);
        config.set("velocity-multiplier", 3.0);
        return config;
    }

    public boolean tryShurikenToss(Hero hero, boolean applyCosts) {
        Player player = hero.getPlayer();
        if (!validateCanCast(hero, applyCosts))
            return false;

        int numShuriken = SkillConfigManager.getUseSetting(hero, this, "num-shuriken", 3, false);

        double degrees = SkillConfigManager.getUseSetting(hero, this, "degrees", 10.0, false);
        double interval = SkillConfigManager.getUseSetting(hero, this, "interval", 0.15, false);
        double velocityMultiplier = SkillConfigManager.getUseSetting(hero, this, "velocity-multiplier", 3.0, false);

        shurikenToss(player, numShuriken, degrees, interval, velocityMultiplier);
        return true;
    }

    public boolean validateCanCast(Hero hero, boolean applyCosts) {
        if (!hero.canUseSkill(this))
            return false;

        if (hero.hasEffect(cooldownEffectName)) {
            double remainingTime = ((ShurikenTossCooldown) hero.getEffect(cooldownEffectName)).getRemainingTime() / 1000.0;
            if (remainingTime > 0.0) {    // Sometimes we are below zero with this thing. Kinda weird.
                String formattedRemainingTime = Util.decFormatCDs.format(remainingTime);
                ActiveSkill.sendResultMessage(hero, this, new SkillResult(SkillResult.ResultType.ON_COOLDOWN, true, this.getName(), formattedRemainingTime));
                return false;
            }
        }

        Player player = hero.getPlayer();
        PlayerInventory playerInv = player.getInventory();
        ItemStack mainHand = NMSHandler.getInterface().getItemInMainHand(playerInv);
        ItemStack offHand = NMSHandler.getInterface().getItemInOffHand(playerInv);
        List<String> weapons = SkillConfigManager.getUseSetting(hero, this, "toss-weapons", defaultWeapons);
        if ((mainHand == null || !weapons.contains(mainHand.getType().name())) && (offHand == null || !weapons.contains(offHand.getType().name())))
            return false;

        double healthCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST, 0.0, false);
        int stamCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA, 0, false);
        int manaCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 0, false);

        final SkillUseEvent skillEvent = new SkillUseEvent(this, player, hero, manaCost, healthCost, stamCost, null, null);
        plugin.getServer().getPluginManager().callEvent(skillEvent);
        if (skillEvent.isCancelled()) {
            ActiveSkill.sendResultMessage(hero, this, SkillResult.CANCELLED);
            return false;
        }

        // Update manaCost with result of SkillUseEvent
        manaCost = skillEvent.getManaCost();
        if (manaCost > hero.getMana()) {
            ActiveSkill.sendResultMessage(hero, this, SkillResult.LOW_MANA);
            return false;
        }

        // Update healthCost with results of SkillUseEvent
        healthCost = skillEvent.getHealthCost();
        if (healthCost > 0 && (hero.getPlayer().getHealth() <= healthCost)) {
            ActiveSkill.sendResultMessage(hero, this, SkillResult.LOW_HEALTH);
            return false;
        }

        //Update staminaCost with results of SkilluseEvent
        stamCost = skillEvent.getStaminaCost();
        if (stamCost > 0 && (hero.getStamina() < stamCost)) {
            ActiveSkill.sendResultMessage(hero, this, SkillResult.LOW_STAMINA);
            return false;
        }

        if (!applyCosts)
            return true;

        // Deduct health
        if (healthCost > 0) {
            player.setHealth(player.getHealth() - healthCost);
        }

        // Deduct mana
        if (manaCost > 0) {
            hero.setMana(hero.getMana() - manaCost);
            if (hero.isVerboseMana()) {
                hero.getPlayer().sendMessage(ChatComponents.Bars.mana(hero.getMana(), hero.getMaxMana(), true));
            }
        }

        // Deduct stamina
        if (stamCost > 0) {
            hero.setStamina(hero.getStamina() - stamCost);
            if (hero.isVerboseStamina()) {
                hero.getPlayer().sendMessage(ChatComponents.Bars.stamina(hero.getStamina(), hero.getMaxStamina(), true));
            }
        }

        return true;
    }

    private void shurikenToss(final Player player, int numShuriken, double degrees, double interval, final double velocityMultiplier) {
        Hero hero = plugin.getCharacterManager().getHero(player);

        if (numShuriken < 1 || hero.hasEffect(cooldownEffectName)) {
            return;
        } else if (numShuriken == 1) {
            // If we're only firing a single shuriken, there is no need for fancy math.
            Arrow shuriken = player.launchProjectile(Arrow.class);
            shuriken.setVelocity(shuriken.getLocation().getDirection().multiply(velocityMultiplier));
            shurikens.put(shuriken, System.currentTimeMillis());

            return;
        }

        // Otherwise, let's get on to the annoying stuff

        // Create arrow spread
        double degreesRad = degrees * (Math.PI / 180);      // Convert degrees to radians
        double diff = degreesRad / (numShuriken - 1);       // Create our difference for the spread

        // Center the projectile direction based on yaw, and then convert it to radians.
        double degreeOffset = (90.0 - (degrees / 2.0));
        final double degreeOffsetRad = degreeOffset * (Math.PI / 180);

        // Throw shurikens in a clockwise direction, with a delay based on the passed interval.
        int i = 1;
        for (double a = 0; a <= degreesRad; a += diff) {
            final double finalA = a;
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    // Convert yaw to radians
                    double yaw = player.getEyeLocation().getYaw();
                    yaw = yaw * (Math.PI / 180);

                    // Offset Yaw
                    yaw = yaw + degreeOffsetRad;

                    // Convert Pitch to radians
                    double pitch = player.getEyeLocation().getPitch();
                    pitch *= -1;    // Invert pitch
                    pitch = pitch * (Math.PI / 180);

                    Arrow shuriken = player.launchProjectile(Arrow.class);
                    double yVel = shuriken.getVelocity().getY();

                    // Create our velocity direction based on where the player is facing.
                    Vector velocity = new Vector(Math.cos(yaw + finalA), yVel, Math.sin(yaw + finalA)).multiply(velocityMultiplier);
                    shuriken.setVelocity(velocity.setY(yVel));
                    shurikens.put(shuriken, System.currentTimeMillis());
                }

            }, (long) ((interval * i) * 20));

            i++;
        }

        // Add the cooldown effect
        int cdDuration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 1000, false);
        hero.addEffect(new ShurikenTossCooldown(this, player, cdDuration));
    }

    public class SkillEntityListener implements Listener {
        private Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerInteract(PlayerInteractEvent event) {
            if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
                return;

            Player player = event.getPlayer();
            Hero hero = plugin.getCharacterManager().getHero(player);
            if (!hero.canUseSkill(skill))
                return;

            if (event.getClickedBlock() != null) {
                if ((Util.interactableBlocks.contains(event.getClickedBlock().getType()))) {
                    return;
                }
            }

            tryShurikenToss(hero, true);
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onProjectileHit(ProjectileHitEvent event) {
            if (!(event.getEntity() instanceof Arrow))
                return;

            final Arrow projectile = (Arrow) event.getEntity();
            if ((!(projectile.getShooter() instanceof Player)))
                return;
            if (!(shurikens.containsKey(projectile)))
                return;

            // Delete the projectile so it cannot be picked up by any players.
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    shurikens.remove(projectile);
                    projectile.remove();
                }
            }, 2L);
        }

        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageEvent event) {
            if ((!(event instanceof EntityDamageByEntityEvent)) || (!(event.getEntity() instanceof LivingEntity)))
                return;

            Entity projectile = ((EntityDamageByEntityEvent) event).getDamager();
            if ((!(projectile instanceof Arrow)) || (!(((Projectile) projectile).getShooter() instanceof Player)))
                return;
            if (!(shurikens.containsKey(projectile)))
                return;

            Arrow shuriken = (Arrow) projectile;
            Player player = (Player) shuriken.getShooter();
            Hero hero = plugin.getCharacterManager().getHero(player);

            shurikens.remove(shuriken);

            LivingEntity target = (LivingEntity) event.getEntity();

            // Damage the target
            double damage = SkillConfigManager.getScaledUseSettingDouble(hero, skill, SkillSetting.DAMAGE, false);

            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK, false);

            // Prevent arrow from dealing damage
            shuriken.remove();
            event.setDamage(0.0);
            event.setCancelled(true);
        }
    }

    // Effect required for implementing an internal cooldown
    private class ShurikenTossCooldown extends ExpirableEffect {
        ShurikenTossCooldown(Skill skill, Player applier, long duration) {
            super(skill, cooldownEffectName, applier, duration);
        }
    }
}