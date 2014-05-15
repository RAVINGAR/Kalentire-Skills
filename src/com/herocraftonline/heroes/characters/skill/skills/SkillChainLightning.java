package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.Map.Entry;

public class SkillChainLightning extends TargettedSkill {

    // Maps. Lots of em.
    Map<Hero, Snowball> snowballs = new HashMap<Hero, Snowball>();
    Map<Hero, Double> snowballVelocities = new HashMap<Hero, Double>();
    Map<Hero, LivingEntity> targets = new HashMap<Hero, LivingEntity>();
    Map<Hero, List<LivingEntity>> hitTargets = new HashMap<Hero, List<LivingEntity>>();
    Map<Hero, Integer> maxTargets = new HashMap<Hero, Integer>();

    // Normal skill stuff
    public SkillChainLightning(Heroes plugin) {
        super(plugin, "ChainLightning");
        setArgumentRange(0,0);
        setUsage("/skill chainlightning");
        setIdentifiers("skill chainlightning");
        setDescription("Start a ChainLighting chain aimed at the target, bolting hit targets for $1 damage and spreading to a random target within $2 blocks, up to $3 targets.");
        setTypes(SkillType.ABILITY_PROPERTY_LIGHTNING, SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
        plugin.getServer().getPluginManager().registerEvents(new ChainLightningListener(this), plugin);

        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new ChainLightningRunnable(), 0, 1);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription()
                .replace("$1", getDamage(hero) + "")
                .replace("$2", getRadius(hero) + "")
                .replace("$3", getMaxTargets(hero) + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE.node(), 200D);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.1D);
        node.set(SkillSetting.RADIUS.node(), 5D);
        node.set(SkillSetting.RADIUS_INCREASE_PER_WISDOM.node(), 0.17D);
        node.set("max-targets", 3);
        node.set("max-targets-per-level", 0.04D);
        node.set("velocity", 0.5D);
        node.set("error-correction-hit-range", 1D);
        node.set("lightning-volume", 0.0F);
        return node;
    }

    // Methods to grab config info, because I don't feel like putting this math everywhere.
    public double getDamage(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 200D, false) + SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.1D, false) * hero.getAttributeValue(AttributeType.INTELLECT);
    }

    public double getRadius(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5D, false) + SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS_INCREASE_PER_WISDOM, 0.17D, false) * hero.getAttributeValue(AttributeType.WISDOM);
    }

    public int getMaxTargets(Hero hero) {
        return (int) (SkillConfigManager.getUseSetting(hero, this, "max-targets", 3, false) + SkillConfigManager.getUseSetting(hero, this, "radius-per-level", 0.04D, false) * hero.getSkillLevel(this));
    }

    public double getVelocity(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, "velocity", 0.5D, false);
    }

    public double getErrorCorrectionHitRange(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, "error-correction-hit-range", 1D, false);
    }

    public float getLightningVolume(Hero h) {
    	return (float) SkillConfigManager.getUseSetting(h, this, "lightning-volume", 0.0F, false);
    }
    
    // And now, back to your regularly scheduled code.
    // Checks for active chain, spawns initial snowball, sets up map data.
    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        if(snowballs.containsKey(hero)) {
            hero.getPlayer().sendMessage(ChatColor.RED + "You already have a chain going!");
            return SkillResult.FAIL;
        }
        Snowball snowball = ((Snowball) hero.getPlayer().launchProjectile(Snowball.class));
        snowball.setShooter(hero.getPlayer());
        snowballs.put(hero, snowball);
        snowballVelocities.put(hero, getVelocity(hero));
        targets.put(hero, target);
        hitTargets.put(hero, new ArrayList<LivingEntity>());
        maxTargets.put(hero, getMaxTargets(hero));
        return SkillResult.NORMAL;
    }

    // Sets velocity, applies visual effect and does compensation hitting
    private void setVelocity(Hero key, Snowball snowball, LivingEntity target) {
        Vector tLoc = target.getEyeLocation().toVector();
        Vector aLoc = snowball.getLocation().toVector();
        snowball.setVelocity(tLoc.subtract(aLoc).normalize().multiply(snowballVelocities.get(key)));
        snowball.getWorld().spigot().playEffect(snowball.getLocation(), Effect.SNOW_SHOVEL, 0, 0, 0, 0, 0, 1, 25, 16);
        if(target.getEyeLocation().distanceSquared(snowball.getLocation()) <= getErrorCorrectionHitRange(key)) {
            snowball.remove();
            hitTarget(snowball, key, target);
            if(hitTargets.get(key).size() >= maxTargets.get(key)) {
                // snowball being null means end
                snowballs.put(key, null);
            }
        }
    }

    // Retargets around just a snowball
    public void retarget(Snowball oldSnowball, Hero key) {
        if(oldSnowball == null) {
            return;
        }
        LivingEntity target = getTarget(oldSnowball, key);
        if(target == null) {
            snowballs.put(key, null);
            return;
        }
        Location loc = oldSnowball.getLocation();
        Snowball snowball = (Snowball) loc.getWorld().spawn(loc, Snowball.class);
        snowball.setShooter(key.getPlayer());
        snowballs.put(key, snowball);
        targets.put(key, target);
        setVelocity(key, snowball, target);
    }

    // Retargets around a hit entity
    public void retarget(LivingEntity livingEntity, Hero key) {
        Snowball oldSnowball = snowballs.get(key);
        if(oldSnowball == null) {
            return;
        }
        LivingEntity target = getTarget(livingEntity, key);
        if(target == null) {
            snowballs.put(key, null);
            return;
        }
        Snowball snowball = (Snowball) livingEntity.launchProjectile(Snowball.class);
        oldSnowball.remove();
        snowball.setShooter(key.getPlayer());
        snowballs.put(key, snowball);
        targets.put(key, target);
        setVelocity(key, snowball, target);
    }

    // Gets a new target
    private LivingEntity getTarget(Entity centerEntity, Hero key) {
        double radius = getRadius(key);
        List<Entity> entities = centerEntity.getNearbyEntities(radius, radius, radius);
        List<LivingEntity> possibleTargets = new ArrayList<LivingEntity>();
        Player player = key.getPlayer();
        for(Entity entity : entities) {
            if(entity instanceof LivingEntity) {
                LivingEntity lEntity = (LivingEntity) entity;
                if(damageCheck(player, lEntity) && !hitTargets.get(key).contains(lEntity)) {
                    possibleTargets.add(lEntity);
                }                      
            }
        }
        if(possibleTargets.isEmpty()) {
            return null;
        }
        LivingEntity target = possibleTargets.get(Util.nextInt(possibleTargets.size()));
        return target;
    }

    // Damages the target
    private void hitTarget(Snowball snowball, Hero hero, LivingEntity targetLE) {
        Player player = hero.getPlayer();
        
        if (!Skill.damageCheck(player, targetLE)) {
            return;
        }
        
        // Piece of the old method of faking lightning that involved the effects below. No longer needed
        //player.getWorld().playSound(player.getLocation(), Sound.AMBIENCE_THUNDER, 1.0F, 1.0F);
        plugin.getDamageManager().addSpellTarget(targetLE, hero, this);
        damageEntity(targetLE, player, getDamage(hero));
        snowball.setMetadata("ChainLightningHitTarget", new FixedMetadataValue(plugin, 1));
        targets.put(hero, targetLE);
        hitTargets.get(hero).add(targetLE);
        
        targetLE.getWorld().spigot().strikeLightningEffect(targetLE.getLocation(), true);
        targetLE.getWorld().playSound(targetLE.getLocation(), Sound.AMBIENCE_THUNDER, getLightningVolume(hero), 1.0F);
        
        // We have lightning, so we don't need this
        /* This is the new Particle API system for Spigot - the first few int = id, data, offsetX/Y/Z, speed, count, radius)
         * offset controls how spread out the particles are
         * id and data only work for two particles: ITEM_BREAK and TILE_BREAK
         * */
        /*targetLE.getWorld().spigot().playEffect(targetLE.getLocation().add(0, 0.6, 0), Effect.SNOW_SHOVEL, 0, 0, 0, 0, 0, 1, 25, 16);
        targetLE.getWorld().spigot().playEffect(targetLE.getLocation().add(0, 0.7, 0), Effect.SNOW_SHOVEL, 0, 0, 0, 0, 0, 1, 25, 16);
        targetLE.getWorld().spigot().playEffect(targetLE.getLocation().add(0, 0.9, 0), Effect.SNOW_SHOVEL, 0, 0, 0, 0, 0, 1, 25, 16);
        targetLE.getWorld().spigot().playEffect(targetLE.getLocation().add(0, 1.0, 0), Effect.SNOW_SHOVEL, 0, 0, 0, 0, 0, 1, 25, 16);
        targetLE.getWorld().spigot().playEffect(targetLE.getLocation().add(0, 1.1, 0), Effect.SNOW_SHOVEL, 0, 0, 0, 0, 0, 1, 25, 16);
        targetLE.getWorld().spigot().playEffect(targetLE.getLocation().add(0, 1.2, 0), Effect.SNOW_SHOVEL, 0, 0, 0, 0, 0, 1, 25, 16);   
        */     
    }

    // Keeps the logic running
    private class ChainLightningRunnable implements Runnable {

        @Override
        public void run() {
            Iterator<Entry<Hero, Snowball>> iter = snowballs.entrySet().iterator();
            while(iter.hasNext()) {
                Entry<Hero, Snowball> pair = iter.next();
                Hero hero = pair.getKey();
                Snowball snowball = pair.getValue();
                LivingEntity target = targets.get(hero);
                // Snowball being null is when it's done
                if(snowball == null || !snowball.isValid()) {
                    if(snowball != null){
                        if(snowball.hasMetadata("ChainLightningHitTarget")) {
                            retarget(target, hero);
                            snowball.removeMetadata("ChainLightningHitTarget", plugin);
                            if(snowball.hasMetadata("ChainLightningHit")) {
                                snowball.removeMetadata("ChainLightningHit", plugin);
                            }
                            continue;
                        }
                        else if(snowball.hasMetadata("ChainLightningHit")) {
                            retarget(snowball, hero);
                            snowball.removeMetadata("ChainLightningHit", plugin);
                            continue;
                        }
                    }
                    iter.remove();
                    snowballVelocities.remove(hero);
                    hitTargets.remove(hero);
                    maxTargets.remove(hero);
                    continue;
                }
                if(!target.isValid()) {
                    retarget(snowball, hero);
                    continue;
                }
                setVelocity(hero, snowball, target);
            }
        }
    }

    private class ChainLightningListener implements Listener {
        private SkillChainLightning skill;

        public ChainLightningListener(SkillChainLightning skill) {
            this.skill = skill;
        }

        // Hit on the snowball hitting. Needed for in case the snowball hits the target normally or another target intercepts
        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageByEntityEvent event) {

            if ((!(event.getEntity() instanceof LivingEntity) || !(event.getDamager() instanceof Snowball))) {
                return;
            }

            Snowball snowball = (Snowball) event.getDamager();
            LivingEntity targetLE = (LivingEntity) event.getEntity();

            if(!(snowball.getShooter() instanceof Player)) {
                return;
            }
            Player player = (Player) snowball.getShooter();
            Hero hero = plugin.getCharacterManager().getHero(player);
            if(!snowballs.containsKey(hero)) {
                return;
            }
            event.setCancelled(true);

            if(!hitTargets.get(hero).contains(targetLE)) {
                hitTarget(snowball, hero, targetLE);                
                if(hitTargets.get(hero).size() >= maxTargets.get(hero)) {
                    // snowball being null means end
                    snowballs.put(hero, null);
                }
                else {
                    retarget(targetLE, hero);
                }
            }
        }

        // In case the snowball hits a wall or an entity without firing damage, retarget next loop
        @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = true)
        public void onProjectileHit(ProjectileHitEvent event) {
            if(!(event.getEntity() instanceof Snowball)) {
                return;
            }
            Snowball snowball = (Snowball) event.getEntity();
            if(snowballs.containsValue(snowball)) {
                snowball.setMetadata("ChainLightningHit", new FixedMetadataValue(skill.plugin, 1));
            }
        }

        // If the chain owner leaves, stop it. Be crazy otherwise
        @EventHandler()
        public void onPlayerQuit(PlayerQuitEvent event) {
            Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
            if(snowballs.containsKey(hero)) {
                snowballs.put(hero, null);
            }
        }
    }
}
