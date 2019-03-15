//package com.herocraftonline.heroes.characters.skill.reborn.necromancer;
//
//import com.herocraftonline.heroes.Heroes;
//import com.herocraftonline.heroes.api.SkillResult;
//import com.herocraftonline.heroes.attributes.AttributeType;
//import com.herocraftonline.heroes.characters.Hero;
//import com.herocraftonline.heroes.characters.Monster;
//import com.herocraftonline.heroes.characters.skill.*;
//import org.bukkit.*;
//import org.bukkit.configuration.ConfigurationSection;
//import org.bukkit.entity.Entity;
//import org.bukkit.entity.LivingEntity;
//import org.bukkit.entity.Player;
//import org.bukkit.entity.Zombie;
//import org.bukkit.event.EventHandler;
//import org.bukkit.event.EventPriority;
//import org.bukkit.event.Listener;
//import org.bukkit.event.entity.EntityDamageEvent;
//import org.bukkit.event.entity.EntityDeathEvent;
//import org.bukkit.event.player.PlayerShearEntityEvent;
//
//import java.util.HashMap;
//import java.util.Map;
//
//public class SkillSummonNoxiousMinion extends ActiveSkill {
//    private Map<Integer, Player> noxiousBombs = new HashMap<Integer, Player>();
//
//    public SkillSummonNoxiousMinion(Heroes plugin) {
//        super(plugin, "NoxiousMinion");
//        setDescription("Conjures a diseased zombie and throws it. When the zombie dies, it will deal $1 damage in a $2 block radius.");
//        setUsage("/skill noxiousminion");
//        setArgumentRange(0, 0);
//        setIdentifiers("skill noxiousminion");
//
//        setTypes(SkillType.SUMMONING, SkillType.AREA_OF_EFFECT, SkillType.ABILITY_PROPERTY_DISEASE, SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_MAGICAL);
//    }
//
//    public String getDescription(Hero hero) {
//        return getDescription();
//    }
//
//    public ConfigurationSection getDefaultConfig() {
//        ConfigurationSection config = super.getDefaultConfig();
//        config.set("minion-attack-damage", 0.0);
//        config.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.0);
//        config.set(SkillSetting.RADIUS.node(), 5);
//        config.set("zombie-velocity", 1.0);
//        config.set("zombie-duration", 10000);
//        return config;
//    }
//
//    public SkillResult use(Hero hero, String[] args) {
//        Player player = hero.getPlayer();
//
//        double zombieVelocity = SkillConfigManager.getUseSetting(hero, this, "zombie-velocity", 1.0, false);
//        double zombieDuration = SkillConfigManager.getUseSetting(hero, this, "zombie-duration", 10000, false);
//
//        final Zombie zombie = (Zombie) player.getWorld().spawn(player.getEyeLocation(), Zombie.class);
//        final Monster zombieMonster = plugin.getCharacterManager().getMonster(zombie);
//        zombieMonster.setExperience(0);
//
//        noxiousBombs.put(zombie.getEntityId(), player);
//        zombie.setMaxHealth(1000.0D);
//        zombie.setHealth(1000.0D);
//        zombie.setCustomName(ChatColor.DARK_RED + "NoxiousZombie");
//        zombie.setVelocity(player.getLocation().getDirection().normalize().multiply(zombieVelocity));
//
//        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
//            public void run() {
//                if (!zombie.isDead()) {
//                    zombieBomb(zombie);
//                }
//            }
//        }, (long) zombieDuration / 1000L * 20L);
//
//        return SkillResult.NORMAL;
//    }
//
//    public void zombieBomb(Zombie zombie) {
//        Player player = noxiousBombs.get(zombie.getEntityId());
//        Hero hero = plugin.getCharacterManager().getHero(player);
//        zombie.setColor(DyeColor.BLUE);
//        zombie.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, zombie.getLocation(), 3);
//        zombie.getWorld().playSound(zombie.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8F, 1.0F);
//        zombie.damage(1000.0D);
//        noxiousBombs.remove(zombie.getEntityId());
//
//        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);
//        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 250.0, false);
//        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.0, false);
//        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);
//        for (Entity entity : zombie.getNearbyEntities(radius, radius, radius)) {
//            if ((entity instanceof LivingEntity)) {
//                LivingEntity target = (LivingEntity) entity;
//                if (damageCheck(player, target)) {
//                    addSpellTarget(target, hero);
//                    damageEntity(target, player, damage, EntityDamageEvent.DamageCause.MAGIC);
//                }
//            }
//        }
//    }
//}
