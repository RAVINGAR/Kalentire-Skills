/*package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.CombustEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillExplosiveFireball extends ActiveSkill {

    public SkillExplosiveFireball(Heroes plugin) {
        super(plugin, "ExplosiveFireball");
        setDescription("You shoot an explosive ball of fire which deals $1 damage.");
        setUsage("/skill fireball");
        setArgumentRange(0, 0);
        setIdentifiers("skill explosivefireball");
        setTypes(SkillType.FIRE, SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.HARMFUL);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE.node(), 4);
        node.set("fire-ticks", 100);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        
        Fireball fireball = player.launchProjectile(Fireball.class);
        fireball.setIsIncendiary(false);
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {

        private final Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.LOW)
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof LivingEntity)) {
                return;
            }

            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
                Entity attacker = subEvent.getDamager();
                if (attacker instanceof Fireball) {
                    Fireball fireball = (Fireball) attacker;
                    if (fireball.getShooter() instanceof Player) {
                        Entity entity = event.getEntity();
                        Player shooter = (Player) fireball.getShooter();
                        Hero hero = plugin.getCharacterManager().getHero(shooter);
                        if (!damageCheck(shooter, (LivingEntity) entity)) {
                            event.setCancelled(true);
                        }
                        int damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 4, false);
                        entity.setFireTicks(SkillConfigManager.getUseSetting(hero, skill, "fire-ticks", 100, false));
                        if (entity instanceof LivingEntity)
                        plugin.getCharacterManager().getCharacter((LivingEntity) entity).addEffect(new CombustEffect(skill, shooter));
                        event.setDamage(damage);
                    }
                }
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 4, false);
        return getDescription().replace("$1", damage + "");
    }
}
*/