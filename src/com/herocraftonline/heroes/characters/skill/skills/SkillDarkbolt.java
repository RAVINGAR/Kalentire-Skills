package com.herocraftonline.heroes.characters.skill.skills;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;

public class SkillDarkbolt extends ActiveSkill {

    private Map<WitherSkull, Long> skulls = new LinkedHashMap<WitherSkull, Long>(100) {
        private static final long serialVersionUID = 842097204945866103L;

        @Override
        protected boolean removeEldestEntry(Entry<WitherSkull, Long> eldest) {
            return (size() > 60 || eldest.getValue() + 5000 <= System.currentTimeMillis());
        }
    };
    
    public SkillDarkbolt(Heroes plugin) {
        super(plugin, "Darkbolt");
        setDescription("You shoot a skull of doom that deals $1 damage!");
        setUsage("/skill darkbolt");
        setArgumentRange(0, 0);
        setIdentifiers("skill darkbolt");
        setTypes(SkillType.DARK, SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.HARMFUL);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DAMAGE.node(), 4);
        node.set(Setting.DAMAGE_INCREASE.node(), 0.0);
        node.set("velocity-multiplier", 1.5);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        WitherSkull skullProj = player.launchProjectile(WitherSkull.class);
        skulls.put(skullProj, System.currentTimeMillis());
        double mult = SkillConfigManager.getUseSetting(hero, this, "velocity-multiplier", 1.5, false);
        skullProj.setVelocity(skullProj.getVelocity().multiply(mult));
        skullProj.setShooter(player);
        broadcastExecuteText(hero); 
        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {

        private final Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(ignoreCancelled = true)
        public void onEntityDamage(EntityDamageEvent event) {
            if (!(event instanceof EntityDamageByEntityEvent) || !(event.getEntity() instanceof LivingEntity)) {
                return;
            }

            EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
            Entity projectile = subEvent.getDamager();
            if (!(projectile instanceof WitherSkull) || !skulls.containsKey(projectile)) {
                return;
            }
            skulls.remove(projectile);
            LivingEntity entity = (LivingEntity) subEvent.getEntity();
            Entity dmger = ((WitherSkull) projectile).getShooter();
            if (dmger instanceof Player) {
                Hero hero = plugin.getCharacterManager().getHero((Player) dmger);

                if (!damageCheck((Player) dmger, entity)) {
                    event.setCancelled(true);
                    return;
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
