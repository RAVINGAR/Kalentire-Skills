package com.herocraftonline.heroes.characters.skill.reborn;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.HealthRegainReductionEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SkillDarkBolt extends ActiveSkill {

    private final Map<WitherSkull, Long> darkBolts = new LinkedHashMap<WitherSkull, Long>(100) {
        private static final long serialVersionUID = 4329526013158603250L;

        @Override
        protected boolean removeEldestEntry(final Map.Entry<WitherSkull, Long> eldest) {
            return (size() > 60 || eldest.getValue() + 5000 <= System.currentTimeMillis());
        }
    };
    private String applyText;
    private String expireText;

    public SkillDarkBolt(final Heroes plugin) {
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
    public String getDescription(final Hero hero) {
        final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 4.0, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 80, false);
        final double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.25, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 17500, false);

        final double healingReductionPercent = SkillConfigManager.getUseSetting(hero, this, "healing-reduction-percent", 0.15, false);

        final String formattedDamage = Util.decFormat.format(damage);
        final String formattedDuration = Util.decFormat.format(duration / 1000.0);
        final String formattedHealingReduction = Util.decFormat.format(healingReductionPercent * 100.0);

        return getDescription().replace("$1", radius + "").replace("$2", formattedDamage).replace("$3", formattedDuration).replace("$4", formattedHealingReduction);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 50);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.0);
        config.set(SkillSetting.RADIUS.node(), 4.0);
        config.set(SkillSetting.DURATION.node(), 7000);
        config.set("wither-level", 1);
        config.set("healing-reduction-percent", 0.35);
        config.set("projectile-velocity", 1.0);
        config.set("projectile-max-ticks-lived", 10);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target%'s begins to wither away!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target%'s is no longer withering.");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%target%'s begins to wither away!").replace("%target%", "$1").replace("$target$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%target%'s is no longer withering.").replace("%target%", "$1").replace("$target$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.4F, 0.8F);

        final WitherSkull darkBolt = player.launchProjectile(WitherSkull.class);
        darkBolts.put(darkBolt, System.currentTimeMillis());
        darkBolt.setShooter(player);

        final double projVel = SkillConfigManager.getUseSetting(hero, this, "projectile-velocity", 1.5, false);
        darkBolt.setVelocity(player.getLocation().getDirection().normalize().multiply(projVel));
        darkBolt.setIsIncendiary(false);
        darkBolt.setCharged(false);
        darkBolt.setYield(0.0F);

        final int ticksLived = SkillConfigManager.getUseSetting(hero, this, "projectile-max-ticks-lived", 20, false);

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (!darkBolt.isDead()) {
                explodeDarkBolt(darkBolt);
                darkBolts.remove(darkBolt);
            }
        }, ticksLived);

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    private void explodeDarkBolt(final WitherSkull darkBolt) {

        final Player player = (Player) darkBolt.getShooter();
        final Hero hero = plugin.getCharacterManager().getHero(player);

        final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 3, false);
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 80, false);
        final double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 17500, false);
        final int witherLevel = SkillConfigManager.getUseSetting(hero, this, "wither-level", 1, false);
        final double healingReductionPercent = SkillConfigManager.getUseSetting(hero, this, "healing-reduction-percent", 0.15, false);

        final Location darkBoltLoc = darkBolt.getLocation();
        darkBoltLoc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, darkBoltLoc, 15, 1, 1, 1, 0);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.1F, 2.0F);

        final List<Location> circle = Util.getCircleLocationList(darkBoltLoc, (int) radius, 0, true, true, 0);
        for (final Location location : circle) {
            darkBolt.getWorld().spawnParticle(Particle.SPELL_WITCH, location, 2, 0.2, 0.3, 0.2, 0);
        }

        final List<Entity> targets = darkBolt.getNearbyEntities(radius, radius, radius);
        for (final Entity entity : targets) {
            if (!(entity instanceof LivingEntity) || !damageCheck(player, (LivingEntity) entity)) {
                continue;
            }

            final LivingEntity target = (LivingEntity) entity;
            final CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);

            // Damage the target
            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.MAGIC);

            // Add withering effect to the target.
            targetCT.addEffect(new WitheringEffect(this, player, duration, witherLevel, healingReductionPercent));
        }

        darkBolt.remove();
    }

    public class SkillEntityListener implements Listener {

        public SkillEntityListener() {
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onProjectileHit(final ProjectileHitEvent event) {
            if (!(event.getEntity() instanceof WitherSkull)) {
                return;
            }

            final WitherSkull darkBolt = (WitherSkull) event.getEntity();
            if ((!(darkBolt.getShooter() instanceof Player))) {
                return;
            }
            if (!(darkBolts.containsKey(darkBolt))) {
                return;
            }

            explodeDarkBolt(darkBolt);

            // Delay the removal from the map so that we ensure that the damage event always catches each darkbolt.
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> darkBolts.remove(darkBolt), 1);
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEntityDamage(final EntityDamageEvent event) {
            if (!(event instanceof EntityDamageByEntityEvent) || !(event.getEntity() instanceof LivingEntity)) {
                return;
            }

            final EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
            final Entity projectile = subEvent.getDamager();
            if (!(projectile instanceof WitherSkull) || !darkBolts.containsKey(projectile)) {
                return;
            }

            final WitherSkull darkBolt = (WitherSkull) projectile;

            explodeDarkBolt(darkBolt);
            darkBolts.remove(projectile);
            event.setCancelled(true);
        }
    }

    public class WitheringEffect extends HealthRegainReductionEffect {

        public WitheringEffect(final Skill skill, final Player applier, final long duration, final int witherLevel, final double healingReductionPercent) {
            super(skill, applier.getName() + "-DarkBoltWithering", applier, duration, healingReductionPercent, applyText, expireText);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.WITHER);

            addPotionEffect(new PotionEffect(PotionEffectType.WITHER, (int) (duration / 50), witherLevel));
        }
    }
}