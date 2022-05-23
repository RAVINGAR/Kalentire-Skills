package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;

import java.util.HashMap;
import java.util.Map;

public class SkillPlagueBomb extends ActiveSkill {
    private Map<Integer, Player> plagueBombs = new HashMap<Integer, Player>();

    public SkillPlagueBomb(Heroes plugin) {
        super(plugin, "PlagueBomb");
        setDescription("Conjures a diseased sheep and throws it. When the sheep dies, it will deal $1 damage in a $2 block radius.");
        setUsage("/skill plaguebomb");
        setArgumentRange(0, 0);
        setIdentifiers("skill plaguebomb");

        setTypes(SkillType.DAMAGING, SkillType.AREA_OF_EFFECT, SkillType.ABILITY_PROPERTY_DISEASE, SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_MAGICAL);
        Bukkit.getServer().getPluginManager().registerEvents(new PlagueBombListener(this), plugin);
    }

    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        double radius = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.RADIUS, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(damage))
                .replace("$2", Util.decFormat.format(radius));
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 250.0);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.0);
        config.set(SkillSetting.RADIUS.node(), 5);
        config.set("sheep-velocity", 1.0);
        config.set("sheep-duration", 10000);
        return config;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        double launchVelocity = SkillConfigManager.getUseSetting(hero, this, "sheep-velocity", 1.0, false);
        long maxDuration = SkillConfigManager.getUseSetting(hero, this, "sheep-duration", 10000, false);

        final Sheep sheep = (Sheep) player.getWorld().spawn(player.getEyeLocation(), Sheep.class);
        final Monster sheepMonster = plugin.getCharacterManager().getMonster(sheep);
        sheepMonster.setExperience(0);

        plagueBombs.put(sheep.getEntityId(), player);
        sheep.setMaxHealth(1000.0D);
        sheep.setHealth(1000.0D);
        sheep.setCustomName(ChatColor.DARK_RED + "Plague Bomb");
        sheep.setCustomNameVisible(true);
        sheep.setVelocity(player.getLocation().getDirection().normalize().multiply(launchVelocity));

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                if (!sheep.isDead()) {
                    sheepBomb(sheep);
                }
            }
        }, (long) maxDuration / 1000L * 20L);

        return SkillResult.NORMAL;
    }

    public void sheepBomb(Sheep sheep) {
        Player player = plagueBombs.get(sheep.getEntityId());
        Hero hero = plugin.getCharacterManager().getHero(player);
        sheep.setColor(DyeColor.GREEN);
        sheep.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, sheep.getLocation(), 3);
        sheep.getWorld().playSound(sheep.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8F, 1.0F);
        sheep.damage(1000.0D);
        plagueBombs.remove(sheep.getEntityId());

        double radius = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.RADIUS, false);
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        for (Entity entity : sheep.getNearbyEntities(radius, radius, radius)) {
            if ((entity instanceof LivingEntity)) {
                LivingEntity target = (LivingEntity) entity;
                if (damageCheck(player, target)) {
                    addSpellTarget(target, hero);
                    damageEntity(target, player, damage, EntityDamageEvent.DamageCause.MAGIC);
                }
            }
        }
    }

    private class PlagueBombListener implements Listener {
        private Skill skill;

        PlagueBombListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onShear(PlayerShearEntityEvent event) {
            if (!(event.getEntity() instanceof Sheep))
                return;

            Sheep sheep = (Sheep) event.getEntity();
            if (plagueBombs.containsKey(sheep.getEntityId())) {
                event.setCancelled(true);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityDeath(EntityDeathEvent event) {
            if (!(event.getEntity() instanceof Sheep))
                return;

            Sheep sheep = (Sheep) event.getEntity();
            if (plagueBombs.containsKey(sheep.getEntityId())) {
                event.setDroppedExp(0);
                event.getDrops().clear();
                sheepBomb(sheep);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityDamage(EntityDamageEvent event) {
            if (!(event.getEntity() instanceof Sheep))
                return;

            Sheep sheep = (Sheep) event.getEntity();
            if (!plagueBombs.containsKey(sheep.getEntityId()) || sheep.isDead())
                return;

            event.setDamage(1000.0D);
        }
    }
}
