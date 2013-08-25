package com.herocraftonline.heroes.characters.skill.unfinishedskills;
//http://pastie.org/private/oz5iqyfjto1vgoova1qn2g (original source)
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.CombustEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;

public class SkillFirewave extends ActiveSkill {

    private boolean ncpEnabled = false;

    private Map<Snowball, Long> fireballs = new LinkedHashMap<Snowball, Long>(100) {
        private static final long serialVersionUID = 4329526013158603250L;
        @Override
        protected boolean removeEldestEntry(Entry<Snowball, Long> eldest) {
            return (size() > 60 || eldest.getValue() + 5000 <= System.currentTimeMillis());
        }
    };

	public SkillFirewave(Heroes plugin) {
		super(plugin, "Firewave");
		setDescription("You throw a wave of fire that deals $1 fire damage in all directions.");
		setUsage("/skill firewave");
		setArgumentRange(0, 0);
		setTypes(SkillType.FIRE, SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.HARMFUL);
		setIdentifiers("skill firewave");
		Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);

        try {
            if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null) {
                ncpEnabled = true;
            }
        }
        catch (Exception e) {}
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();
		node.set("fireballs", 8);
		node.set("fireballs-per-level", .2);
        node.set(SkillSetting.DAMAGE.node(), 4);
        node.set(SkillSetting.DAMAGE_INCREASE.node(), 0.0);
		return node;
	}

	@Override
	public SkillResult use(Hero hero, String[] args) {
		Player player = hero.getPlayer();
		int numFireballs = SkillConfigManager.getUseSetting(hero, this, "fireballs", 8, false);
		numFireballs += (SkillConfigManager.getUseSetting(hero, this, "fireballs-per-level", .2, false) * hero.getSkillLevel(this));

        // Let's bypass the nocheat issues...
        if (ncpEnabled) {
            if (!player.isOp()) {
                NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(this);
                hero.addEffect(ncpExemptEffect);
            }
        }

		double diff = 2 * Math.PI / numFireballs;
		long time = System.currentTimeMillis(); //<- red = variable type
		for (double a = 0; a < 2 * Math.PI; a += diff) {
			Vector vel = new Vector(Math.cos(a), 0, Math.sin(a));
			Snowball snowball = player.launchProjectile(Snowball.class);
			snowball.setVelocity(vel);
			fireballs.put(snowball, time);
			snowball.setFireTicks(100);
		}

        // Let's bypass the nocheat issues...
        if (ncpEnabled) {
            if (!player.isOp()) {
                if (hero.hasEffect("NCPExemptionEffect_FIGHT"))
                    hero.removeEffect(hero.getEffect("NCPExemptionEffect_FIGHT"));
            }
        }

		return SkillResult.NORMAL;
	}

	@Override
	public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 1, false);
        damage += (int) (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0.0, false) * hero.getSkillLevel(this));
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
            if (!(projectile instanceof Snowball) || !fireballs.containsKey(projectile)) {
                return;
            }

            fireballs.remove(projectile);
            LivingEntity entity = (LivingEntity) subEvent.getEntity();
            Entity dmger = ((Snowball) projectile).getShooter();

            if (dmger instanceof Player) {
                Hero hero = plugin.getCharacterManager().getHero((Player) dmger);

                if (!damageCheck((Player) dmger, entity)) {
                    event.setCancelled(true);
                    return;
                    //CharacterTemplate cm = plugin.getCharacterManager().getCharacter(entity);
                    //cm.hasEffect/cm.addEffect
                }
                
                // Check if entity is immune to further firewave hits
                if(plugin.getCharacterManager().getCharacter(entity).hasEffect("FireWaveAntiMultiEffect")) {
                	//cm.hasEffect/cm.addEffect
                    event.setCancelled(true);
                    return;
                }

                // Ignite the player
                entity.setFireTicks(SkillConfigManager.getUseSetting(hero, skill, "fire-ticks", 100, false));
                //cm.hasEffect/cm.addEffect
                plugin.getCharacterManager().getCharacter(entity).addEffect(new CombustEffect(skill, (Player) dmger));

                // Damage the player
                addSpellTarget(entity, hero);
                double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 4, false);
                damage += (SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE, 0.0, false) * hero.getSkillLevel(skill));
                damageEntity(entity, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.MAGIC);
                event.setCancelled(true);

                //Adds an Effect to Prevent Multihit
                //cm.hasEffect/cm.addEffect
                plugin.getCharacterManager().getCharacter(entity).addEffect(new ExpirableEffect(skill, "FireWaveAntiMultiEffect", 500));
            }
        }
    }

    private class NCPExemptionEffect extends Effect {

        public NCPExemptionEffect(Skill skill) {
            super(skill, "NCPExemptionEffect_FIGHT");
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.exemptPermanently(player, CheckType.FIGHT);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.unexempt(player, CheckType.FIGHT);
        }
    }
}