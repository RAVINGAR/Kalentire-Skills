package com.herocraftonline.heroes.characters.skill.reborn.pyromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.HealthRegainReductionEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SkillDarkBolt extends ActiveSkill {

    private String applyText;
    private String expireText;

    private Map<WitherSkull, Long> darkBolts = new LinkedHashMap<WitherSkull, Long>(100) {
        private static final long serialVersionUID = 4329526013158603250L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<WitherSkull, Long> eldest) {
            return (size() > 60 || eldest.getValue() + 5000 <= System.currentTimeMillis());
        }
    };

    public SkillDarkBolt(Heroes plugin) {
        super(plugin, "DarkBolt");
        setDescription("Launch a Wither Skull imbued with dark energy. The skull will explode shortly after launching, or after hitting an enemy. "
                + "Enemies caught within $1 blocks of the explosion are dealt $2 damage and withered for $3 second(s). "
                + "Withering disrupts their sense of health, and weakens all incomming healing by $4%.");
        setUsage("/skill darkbolt");
        setArgumentRange(0, 0);
        setIdentifiers("skill darkbolt");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.ABILITY_PROPERTY_WITHER, SkillType.ABILITY_PROPERTY_PROJECTILE,
                SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AREA_OF_EFFECT);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 4, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 80, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.25, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 17500, false);

        double healingReductionPercent = SkillConfigManager.getUseSetting(hero, this, "healing-reduction-percent", 0.15, false);

        String formattedDamage = Util.decFormat.format(damage);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedHealingReduction = Util.decFormat.format(healingReductionPercent * 100.0);

        return getDescription().replace("$1", radius + "").replace("$2", formattedDamage).replace("$3", formattedDuration).replace("$4", formattedHealingReduction);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 80);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.25);
        config.set(SkillSetting.RADIUS.node(), 3);
        config.set(SkillSetting.DURATION.node(), 6000);
        config.set("wither-level", 1);
        config.set("healing-reduction-percent", 0.15);
        config.set("projectile-velocity", 1.5);
        config.set("projectile-ticks-lived", 20);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target%'s begins to wither away!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target%'s is no longer withering.");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%target%'s begins to wither away!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%target%'s is no longer withering.").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.4F, 0.8F);

        final WitherSkull darkBolt = player.launchProjectile(WitherSkull.class);
        darkBolts.put(darkBolt, System.currentTimeMillis());
        darkBolt.setShooter(player);

        double projVel = SkillConfigManager.getUseSetting(hero, this, "projectile-velocity", 1.5, false);
        darkBolt.setVelocity(player.getLocation().getDirection().normalize().multiply(projVel));
        darkBolt.setIsIncendiary(false);
        darkBolt.setCharged(false);
        darkBolt.setYield(0.0F);

        int ticksLived = SkillConfigManager.getUseSetting(hero, this, "projectile-ticks-lived", 20, false);

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                if (!darkBolt.isDead()) {
                    explodeDarkBolt(darkBolt);
                    darkBolts.remove(darkBolt);
                }
            }
        }, ticksLived);

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {

        public SkillEntityListener() {}

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onProjectileHit(ProjectileHitEvent event) {
            if (!(event.getEntity() instanceof WitherSkull))
                return;

            final WitherSkull darkBolt = (WitherSkull) event.getEntity();
            if ((!(darkBolt.getShooter() instanceof Player)))
                return;
            if (!(darkBolts.containsKey(darkBolt)))
                return;

            explodeDarkBolt(darkBolt);

            // Delay the removal from the map so that we ensure that the damage event always catches each darkbolt.
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                public void run() {
                    darkBolts.remove(darkBolt);
                }
            }, 1);
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageEvent event) {
            if (!(event instanceof EntityDamageByEntityEvent) || !(event.getEntity() instanceof LivingEntity))
                return;

            EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
            Entity projectile = subEvent.getDamager();
            if (!(projectile instanceof WitherSkull) || !darkBolts.containsKey(projectile))
                return;

            final WitherSkull darkBolt = (WitherSkull) projectile;

            explodeDarkBolt(darkBolt);
            darkBolts.remove(projectile);
            event.setCancelled(true);
        }
    }

    private void explodeDarkBolt(WitherSkull darkBolt) {

        Player player = (Player) darkBolt.getShooter();
        Hero hero = plugin.getCharacterManager().getHero(player);

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 3, false);
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 80, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 17500, false);
        int witherLevel = SkillConfigManager.getUseSetting(hero, this, "wither-level", 1, false);
        double healingReductionPercent = SkillConfigManager.getUseSetting(hero, this, "healing-reduction-percent", 0.15, false);

        Location darkBoltLoc = darkBolt.getLocation();
        darkBoltLoc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, darkBoltLoc, 15, 1, 1, 1, 0);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.1F, 2.0F);

        List<Location> circle = Util.getCircleLocationList(darkBoltLoc, radius, 0, true, true, 0);
        for (Location location : circle) {
            darkBolt.getWorld().spawnParticle(Particle.SPELL_WITCH, location, 2, 0.2, 0.3, 0.2, 0);
        }

        List<Entity> targets = darkBolt.getNearbyEntities(radius, radius, radius);
        for (Entity entity : targets) {
            if (!(entity instanceof LivingEntity) || !damageCheck(player, (LivingEntity) entity))
                continue;

            LivingEntity target = (LivingEntity) entity;
            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);

            // Damage the target
            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.MAGIC);

            // Add withering effect to the target.
            targetCT.addEffect(new WitheringEffect(this, player, duration, witherLevel, healingReductionPercent));
        }

        darkBolt.remove();
    }

    public class WitheringEffect extends HealthRegainReductionEffect {

        public WitheringEffect(Skill skill, Player applier, long duration, int witherLevel, double healingReductionPercent) {
            super(skill, applier.getName() + "-DarkBoltWithering", applier, duration, healingReductionPercent, applyText, expireText);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.WITHER);

            addPotionEffect(new PotionEffect(PotionEffectType.WITHER, (int) (duration / 50), witherLevel));
        }
    }
}