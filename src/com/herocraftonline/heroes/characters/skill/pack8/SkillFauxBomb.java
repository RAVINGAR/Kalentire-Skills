package com.herocraftonline.heroes.characters.skill.pack8;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkillFauxBomb extends ActiveSkill {
    private Map<Integer, Player> sheepMap = new HashMap<Integer, Player>();

    public SkillFauxBomb(Heroes plugin) {
        super(plugin, "FauxBomb");
        setDescription("Spawn a diseased explosive sheep. Or so it would appear... (The sheep has no actual effects.)");
        setUsage("/skill fauxbomb");
        setArgumentRange(0, 0);
        setIdentifiers("skill fauxbomb");
        setTypes(SkillType.ABILITY_PROPERTY_ILLUSION, SkillType.AREA_OF_EFFECT, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SUMMONING, SkillType.SILENCEABLE);

        Bukkit.getPluginManager().registerEvents(new SkillListener(), plugin);
    }

    public String getDescription(Hero hero) {
        return getDescription();
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), 0);
        node.set(SkillSetting.RADIUS.node(), 5);
        node.set("fuse-time", 6000);
        node.set("velocity", 1.0D);
        node.set(SkillSetting.REAGENT.node(), 367);
        node.set(SkillSetting.REAGENT_COST.node(), 0);

        return node;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        Vector pLoc = player.getLocation().toVector();
        Vector direction = player.getLocation().getDirection();
        World world = player.getWorld();

        final LivingEntity sheep = (LivingEntity) world.spawnEntity(pLoc.toLocation(world), EntityType.SHEEP);
        sheepMap.put(sheep.getEntityId(), player);

        sheep.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 10000, 0));
        //Replaced sheep health with lower value, since sheep doesn't explode and having high health sheep around can be a issue (crowding)
//        sheep.setMaxHealth(10000);
//        sheep.setHealth(10000);
        sheep.setMaxHealth(100);
        sheep.setHealth(100);

        double velocity = SkillConfigManager.getUseSetting(hero, this, "velocity", 1.0D, false);
        sheep.setVelocity(direction.multiply(velocity).add(new Vector(0.0D, 0.15D, 0.0D)));

        int fuse = SkillConfigManager.getUseSetting(hero, this, "fuse-time", 6000, true);

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                //explodeSheep(sheep);
            }
        }, fuse / 1000 * 20);

        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    private class SkillListener implements Listener {
        private SkillListener() {}

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDeath(EntityDeathEvent event) {
            LivingEntity living = event.getEntity();
            if (living instanceof Sheep) {
                Sheep sheep = (Sheep) living;
                if (sheepMap.containsKey(sheep.getEntityId())) {
                    event.setDroppedExp(0);
                    event.getDrops().clear();
                    sheep.remove();
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.getDamage() == 0 || !(event.getEntity() instanceof Sheep))
                return;

            LivingEntity sheep = (LivingEntity) event.getEntity();
            int id = sheep.getEntityId();
            if (sheepMap.containsKey(id)) {
                if (event.getCause() == DamageCause.POISON) {
                    event.setDamage(0.0);
                    event.setCancelled(true);
                }
//Sheep exploding was causing server crashes, commented out until a alternative/fix is found.
//                else {
//                    explodeSheep(sheep);
//                }
            }
        }
    }

//Sheep exploding was causing server crashes, commented out until a alternative/fix is found.
/*
    private void explodeSheep(LivingEntity sheep) {
        int id = sheep.getEntityId();
        if (sheepMap.containsKey(id)) {
            Player player = (Player) sheepMap.get(id);
            Hero hero = plugin.getCharacterManager().getHero(player);

            if (!sheep.isDead()) {
                sheep.getWorld().createExplosion(sheep.getLocation(), 0.0F, false);
                sheep.damage(20000.0);

                int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

                List<Entity> entities = sheep.getNearbyEntities(radius, radius, radius);
                for (Entity entity : entities) {
                    if (!(entity instanceof LivingEntity)) {
                        continue;
                    }

                    // Check if the target is damagable
                    if (!damageCheck(player, (LivingEntity) entity)) {
                        continue;
                    }

                    LivingEntity target = (LivingEntity) entity;

                    if (target.equals(sheep) || (target instanceof Sheep && entity.getEntityId() == id)){
                        continue;
                    }

                    if (hero != null) {
                        addSpellTarget(target, hero);
                        damageEntity(target, player, 0.0, DamageCause.MAGIC);
                    }
                }
            }

            sheepMap.remove(id);
        }
    }
*/
}