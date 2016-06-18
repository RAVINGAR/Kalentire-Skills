package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;
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

public class SkillPlagueBomb extends ActiveSkill {
    private Map<Integer, Player> sheepMap = new HashMap<Integer, Player>();

    public SkillPlagueBomb(Heroes plugin) {
        super(plugin, "PlagueBomb");
        setDescription("You spawn a diseased explosive sheep. The sheep will detonate after $1 seconds, or after taking damage. Upon detonation, the sheep will deal $2 damage to all enemies within $3 blocks.");
        setUsage("/skill plaguebomb");
        setArgumentRange(0, 0);
        setIdentifiers("skill plaguebomb");
        setTypes(SkillType.AGGRESSIVE, SkillType.ABILITY_PROPERTY_ILLUSION, SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.AREA_OF_EFFECT);

        Bukkit.getPluginManager().registerEvents(new SkillListener(), plugin);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), 100);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 2.75);
        node.set(SkillSetting.RADIUS.node(), 5);
        node.set("fuse-time", 6000);
        node.set("velocity", 1.0D);
        node.set(SkillSetting.REAGENT.node(), 367);
        node.set(SkillSetting.REAGENT_COST.node(), 0);

        return node;
    }

    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 100, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 2.75, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        double fuseTime = SkillConfigManager.getUseSetting(hero, this, "fuse-time", 5000, true) / 1000;
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

        String formattedDamage = Util.decFormat.format(damage);

        return getDescription().replace("$1", fuseTime + "").replace("$2", formattedDamage).replace("$3", radius + "");
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        Vector pLoc = player.getLocation().toVector();
        Vector direction = player.getLocation().getDirection();
        World world = player.getWorld();

        final LivingEntity sheep = (LivingEntity) world.spawnEntity(pLoc.toLocation(world), EntityType.SHEEP);
        sheepMap.put(sheep.getEntityId(), player);

        //sheep.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 10000, 0));
        sheep.setMaxHealth(10000);
        sheep.setHealth(10000);

        double velocity = SkillConfigManager.getUseSetting(hero, this, "velocity", 1.0D, false);
        sheep.setVelocity(direction.multiply(velocity).add(new Vector(0.0D, 0.15D, 0.0D)));

        int fuse = SkillConfigManager.getUseSetting(hero, this, "fuse-time", 6000, true);

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                explodeSheep(sheep);
            }
        }, fuse / 1000 * 20);

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
                else
                    explodeSheep(sheep);
            }
        }
    }

    private void explodeSheep(LivingEntity sheep) {
        int id = sheep.getEntityId();
        if (sheepMap.containsKey(id)) {
            Player player = sheepMap.get(id);
            Hero hero = plugin.getCharacterManager().getHero(player);
            double damage = 1;
            if (hero != null) {
                damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 100, false);
                double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 2.75, false);
                damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);
            }

            if (!sheep.isDead()) {
                sheep.getWorld().createExplosion(sheep.getLocation(), 0.0F, false);
                sheep.damage(20000.0);

                int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

                List<Entity> entities = sheep.getNearbyEntities(radius, radius, radius);
                for (Entity entity : entities) {
                    if (!(entity instanceof LivingEntity))
                        continue;

                    // Check if the target is damagable
                    if (!damageCheck(player, (LivingEntity) entity))
                        continue;

                    LivingEntity target = (LivingEntity) entity;

                    if (hero != null) {
                        // Damage the target
                        addSpellTarget(target, hero);
                        damageEntity(target, player, damage, DamageCause.MAGIC);
                    }
                }
            }

            sheepMap.remove(id);
        }
    }
}