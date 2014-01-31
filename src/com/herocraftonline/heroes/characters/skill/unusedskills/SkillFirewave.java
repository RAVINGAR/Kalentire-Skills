package com.herocraftonline.heroes.characters.skill.unusedskills;
//http://pastie.org/private/oz5iqyfjto1vgoova1qn2g (original source)
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
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
        setDescription("Unleash of wave of fire around you, launching $1 fireballs in every direction. Each fireball deals $2 fire damage.");
		setUsage("/skill firewave");
		setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_FIRE, SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT);
		setIdentifiers("skill firewave");
		Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);

        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null)
            ncpEnabled = true;
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

        node.set("fireballs", Integer.valueOf(12));
        node.set("fireballs-per-intellect", Double.valueOf(0.325));
        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(95));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), Double.valueOf(1.25));
        node.set(SkillSetting.REAGENT.node(), Integer.valueOf(289));
        node.set(SkillSetting.REAGENT_COST.node(), Integer.valueOf(1));

		return node;
	}

	@Override
	public SkillResult use(Hero hero, String[] args) {
		Player player = hero.getPlayer();
        int numFireballs = SkillConfigManager.getUseSetting(hero, this, "fireballs", 12, false);
        double numFireballsIncrease = SkillConfigManager.getUseSetting(hero, this, "fireballs-per-intellect", 0.325, false);
        numFireballs += (int) (numFireballsIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

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