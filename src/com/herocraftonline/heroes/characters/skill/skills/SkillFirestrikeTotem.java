package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.totem.SkillBaseTotem;
import com.herocraftonline.heroes.characters.skill.skills.totem.Totem;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class SkillFirestrikeTotem extends SkillBaseTotem {
    
    private final Map<SmallFireball, LivingEntity> homingFireballs = new LinkedHashMap<SmallFireball, LivingEntity>();
    private final Map<SmallFireball, Double> fireballVelocities = new LinkedHashMap<SmallFireball, Double>();
    // Order of the faces matters, don't reorder them :(
    private final BlockFace[] firingFaces = { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST,};

    public SkillFirestrikeTotem(Heroes plugin) {
        super(plugin, "FirestrikeTotem");
        setArgumentRange(0,0);
        setUsage("/skill firestriketotem");
        setIdentifiers("skill firestriketotem");
        setDescription("Places a firestrike totem at target location that shoots fireballs at entities in a $1 radius dealing $2 damage. Lasts for $3 seconds.");
        setTypes(SkillType.ABILITY_PROPERTY_FIRE, SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT);
        material = Material.NETHERRACK;
        plugin.getServer().getPluginManager().registerEvents(new FirestrikeEntityListener(this), plugin);
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new FirestrikeFireballTask(), 0, 1);
    }

    @Override
    public String getDescription(Hero h) {
        return getDescription()
                .replace("$1", getRange(h) + "")
                .replace("$2", getDamage(h) + "")
                .replace("$3", getDuration(h)*0.001 + "");
    }

    @Override
    public void usePower(Hero hero, Totem totem) {
        Player heroP = hero.getPlayer();
        Block locForRel = totem.getLocation().getBlock().getRelative(BlockFace.UP, 2);

        int maxTargets = SkillConfigManager.getUseSetting(hero, this, "max-targets", 0, false);
        int targetsHit = 0;
        for(LivingEntity entity : totem.getTargets(hero)) {
            // Check to see if we've exceeded the max targets
            if (maxTargets > 0 && targetsHit >= maxTargets) {
                break;
            }
            if(!damageCheck(heroP, entity)) {
                continue;
            }
            for(int i = 0; i < 4; i++) {
                SmallFireball fireball = entity.getWorld().spawn(locForRel.getRelative(firingFaces[i], 2).getLocation(), SmallFireball.class);
                homingFireballs.put(fireball, entity);
                fireballVelocities.put(fireball, getVelocity(hero));
                setVelocity(fireball, entity);
                fireball.setShooter(heroP);
            }
            targetsHit++;
        }
    }

    private void setVelocity(SmallFireball fireball, LivingEntity target) {
        Vector tLoc = target.getEyeLocation().toVector();
        Vector aLoc = fireball.getLocation().toVector();
        fireball.setVelocity(tLoc.subtract(aLoc).normalize().multiply(fireballVelocities.get(fireball)));
    }

    @Override
    public ConfigurationSection getSpecificDefaultConfig(ConfigurationSection node) {
        node.set(SkillSetting.DAMAGE.node(), 50.0);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 5.0);
        node.set("fire-ticks", 50);
        node.set("velocity", 1.5);
        node.set("max-targets", 5);
        return node;
    }

    // Methods to grab config info that is specific to this skill
    public double getDamage(Hero h) {
        return SkillConfigManager.getUseSetting(h, this, SkillSetting.DAMAGE, 50.0, false) + SkillConfigManager.getUseSetting(h, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 5.0, false) * h.getAttributeValue(AttributeType.INTELLECT);
    }

    public int getFireTicks(Hero h) {
        return SkillConfigManager.getUseSetting(h, this, "fire-ticks", 50, false);
    }

    public double getVelocity(Hero h) {
        return SkillConfigManager.getUseSetting(h, this, "velocity", 1.5, false);
    }

    private class FirestrikeFireballTask implements Runnable {

        public void run() {
            Iterator<Entry<SmallFireball, LivingEntity>> fireballs = homingFireballs.entrySet().iterator();
            while(fireballs.hasNext()) {
                Entry<SmallFireball, LivingEntity> pair = fireballs.next();
                SmallFireball fireball = pair.getKey();
                LivingEntity target = pair.getValue();
                if(!fireball.isValid() || !target.isValid()) {
                    fireball.remove();
                    fireballs.remove();
                    fireballVelocities.remove(fireball);
                    continue;
                }
                setVelocity(fireball, target);
            }
        }
    }

    public class FirestrikeEntityListener implements Listener {
        private final SkillFirestrikeTotem skill;

        public FirestrikeEntityListener(SkillFirestrikeTotem skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageByEntityEvent event) {

            Entity projectile = event.getDamager();
            if (!(event.getEntity() instanceof LivingEntity) || (!(projectile instanceof SmallFireball)) || (!skill.homingFireballs.containsKey(projectile))) {
                return;
            }

            skill.homingFireballs.remove(projectile);
            skill.fireballVelocities.remove(projectile);
            event.setCancelled(true);

            LivingEntity targetLE = (LivingEntity)event.getEntity();
            ProjectileSource source = ((Projectile) event.getDamager()).getShooter();
            if (!(source instanceof Entity))
                return;
            Entity dmger = (LivingEntity) source;
            if ((dmger instanceof Player)) {
                Hero hero = skill.plugin.getCharacterManager().getHero((Player)dmger);
                Player player = (Player) dmger;

                if (!Skill.damageCheck(player, targetLE)) {
                    return;
                }

                // No target igniting here...
                // Ignite the target
                // targetLE.setFireTicks(skill.getFireTicks(hero));
                // skill.plugin.getCharacterManager().getCharacter(targetLE).addEffect(new CombustEffect(this.skill, player));
                skill.plugin.getDamageManager().addSpellTarget(targetLE, hero, skill);
                skill.damageEntity(targetLE, player, skill.getDamage(hero));

                /* This is the new Particle API system for Spigot - the first few int = id, data, offsetX/Y/Z, speed, count, radius)
                 * offset controls how spread out the particles are
                 * id and data only work for two particles: ITEM_BREAK and TILE_BREAK
                 * */
                targetLE.getWorld().spigot().playEffect(targetLE.getLocation().add(0, 0.6, 0), Effect.MOBSPAWNER_FLAMES, 0, 0, 0, 0, 0, 1, 150, 16);
            }
        }

        @EventHandler(ignoreCancelled=true)
        public void onBlockIgnite(BlockIgniteEvent event) {
            if(event.getCause() != IgniteCause.FIREBALL || event.getIgnitingEntity().getType() != EntityType.SMALL_FIREBALL) {
                return;
            }
            SmallFireball fireball = (SmallFireball) event.getIgnitingEntity();
            if(skill.homingFireballs.containsKey(fireball)) {
                skill.homingFireballs.remove(fireball);
                skill.fireballVelocities.remove(fireball);
                event.setCancelled(true); 
            }
        }
    }

}