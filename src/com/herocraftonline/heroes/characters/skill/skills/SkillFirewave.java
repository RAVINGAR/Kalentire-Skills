package com.herocraftonline.heroes.characters.skill.skills;
//http://pastie.org/private/oz5iqyfjto1vgoova1qn2g (original source)
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.CombustEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.LinkedHashMap;
import java.util.Map;

public class SkillFirewave extends ActiveSkill {

    private Map<Snowball, Long> fireballs = new LinkedHashMap<Snowball, Long>(100) {
        private static final long serialVersionUID = 4329526013158603250L;
        @Override
        protected boolean removeEldestEntry(Map.Entry<Snowball, Long> eldest) {
            return (size() > 60 || eldest.getValue() + 5000 <= System.currentTimeMillis());
        }
    };

	public SkillFirewave(Heroes plugin) {
		super(plugin, "Firewave");
        setDescription("Unleash of wave of fire around you, launching $1 fireballs in every direction. Each fireball deals $2 fire damage.");
		setUsage("/skill firewave");
		setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_FIRE, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT);
		setIdentifiers("skill firewave");
		Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
	}

    @Override
    public String getDescription(Hero hero) {

        int numFireballs = SkillConfigManager.getUseSetting(hero, this, "fireballs", 12, false);
        double numFireballsIncrease = SkillConfigManager.getUseSetting(hero, this, "fireballs-per-intellect", 0.2, false);
        numFireballs += (int) (numFireballsIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 80, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.25, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        return getDescription().replace("$1", numFireballs + "").replace("$2", damage + "");
    }

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

        node.set("fireballs", 12);
        node.set("fireballs-per-intellect", 0.325);
        node.set(SkillSetting.DAMAGE.node(), 95);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.25);
        node.set(SkillSetting.REAGENT.node(), 289);
        node.set(SkillSetting.REAGENT_COST.node(), 1);

		return node;
	}

	@Override
	public SkillResult use(Hero hero, String[] args) {
		Player player = hero.getPlayer();
        int numFireballs = SkillConfigManager.getUseSetting(hero, this, "fireballs", 12, false);
        double numFireballsIncrease = SkillConfigManager.getUseSetting(hero, this, "fireballs-per-intellect", 0.325, false);
        numFireballs += (int) (numFireballsIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

		double diff = 2 * Math.PI / numFireballs;
		long time = System.currentTimeMillis(); //<- red = variable type
		for (double a = 0; a < 2 * Math.PI; a += diff) {
			Vector vel = new Vector(Math.cos(a), 0, Math.sin(a));
			Snowball snowball = player.launchProjectile(Snowball.class);
			snowball.setVelocity(vel);
			fireballs.put(snowball, time);
			snowball.setFireTicks(100);
		}

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
            if (!(projectile instanceof Snowball) || !fireballs.containsKey(projectile)) {
                return;
            }

            fireballs.remove(projectile);
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
                
                // Check if entity is immune to further firewave hits
                if (plugin.getCharacterManager().getCharacter(targetLE).hasEffect("FireWaveAntiMultiEffect")) {
                    event.setCancelled(true);
                    return;
                }

                // Ignite the player
                targetLE.setFireTicks(SkillConfigManager.getUseSetting(hero, skill, "fire-ticks", 50, false));
                plugin.getCharacterManager().getCharacter(targetLE).addEffect(new CombustEffect(skill, (Player) dmger));

                // Damage the player
                double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 80, false);
                double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.5, false);
                damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

                // Damage the target
                addSpellTarget(targetLE, hero);
                damageEntity(targetLE, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.MAGIC);
                event.setCancelled(true);

                //Adds an Effect to Prevent Multihit
                plugin.getCharacterManager().getCharacter(targetLE).addEffect(new ExpirableEffect(skill, "FireWaveAntiMultiEffect", (Player) dmger, 500));
            }
        }
    }
}