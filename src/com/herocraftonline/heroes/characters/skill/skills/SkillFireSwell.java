package com.herocraftonline.heroes.characters.skill.skills;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.CombustEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.ncp.NCPFunction;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class SkillFireSwell extends ActiveSkill {
    
    private Map<Fireball, Long> fireballs = new LinkedHashMap<Fireball, Long>(100) {
        private static final long serialVersionUID = 4329526013158603250L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<Fireball, Long> eldest) {
            return (size() > 60 || eldest.getValue() + 5000 <= System.currentTimeMillis());
        }
    };

    public SkillFireSwell(Heroes plugin) {
        super(plugin, "FireSwell");
        setDescription("You release a swell of fireballs. Launching $1 fiery projectiles in all " +
                "directions around you. Each projectile deals $2 damage to targets hit.");
        setUsage("/skill fireswell");
        setArgumentRange(0, 0);
        setTypes(SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.ABILITY_PROPERTY_FIRE, 
                SkillType.AREA_OF_EFFECT, SkillType.SILENCEABLE);
        setIdentifiers("skill fireswell");
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {

        int numFireballs = SkillConfigManager.getUseSetting(hero, this, "fireballs-launched", 12, false);
        double numFireballsIncrease = SkillConfigManager.getUseSetting(hero, this, "fireballs-launched-per-intellect", 0.2, false);
        numFireballs += (int) (numFireballsIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 80, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.25, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        return getDescription().replace("$1", numFireballs + "").replace("$2", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("fireballs-launched", 12);
        node.set("fireballs-launched-per-intellect", 0.325);
        node.set("velocity-multiplier", 1.0);
        node.set(SkillSetting.DAMAGE.node(), 90);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.5);
        node.set(SkillSetting.REAGENT.node(), 289);
        node.set(SkillSetting.REAGENT_COST.node(), 1);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        double numFireballsIncrease = SkillConfigManager.getUseSetting(hero, this, "fireballs-launched-per-intellect", 0.325, false);
        final int numFireballs = SkillConfigManager.getUseSetting(hero, this, "fireballs-launched", 12, false)
                + (int) (numFireballsIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        broadcastExecuteText(hero);
        
        

        final long time = System.currentTimeMillis();
        final Random ranGen = new Random((int) ((time / 2.0) * 12));

        final double velocityMultiplier = SkillConfigManager.getUseSetting(hero, this, "velocity-multiplier", 0.75, false);

        // Let's bypass the nocheat issues...
        NCPUtils.applyExemptions(player, new NCPFunction() {

            @Override
            public void execute()
            {
                for (double i = 0; i < numFireballs; i++) {
                    Fireball fireBall = player.launchProjectile(Fireball.class);
                    fireBall.setFireTicks(100);

                    double randomX = ranGen.nextGaussian();
                    double randomY = ranGen.nextGaussian();
                    double randomZ = ranGen.nextGaussian();

                    Vector vel = new Vector(randomX, randomY, randomZ);
                    fireBall.setVelocity(vel.multiply(velocityMultiplier));

                    fireballs.put(fireBall, time);
                }
            }
        }, Lists.newArrayList("BLOCKPLACE_SPEED"), 0);

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
            if (!(projectile instanceof Fireball) || !fireballs.containsKey(projectile)) {
                return;
            }

            fireballs.remove(projectile);
            LivingEntity targetLE = (LivingEntity) subEvent.getEntity();
            //targetLE.getWorld().spigot().playEffect(targetLE.getLocation().add(0, 0.5, 0), Effect.LAVA_POP, 0, 0, 0.2F, 0.2F, 0.2F, 0.4F, 45, 16);
            targetLE.getWorld().spawnParticle(Particle.LAVA, targetLE.getLocation().add(0, 0.5, 0), 45, 0.2, 0.2, 0.2, 0.4);
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
                if (plugin.getCharacterManager().getCharacter(targetLE).hasEffect("DoomWaveAntiMultiEffect")) {
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
                damageEntity(targetLE, hero.getPlayer(), damage, DamageCause.MAGIC);
                event.setCancelled(true);

                //Adds an Effect to Prevent Multihit
                plugin.getCharacterManager().getCharacter(targetLE).addEffect(new ExpirableEffect(skill, "DoomWaveAntiMultiEffect", (Player) dmger, 500));
            }
        }
    }
}