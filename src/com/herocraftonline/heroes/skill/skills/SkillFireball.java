package com.herocraftonline.heroes.skill.skills;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.effects.common.CombustEffect;
import com.herocraftonline.heroes.hero.Hero;
import com.herocraftonline.heroes.skill.ActiveSkill;
import com.herocraftonline.heroes.skill.Skill;
import com.herocraftonline.heroes.skill.SkillConfigManager;
import com.herocraftonline.heroes.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;

public class SkillFireball extends ActiveSkill {

    private Map<SmallFireball, Long> fireballs = new LinkedHashMap<SmallFireball, Long>(100) {
        private static final long serialVersionUID = 4329526013158603250L;
        @Override
        protected boolean removeEldestEntry(Entry<SmallFireball, Long> eldest) {
            return (size() > 60 || eldest.getValue() + 5000 <= System.currentTimeMillis());
        }
        
    };
    
    public SkillFireball(Heroes plugin) {
        super(plugin, "Fireball");
        setDescription("You shoot a ball of fire that deals $1 damage and lights your target on fire");
        setUsage("/skill fireball");
        setArgumentRange(0, 0);
        setIdentifiers("skill fireball");
        setTypes(SkillType.FIRE, SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.HARMFUL);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DAMAGE.node(), 4);
        node.set(Setting.DAMAGE_INCREASE.node(), 0.0);
        node.set("velocity-multiplier", 1.5);
        node.set("fire-ticks", 100);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        SmallFireball fireball = player.launchProjectile(SmallFireball.class);
        fireballs.put(fireball, System.currentTimeMillis());
        double mult = SkillConfigManager.getUseSetting(hero, this, "velocity-multiplier", 1.5, false);
        fireball.setVelocity(fireball.getVelocity().multiply(mult));
        broadcastExecuteText(hero); 
        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {

        private final Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler()
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled() || !(event instanceof EntityDamageByEntityEvent) || !(event.getEntity() instanceof LivingEntity)) {
                return;
            }

            EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
            Entity projectile = subEvent.getDamager();
            if (!(projectile instanceof SmallFireball) || !fireballs.containsKey(projectile)) {
                return;
            }
            fireballs.remove(projectile);
            LivingEntity entity = (LivingEntity) subEvent.getEntity();
            Entity dmger = ((SmallFireball) projectile).getShooter();
            if (dmger instanceof Player) {
                Hero hero = plugin.getHeroManager().getHero((Player) dmger);

                if (!damageCheck((Player) dmger, entity)) {
                    event.setCancelled(true);
                    return;
                }

                // Ignite the player
                entity.setFireTicks(SkillConfigManager.getUseSetting(hero, skill, "fire-ticks", 100, false));
                if (entity instanceof Player) {
                    plugin.getHeroManager().getHero((Player) entity).addEffect(new CombustEffect(skill, (Player) dmger));
                } else {
                    plugin.getEffectManager().addEntityEffect(entity, new CombustEffect(skill, (Player) dmger));
                }

                // Damage the player
                addSpellTarget(entity, hero);
                int damage = SkillConfigManager.getUseSetting(hero, skill, Setting.DAMAGE, 4, false);
                damage += (int) (SkillConfigManager.getUseSetting(hero, skill, Setting.DAMAGE_INCREASE, 0.0, false) * hero.getSkillLevel(skill));
                damageEntity(entity, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.MAGIC);
                event.setCancelled(true);
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 1, false);
        damage += (int) (SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE_INCREASE, 0.0, false) * hero.getSkillLevel(this));
        return getDescription().replace("$1", damage + "");
    }
}
