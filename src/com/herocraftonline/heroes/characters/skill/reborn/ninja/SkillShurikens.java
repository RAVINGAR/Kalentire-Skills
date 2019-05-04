package com.herocraftonline.heroes.characters.skill.reborn.ninja;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
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

import java.util.LinkedHashMap;
import java.util.Map;

public class SkillShurikens extends PassiveSkill {

    private Map<Arrow, Long> shurikens = new LinkedHashMap<Arrow, Long>(100) {
        private static final long serialVersionUID = 1L;

        protected boolean removeEldestEntry(Map.Entry<Arrow, Long> eldest) {
            return (size() > 60) || (eldest.getValue() + 5000L <= System.currentTimeMillis());
        }
    };

    public SkillShurikens(Heroes plugin) {
        super(plugin, "Shurikens");
        setDescription("Right click with a shear in hand to throw $1 $2 damage and can be thrown every $3 second(s).");
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

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 20.0, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_DEXTERITY, 0.6, false);
        damage += (int) (hero.getAttributeValue(AttributeType.DEXTERITY) * damageIncrease);

        int cooldown = SkillConfigManager.getUseSetting(hero, this, "shuriken-toss-cooldown", 1000, false);
        String formattedCooldown = Util.decFormat.format(cooldown / 1000.0);

        return getDescription().replace("$1", numShurikenText).replace("$2", damage + "").replace("$3", formattedCooldown);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 35.0);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_DEXTERITY.node(), 0.4);
        config.set(SkillSetting.STAMINA.node(), 100);
        config.set("shuriken-toss-cooldown", 1000);
        config.set("num-shuriken", 3);
        config.set("degrees", (double) 10);
        config.set("interval", 0.15);
        config.set("velocity-multiplier", 3.0);
        return config;
    }

    public class SkillEntityListener implements Listener {

        private Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerInteract(PlayerInteractEvent event) {
            if (!(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK))
                return;
            if (!event.hasItem())
                return;

            ItemStack activatedItem = event.getItem();
            if (activatedItem.getType() != Material.SHEARS)
                return;

            Player player = event.getPlayer();
            if (event.getClickedBlock() != null) {
                if ((Util.interactableBlocks.contains(event.getClickedBlock().getType()))) {
                    return;
                }
            }

            Hero hero = plugin.getCharacterManager().getHero(player);
            if (!hero.canUseSkill(skill))
                return;
            if (hero.hasEffect("ShurikenTossCooldown"))
                return;

            int staminaCost = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.STAMINA, 100, false);
            if (staminaCost > 0) {
                if (hero.getStamina() < staminaCost) {
                    player.sendMessage("    " + ChatComponents.GENERIC_SKILL + "You are too fatigued!");
                    return;
                }
                hero.setStamina(hero.getStamina() - staminaCost);
            }

            shurikenToss(player);
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
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, new Runnable() {
                @Override
                public void run() {
                    shurikens.remove(projectile);
                    projectile.remove();
                }

            }, 2L);
        }

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
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
            double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 35.0, false);
            double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_DEXTERITY, 0.0, false);
            damage += hero.getAttributeValue(AttributeType.DEXTERITY) * damageIncrease;

            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK, false);

            // Prevent arrow from dealing damage
            shuriken.remove();
            event.setDamage(0.0);
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

        if (numShuriken < 1 || hero.hasEffect("ShurikenTossCooldown"))
            return;
        else if (numShuriken == 1) {
            // If we're only firing a single shuriken, there is no need for fancy math.
            Arrow shuriken = player.launchProjectile(Arrow.class);
            shuriken.setVelocity(shuriken.getVelocity().multiply(velocityMultiplier));
            shuriken.setShooter(player);
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
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, new Runnable() {
                @Override
                public void run() {
                    // Convert yaw to radians
                    double yaw = player.getLocation().getYaw();
                    yaw = yaw * (Math.PI / 180);

                    // Offset Yaw
                    yaw = yaw + degreeOffsetRad;

                    // Convert Pitch to radians
                    double pitch = player.getEyeLocation().getPitch();
                    pitch *= -1;    // Invert pitch
                    pitch = pitch * (Math.PI / 180);

                    Arrow shuriken = player.launchProjectile(Arrow.class);
                    double yValue = shuriken.getVelocity().getY();

                    // Create our velocity direction based on where the player is facing.
                    Vector vel = new Vector(Math.cos(yaw + finalA), 0, Math.sin(yaw + finalA));
                    vel.multiply(velocityMultiplier);
                    vel.setY(yValue * velocityMultiplier);

                    shuriken.setVelocity(vel);
                    shuriken.setShooter(player);
                    shurikens.put(shuriken, System.currentTimeMillis());
                }

            }, (long) ((interval * i) * 20));

            i++;
        }

        // Add the cooldown effect
        int cdDuration = SkillConfigManager.getUseSetting(hero, this, "shuriken-toss-cooldown", 1000, false);
        hero.addEffect(new ShurikenTossCooldown(this, player, cdDuration));
    }

    // Effect required for implementing an internal cooldown
    private class ShurikenTossCooldown extends ExpirableEffect {
        ShurikenTossCooldown(Skill skill, Player applier, long duration) {
            super(skill, "ShurikenTossCooldown", applier, duration);
        }
    }
}