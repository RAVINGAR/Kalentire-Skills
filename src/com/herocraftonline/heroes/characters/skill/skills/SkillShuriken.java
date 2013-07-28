package com.herocraftonline.heroes.characters.skill.skills;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillShuriken extends PassiveSkill {

    private Map<Arrow, Long> shurikens = new LinkedHashMap<Arrow, Long>(100) {
        private static final long serialVersionUID = 1L;

        protected boolean removeEldestEntry(Map.Entry<Arrow, Long> eldest) {
            return (size() > 60) || (((Long) eldest.getValue()).longValue() + 5000L <= System.currentTimeMillis());
        }
    };

    public SkillShuriken(Heroes plugin) {
        super(plugin, "Shuriken");
        setDescription("Right click with a flint in hand to throw $1 $2 damage and can be thrown every $3 seconds.");
        setArgumentRange(0, 0);
        setTypes(SkillType.HARMFUL, SkillType.DAMAGING, SkillType.ITEM, SkillType.PHYSICAL, SkillType.UNBINDABLE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    public String getDescription(Hero hero) {

        String numShurikenText = "";
        int numShuriken = SkillConfigManager.getUseSetting(hero, this, "num-shuriken", 3, false);

        if (numShuriken > 1)
            numShurikenText = "up to " + numShuriken + " Shuriken at once! Each Shuriken deals";
        else
            numShurikenText = "a Shuriken! Shuriken deal";
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 30, false);
        double cooldown = Util.formatDouble(SkillConfigManager.getUseSetting(hero, this, "shuriken-toss-cooldown", 1500, false) / 1000.0);

        return getDescription().replace("$1", numShurikenText).replace("$2", damage + "").replace("$3", cooldown + "");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(30));
        node.set("stamina-cost", Integer.valueOf(2));
        node.set("shuriken-toss-cooldown", Integer.valueOf(1500));
        node.set("num-shuriken", Integer.valueOf(3));
        node.set("degrees", Double.valueOf(15));
        node.set("interval", Double.valueOf(0.5));
        node.set("velocity-multiplier", Double.valueOf(3.0));

        return node;
    }

    public class SkillEntityListener implements Listener {

        private Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @SuppressWarnings("deprecation")
        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerInteract(PlayerInteractEvent event) {

            // Make sure the player is right clicking.
            if (!(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK))
                return;

            if (!event.hasItem())
                return;

            ItemStack activatedItem = event.getItem();

            if (activatedItem.getType() == Material.FLINT) {
                Player player = event.getPlayer();

                // If the clicked block is null, we are clicking air. Air is a valid block that we do not need to validate
                if (event.getClickedBlock() != null) {

                    // VALIDATE NON-AIR BLOCK
                    if ((Util.interactableBlocks.contains(event.getClickedBlock().getType()))) {
                        return;
                    }
                }

                Hero hero = plugin.getCharacterManager().getHero(player);

                // Check if the player's class actually has the skill available
                if (!hero.canUseSkill(skill))
                    return;                 // Class does not have the skill. Do nothing.

                if (hero.hasEffect("ShurikenTossCooldownEffect"))
                    return;     // Shuriken Toss is on cooldown. Do not continue.

                int staminaCost = SkillConfigManager.getUseSetting(hero, skill, "stamina-cost", 2, false);
                if (player.getFoodLevel() < staminaCost) {
                    Messaging.send(player, ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] You are too fatigued!");
                    return;
                }

                // Shuriken toss!
                shurikenToss(player);

                // Remove a flint from their inventory
                //                PlayerInventory inventory = player.getInventory();
                // activatedItem.setAmount(activatedItem.getAmount() - numShuriken);
                activatedItem.setAmount(activatedItem.getAmount() - 1);

                //                if (activatedItem.getAmount() == 0) {
                //                    inventory.clear(inventory.getHeldItemSlot());
                //                }

                player.updateInventory();

                // Reduce their stamina by the stamina cost value
                player.setFoodLevel(Math.max(player.getFoodLevel() - staminaCost, 0));
            }
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
                    if (projectile != null)
                        projectile.remove();
                }

            }, (long) ((0.1) * 20));
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageEvent event) {
            if ((!(event instanceof EntityDamageByEntityEvent)) || (!(event.getEntity() instanceof LivingEntity))) {
                return;
            }

            Entity projectile = ((EntityDamageByEntityEvent) event).getDamager();
            if ((!(projectile instanceof Arrow)) || (!(((Projectile) projectile).getShooter() instanceof Player))) {
                return;
            }

            if (!(shurikens.containsKey((Arrow) projectile))) {
                return;
            }

            Arrow shuriken = (Arrow) projectile;
            Player player = (Player) shuriken.getShooter();
            Hero hero = plugin.getCharacterManager().getHero(player);

            // Remove the shuriken from the hash map
            shurikens.remove(shuriken);

            LivingEntity target = (LivingEntity) event.getEntity();

            // Damage the target
            double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 30, false);
            skill.plugin.getDamageManager().addSpellTarget(target, hero, skill);
            damageEntity(target, player, damage, EntityDamageEvent.DamageCause.MAGIC);

            // Prevent arrow from dealing damage
            shuriken.remove();
            event.setDamage(0);
            event.setCancelled(true);
        }
    }

    public void shurikenToss(Player player) {
        Hero hero = plugin.getCharacterManager().getHero(player);
        int numShuriken = SkillConfigManager.getUseSetting(hero, this, "num-shuriken", 3, false);

        double degrees = SkillConfigManager.getUseSetting(hero, this, "degrees", 10, false);
        double interval = SkillConfigManager.getUseSetting(hero, this, "interval", 0.15, false);
        double velocityMultiplier = SkillConfigManager.getUseSetting(hero, this, "velocity-multiplier", 3.0, false);

        shurikenToss(player, numShuriken, degrees, interval, velocityMultiplier);
    }

    public void shurikenToss(final Player player, int numShuriken, double degrees, double interval, final double velocityMultiplier) {
        Hero hero = plugin.getCharacterManager().getHero(player);

        if (numShuriken < 1 || hero.hasEffect("ShurikenTossCooldownEffect"))
            return;
        else if (numShuriken == 1) {
            // If we're only firing a single shuriken, there is no need for fancy math.
            Arrow shuriken = (Arrow) player.launchProjectile(Arrow.class);
            shuriken.setVelocity(shuriken.getVelocity().multiply(velocityMultiplier));
            shuriken.setShooter(player);
            shurikens.put(shuriken, Long.valueOf(System.currentTimeMillis()));

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
                    double yaw = player.getLocation().getYaw();
                    yaw = yaw * (Math.PI / 180);

                    // Convert Pitch to radians
                    double pitch = player.getLocation().getPitch();
                    pitch = 180 - pitch;
                    pitch = pitch * (Math.PI / 180);
                    yaw = yaw + degreeOffsetRad;

                    // Create our velocity direction based on where the player is facing.
                    Vector vel = new Vector(Math.cos(yaw + finalA), Math.tan(pitch), Math.sin(yaw + finalA));

                    Arrow shuriken = (Arrow) player.launchProjectile(Arrow.class);
                    shuriken.setVelocity(vel.multiply(velocityMultiplier));
                    shuriken.setShooter(player);
                    shurikens.put(shuriken, Long.valueOf(System.currentTimeMillis()));
                }

            }, (long) ((interval * i) * 20));

            i++;
        }

        // Add the cooldown effect
        int cdDuration = SkillConfigManager.getUseSetting(hero, this, "shuriken-toss-cooldown", 1500, false);
        hero.addEffect(new ShurikenTossCooldownEffect(this, cdDuration));
    }

    // Effect required for implementing an internal cooldown
    private class ShurikenTossCooldownEffect extends ExpirableEffect {
        public ShurikenTossCooldownEffect(Skill skill, long duration) {
            super(skill, "ShurikenTossCooldownEffect", duration);
        }
    }
}