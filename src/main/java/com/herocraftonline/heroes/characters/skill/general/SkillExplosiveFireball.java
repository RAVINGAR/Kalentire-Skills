package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.CombustEffect;
import com.herocraftonline.heroes.characters.skill.*;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.LinkedHashMap;
import java.util.Map;

public class SkillExplosiveFireball extends ActiveSkill {

    private final Map<Fireball, Long> fireballs = new LinkedHashMap<Fireball, Long>(100) {
        private static final long serialVersionUID = 4329526013158603250L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<Fireball, Long> eldest) {
            return (size() > 60 || eldest.getValue() + 5000 <= System.currentTimeMillis());
        }
    };

    public SkillExplosiveFireball(Heroes plugin) {
        super(plugin, "ExplosiveFireball");
        setDescription("You shoot a ball of fire that deals $1 damage and lights your target on fire");
        setUsage("/skill fireball");
        setArgumentRange(0, 0);
        setIdentifiers("skill fireball");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_FIRE, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 80, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.25, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        return getDescription().replace("$1", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), 80);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.25);
        node.set("velocity-multiplier", 1.5);
        node.set("fire-ticks", 50);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        double mult = SkillConfigManager.getUseSetting(hero, this, "velocity-multiplier", 1.5, false);
        Fireball fireball = player.launchProjectile(Fireball.class);
        fireball.setVelocity(fireball.getVelocity().normalize().multiply(mult));
        fireball.setFireTicks(100);
        fireballs.put(fireball, System.currentTimeMillis());
        fireball.setShooter(player);

        broadcastExecuteText(hero);

        //player.getWorld().spigot().playEffect(player.getLocation(), Effect.BLAZE_SHOOT);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 1);

        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {

        private final Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onGhastProjectileHit(EntityExplodeEvent event) {
            if (!(event.getEntity() instanceof Fireball))
                return;

            LargeFireball fireball = (LargeFireball) event.getEntity();
            if (!fireballs.containsKey(fireball))
                return;

            fireballs.remove(fireball);
            event.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageEvent event) {
            if (!(event instanceof EntityDamageByEntityEvent) || !(event.getEntity() instanceof LivingEntity)) {
                return;
            }

            EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
            Entity projectile = subEvent.getDamager();
            if (!(projectile instanceof Fireball) || !fireballs.containsKey(projectile)) {
                return;
            }

            fireballs.remove(projectile);
            event.setCancelled(true);

            LivingEntity targetLE = (LivingEntity) subEvent.getEntity();
            ProjectileSource source = ((Projectile) subEvent.getDamager()).getShooter();
            if (!(source instanceof Entity))
                return;
            Entity dmger = (LivingEntity) source;

            if (dmger instanceof Player) {
                Hero hero = plugin.getCharacterManager().getHero((Player) dmger);

                if (!damageCheck((Player) dmger, targetLE)) {
                    event.setCancelled(true);
                    return;
                }

                // Ignite the player
                targetLE.setFireTicks(SkillConfigManager.getUseSetting(hero, skill, "fire-ticks", 50, false));
                plugin.getCharacterManager().getCharacter(targetLE).addEffect(new CombustEffect(skill, (Player) dmger));

                double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 80, false);
                double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.5, false);
                damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

                // Damage the target
                addSpellTarget(targetLE, hero);
                damageEntity(targetLE, hero.getPlayer(), damage, DamageCause.MAGIC);

                //targetLE.getWorld().spigot().playEffect(targetLE.getLocation().add(0, 0.5F, 0), Effect.FLAME, 0, 0, 0.2F, 0.2F, 0.2F, 0.1F, 50, 16);
                targetLE.getWorld().spawnParticle(Particle.FLAME, targetLE.getLocation().add(0, 0.5, 0), 60, 0.2, 0.2, 0.2, 0.1);
                targetLE.getWorld().playSound(targetLE.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 7.0F, 1.0F);
            }
        }
    }
}