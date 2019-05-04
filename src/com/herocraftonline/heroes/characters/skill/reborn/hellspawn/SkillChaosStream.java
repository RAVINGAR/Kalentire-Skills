package com.herocraftonline.heroes.characters.skill.reborn.hellspawn;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.BurningEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class SkillChaosStream extends ActiveSkill {

    private Map<EnderPearl, Long> projectiles = new LinkedHashMap<EnderPearl, Long>(100) {
        private static final long serialVersionUID = 2329013558608752L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<EnderPearl, Long> eldest) {
            return (size() > 300 || eldest.getValue() + 5000 <= System.currentTimeMillis());
        }
    };

    public SkillChaosStream(Heroes plugin) {
        super(plugin, "ChaosStream");
        setDescription("You shoot $1 orbs of chaos in a stream. "
                + "Each orb deals $2 damage and will ignite them, dealing $3 burning damage over the next $4 second(s). "
                + "Additional hits on the same target will deal $5% less damage per hit. The burning effect will not stack.");
        setUsage("/skill chaosstream");
        setArgumentRange(0, 0);
        setIdentifiers("skill chaosstream");
        setTypes(SkillType.ABILITY_PROPERTY_FIRE, SkillType.ABILITY_PROPERTY_PROJECTILE, SkillType.SILENCEABLE, SkillType.DAMAGING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 80.0, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        int numFireballs = SkillConfigManager.getUseSetting(hero, this, "total-projectile-count", 20, false);

        int burnDuration = SkillConfigManager.getUseSetting(hero, this, "burn-duration", 2000, false);
        double burnMultipliaer = SkillConfigManager.getUseSetting(hero, this, "burn-damage-multiplier", 1.5, false);
        double totalBurnDamage = plugin.getDamageManager().calculateFireTickDamage((int) (burnDuration / 50), burnMultipliaer);

        double effectivenessDecrease = SkillConfigManager.getUseSetting(hero, this, "effectiveness-decrease-per-hit-percent", 0.20, false);

        String formattedDamage = Util.decFormat.format(damage);
        String formattedBurnDamage = Util.decFormat.format(totalBurnDamage);
        String formattedBurnDuration = Util.decFormat.format(burnDuration / 1000);
        String formattedEffectivenessPerHit = Util.decFormat.format(effectivenessDecrease * 100);
        return getDescription()
                .replace("$1", numFireballs + "")
                .replace("$2", formattedDamage)
                .replace("$3", formattedBurnDamage)
                .replace("$4", formattedBurnDuration)
                .replace("$5", formattedEffectivenessPerHit);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 35.0);
        config.set("burn-duration", 1000);
        config.set("burn-damage-multiplier", 1.5);
        config.set("effectiveness-decrease-per-hit-percent", 0.20);
        config.set("total-projectile-count", 15);
        config.set("projectiles-per-launch", 3);
        config.set("proj-spread-min", -0.1);
        config.set("proj-spread-max", 0.1);
        config.set("velocity-multiplier", 0.75);
        config.set("launch-delay-server-ticks", 5);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        double mult = SkillConfigManager.getUseSetting(hero, this, "velocity-multiplier", 0.75, false);
        int numFireballs = SkillConfigManager.getUseSetting(hero, this, "total-projectile-count", 20, false);
        int projectilesPerLaunch = SkillConfigManager.getUseSetting(hero, this, "projectiles-per-launch", 2, false);
        int launchDelay = SkillConfigManager.getUseSetting(hero, this, "launch-delay-server-ticks", 3, false);
        int ticksBeforeDrop = SkillConfigManager.getUseSetting(hero, this, "ticks-before-drop", 6, false);
        final double yValue = SkillConfigManager.getUseSetting(hero, this, "y-value-drop", 0.35, false);

        final double randomMin = SkillConfigManager.getUseSetting(hero, this, "proj-spread-min", -0.1, false);
        final double randomMax = SkillConfigManager.getUseSetting(hero, this, "proj-spread-max", 0.1, false);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1F, 0.533F);

        int totalDelayedLaunchLoops = numFireballs / projectilesPerLaunch;
        for (int launchLoopCount = 0; launchLoopCount < totalDelayedLaunchLoops; launchLoopCount++) {

            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                public void run() {
                    for (int launchedThisLoop = 0; launchedThisLoop < projectilesPerLaunch; launchedThisLoop++) {
                        EnderPearl projectile = player.launchProjectile(EnderPearl.class);

                        projectile.setFireTicks(100);
                        projectiles.put(projectile, System.currentTimeMillis());
                        projectile.setShooter(player);
                        projectile.setGravity(true);

                        Vector newVelocity = player.getLocation().getDirection().normalize()
                                .add(new Vector(ThreadLocalRandom.current().nextDouble(randomMin, randomMax), 0, ThreadLocalRandom.current().nextDouble(randomMin, randomMax)))
                                .multiply(mult);
                        projectile.setVelocity(newVelocity);

                        // Nesting schedulers weeeee.
                        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                            public void run() {
                                if (!projectile.isDead()) {
                                    projectile.setVelocity(projectile.getVelocity().setY(-yValue));
                                }
                            }
                        }, ticksBeforeDrop);
                    }

                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.3F, 0.6F);
                }
            }, launchLoopCount * launchDelay);
        }

        return SkillResult.NORMAL;
    }

    private String buildVictimEffectName(Player player) {
        String baseVictimEffectName = "ChaosStreamVictim";
        return player.getName() + "|" + baseVictimEffectName;
    }

    public class ChaosStreamVictimEffect extends ExpirableEffect {

        private int numStacks = 1;
        private final double effectivenessDecreasePerStack;

        public ChaosStreamVictimEffect(Skill skill, Player applier, double effectivenessDecreasePerStack) {
            super(skill, buildVictimEffectName(applier), applier, 1000);
            this.effectivenessDecreasePerStack = effectivenessDecreasePerStack;

            types.add(EffectType.BENEFICIAL);
        }

        public void addStack() {
            this.numStacks++;
            this.setDuration(this.getDuration());
        }

        public double getCurrentEffectivenessMultiplier() {
            double multiplier = effectivenessDecreasePerStack * numStacks;
            if (multiplier < 0) {
                multiplier = 0;
            } else if (multiplier > 1) {
                multiplier = 1;
            }

            return 1.0 - multiplier;
        }
    }

    public class SkillEntityListener implements Listener {

        private final Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onProjectileHit(ProjectileHitEvent event) {
            if (!(event.getEntity() instanceof EnderPearl))
                return;

            final EnderPearl projectile = (EnderPearl) event.getEntity();
            if ((!(projectile.getShooter() instanceof Player)) || !projectiles.containsKey(projectile))
                return;

            if (event.getHitEntity() == null) {
                projectiles.remove(projectile);
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageByEntityEvent event) {
            if (!(event.getEntity() instanceof LivingEntity)) {
                return;
            }

            Entity projectile = event.getDamager();
            if (!(projectile instanceof EnderPearl) || !projectiles.containsKey(projectile)) {
                return;
            }

            projectiles.remove(projectile);
            event.setCancelled(true);

            ProjectileSource source = ((Projectile) event.getDamager()).getShooter();
            if (!(source instanceof Player))
                return;

            Player dmger = (Player) source;
            LivingEntity targetLE = (LivingEntity) event.getEntity();
            if (!damageCheck((Player) dmger, targetLE))
                return;

            Hero hero = plugin.getCharacterManager().getHero(dmger);
            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(targetLE);

            double effectivenessMultiplier = 1.0;
            String victimEffectName = buildVictimEffectName(dmger);
            if (targetCT.hasEffect(victimEffectName)) {
                ChaosStreamVictimEffect victimEffect = (ChaosStreamVictimEffect) targetCT.getEffect(victimEffectName);
                effectivenessMultiplier = victimEffect.getCurrentEffectivenessMultiplier();
                victimEffect.addStack();
            } else {
                double effectivenessDecrease = SkillConfigManager.getUseSetting(hero, skill, "effectiveness-decrease-per-hit-percent", 0.20, false);
                targetCT.addEffect(new ChaosStreamVictimEffect(skill, dmger, effectivenessDecrease));
            }

            if (effectivenessMultiplier == 0)
                return;

            double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 40.0, false);
            double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.0, false);
            int burnDuration = SkillConfigManager.getUseSetting(hero, skill, "burn-duration", 1000, false);
            double burnMultipliaer = SkillConfigManager.getUseSetting(hero, skill, "burn-damage-multiplier", 2.0, false);

            addSpellTarget(targetLE, hero);
            damageEntity(targetLE, dmger, damage * effectivenessMultiplier, DamageCause.MAGIC);

            // Effectiveness multiplier should not apply to the combust debuff.
            if (effectivenessMultiplier != 1.0)
                targetCT.addEffect(new BurningEffect(skill, dmger, burnDuration, false, burnMultipliaer));

            targetLE.getWorld().spawnParticle(Particle.FLAME, targetLE.getLocation(), 50, 0.2F, 0.7F, 0.2F, 16);
            targetLE.getWorld().playSound(targetLE.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 7.0F, 1.0F);
        }
    }
}