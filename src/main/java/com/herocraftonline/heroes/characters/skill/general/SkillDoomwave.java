package com.herocraftonline.heroes.characters.skill.general;

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
import org.bukkit.Particle;
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

public class SkillDoomwave extends ActiveSkill {

    private final Map<EnderPearl, Long> doomPearls = new LinkedHashMap<EnderPearl, Long>(100) {
        private static final long serialVersionUID = 4329526013158603250L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<EnderPearl, Long> eldest) {
            return (size() > 60 || eldest.getValue() + 5000 <= System.currentTimeMillis());
        }
    };

    public SkillDoomwave(Heroes plugin) {
        super(plugin, "Doomwave");
        setDescription("Unleash a wave of doom around you. Doomwave will launch $1 fiery ender pearls in all directions around you. Each pearl will deal $2 damage to targets hit, and teleport you to each location.");
        setUsage("/skill doomwave");
        setArgumentRange(0, 0);
        setTypes(SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.ABILITY_PROPERTY_DARK, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_FIRE, SkillType.AREA_OF_EFFECT, SkillType.SILENCEABLE);
        setIdentifiers("skill doomwave");
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {

        int numEnderPearls = SkillConfigManager.getUseSetting(hero, this, "enderpearls-launched", 12, false);
        double numEnderPearlsIncrease = SkillConfigManager.getUseSetting(hero, this, "enderpearls-launched-per-intellect", 0.2, false);
        numEnderPearls += (int) (numEnderPearlsIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 80, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.25, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        return getDescription().replace("$1", numEnderPearls + "").replace("$2", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("enderpearls-launched", 12);
        node.set("enderpearls-launched-per-intellect", 0.325);
        node.set("velocity-multiplier", 1.0);
        node.set(SkillSetting.DAMAGE.node(), 90);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.5);
        node.set(SkillSetting.REAGENT.node(), "GUNPOWDER");
        node.set(SkillSetting.REAGENT_COST.node(), 1);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        double numEnderPearlsIncrease = SkillConfigManager.getUseSetting(hero, this, "enderpearls-launched-per-intellect", 0.325, false);
        final int numEnderPearls = SkillConfigManager.getUseSetting(hero, this, "enderpearls-launched", 12, false)
                + (int) (numEnderPearlsIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        broadcastExecuteText(hero);
        
        

        final long time = System.currentTimeMillis();
        final Random ranGen = new Random((int) ((time / 2.0) * 12));

        final double velocityMultiplier = SkillConfigManager.getUseSetting(hero, this, "velocity-multiplier", 0.75, false);

        // Let's bypass the nocheat issues...
        NCPUtils.applyExemptions(player, () -> {
            for (double i = 0; i < numEnderPearls; i++) {
                EnderPearl doomPearl = player.launchProjectile(EnderPearl.class);
                doomPearl.setFireTicks(100);

                double randomX = ranGen.nextGaussian();
                double randomY = ranGen.nextGaussian();
                double randomZ = ranGen.nextGaussian();

                Vector vel = new Vector(randomX, randomY, randomZ);
                doomPearl.setVelocity(vel.normalize().multiply(velocityMultiplier));

                doomPearls.put(doomPearl, time);
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
            if (!(projectile instanceof EnderPearl) || !doomPearls.containsKey(projectile)) {
                return;
            }

            doomPearls.remove(projectile);
            LivingEntity targetLE = (LivingEntity) subEvent.getEntity();
            //targetLE.getWorld().spigot().playEffect(targetLE.getLocation().add(0, 0.5, 0), Effect.LAVA_POP, 0, 0, 0.2F, 0.2F, 0.2F, 0.4F, 45, 16);
            targetLE.getWorld().spawnParticle(Particle.LAVA, targetLE.getLocation().add(0, 0.5, 0), 35, 0.2, 0.2, 0.2, 0.4);
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
                targetLE.setFireTicks(targetLE.getFireTicks() + SkillConfigManager.getUseSetting(hero, skill, "fire-ticks", 50, false));
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