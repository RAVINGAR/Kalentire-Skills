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
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.CombustEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;

public class SkillDecimation extends ActiveSkill {

    private Map<WitherSkull, Long> fireballs = new LinkedHashMap<WitherSkull, Long>(100) {
        private static final long serialVersionUID = 4329526013158603250L;
        @Override
        protected boolean removeEldestEntry(Entry<WitherSkull, Long> eldest) {
            return (size() > 60 || eldest.getValue() + 5000 <= System.currentTimeMillis());
        }
    };

	public SkillDecimation(Heroes plugin) {
		super(plugin, "Decimation");
		setDescription("You throw a wave of fire that deals $1 fire damage in all directions.");
		setUsage("/skill decimation");
		setArgumentRange(0, 0);
		setTypes(SkillType.DARK, SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.HARMFUL);
		setIdentifiers("skill decimation");
		Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();
		node.set("fireballs", 8);
		node.set("fireballs-per-level", .2);
        node.set(Setting.DAMAGE.node(), 4);
        node.set(Setting.DAMAGE_INCREASE.node(), 0.0);
		return node;
	}

	@Override
	public SkillResult use(Hero hero, String[] args) {
		Player player = hero.getPlayer();
		int numFireballs = SkillConfigManager.getUseSetting(hero, this, "fireballs", 8, false);
		numFireballs += (SkillConfigManager.getUseSetting(hero, this, "fireballs-per-level", .2, false) * hero.getSkillLevel(this));

		double diff = 2 * Math.PI / numFireballs;
		long time = System.currentTimeMillis(); //<- red = variable type
		for (double a = 0; a < 2 * Math.PI; a += diff) {
			Vector vel = new Vector(Math.cos(a), 0, Math.sin(a));
			WitherSkull snowball = player.launchProjectile(WitherSkull.class);
			snowball.setVelocity(vel);
			fireballs.put(snowball, time);
			snowball.setFireTicks(100);
		}
		broadcastExecuteText(hero);
		return SkillResult.NORMAL;
	}

	@Override
	public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 1, false);
        damage += (int) (SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE_INCREASE, 0.0, false) * hero.getSkillLevel(this));
        return getDescription().replace("$1", damage + "");
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
            if (!(projectile instanceof WitherSkull) || !fireballs.containsKey(projectile)) {
                return;
            }
            fireballs.remove(projectile);
            LivingEntity entity = (LivingEntity) subEvent.getEntity();
            Entity dmger = ((WitherSkull) projectile).getShooter();
            if (dmger instanceof Player) {
                Hero hero = plugin.getCharacterManager().getHero((Player) dmger);

                if (!damageCheck((Player) dmger, entity)) {
                    event.setCancelled(true);
                    return;
                }

                // Ignite the player
                entity.setFireTicks(SkillConfigManager.getUseSetting(hero, skill, "fire-ticks", 100, false));
                plugin.getCharacterManager().getCharacter(entity).addEffect(new CombustEffect(skill, (Player) dmger));

                // Damage the player
                addSpellTarget(entity, hero);
                int damage = SkillConfigManager.getUseSetting(hero, skill, Setting.DAMAGE, 4, false);
                damage += (int) (SkillConfigManager.getUseSetting(hero, skill, Setting.DAMAGE_INCREASE, 0.0, false) * hero.getSkillLevel(skill));
                damageEntity(entity, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.MAGIC);
                event.setCancelled(true);
            }
        }
    }
}
