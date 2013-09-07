package com.herocraftonline.heroes.characters.skill.skills;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

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
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;

public class SkillDarkBolt extends ActiveSkill {

    public VisualEffect fplayer = new VisualEffect();

    private String applyText;
    private String expireText;

    private Map<WitherSkull, Long> darkBolts = new LinkedHashMap<WitherSkull, Long>(100) {
        private static final long serialVersionUID = 4329526013158603250L;

        @Override
        protected boolean removeEldestEntry(Entry<WitherSkull, Long> eldest) {
            return (size() > 60 || eldest.getValue() + 5000 <= System.currentTimeMillis());
        }
    };

    public SkillDarkBolt(Heroes plugin) {
        super(plugin, "DarkBolt");
        setDescription("Launch a Wither Skull imbued with dark energy. Enemies hit by the projectile will be damaged for $1 damage and succumb to withering for $2 seconds. Withering disrupts their sense of health and weakens incomming by $3%.");
        setUsage("/skill darkbolt");
        setArgumentRange(0, 0);
        setIdentifiers("skill darkbolt");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 80, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.25, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        return getDescription().replace("$1", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(80));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), Double.valueOf(1.25));
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(6000));
        node.set("velocity-multiplier", Double.valueOf(2.0));
        node.set("ticks-lived", Integer.valueOf(3));
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%target%'s begins to wither away!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%target%'s is no longer withering.");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%target%'s begins to wither away!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%target%'s is no longer withering.").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        final WitherSkull darkBolt = player.launchProjectile(WitherSkull.class);
        darkBolts.put(darkBolt, System.currentTimeMillis());

        darkBolt.setShooter(player);

        double mult = SkillConfigManager.getUseSetting(hero, this, "velocity-multiplier", 1.5, false);
        darkBolt.setVelocity(darkBolt.getVelocity().multiply(mult));

        darkBolt.setIsIncendiary(false);
        darkBolt.setYield(0.0F);

        int ticksLived = SkillConfigManager.getUseSetting(hero, this, "ticks-lived", 20, false);

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                if (!darkBolt.isDead()) {
                    explodeDarkBolt(darkBolt);
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

            WitherSkull darkBolt = (WitherSkull) event.getEntity();
            if ((!(darkBolt.getShooter() instanceof Player)))
                return;

            if (!(darkBolts.containsKey(darkBolt)))
                return;

            explodeDarkBolt(darkBolt);
        }
    }

    private void explodeDarkBolt(WitherSkull darkBolt) {

        Location darkBoltLoc = darkBolt.getLocation();

        try {
            fplayer.playFirework(darkBoltLoc.getWorld(), darkBoltLoc, FireworkEffect.builder().flicker(false).trail(true).with(FireworkEffect.Type.CREEPER).withColor(Color.BLACK).withFade(Color.PURPLE).build());
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        Player player = (Player) darkBolt.getShooter();
        Hero hero = plugin.getCharacterManager().getHero(player);

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 4, false);
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(80), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, Double.valueOf(1.5), false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 17500, false);
        double healingReductionPercent = SkillConfigManager.getUseSetting(hero, this, "healing-reduction-percent", Double.valueOf(0.15), false);

        List<Entity> targets = darkBolt.getNearbyEntities(radius, radius, radius);
        for (Entity entity : targets) {
            // Check to see if the entity can be damaged
            if (!(entity instanceof LivingEntity) || !damageCheck(player, (LivingEntity) entity))
                continue;

            LivingEntity target = (LivingEntity) entity;
            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);

            // Damage the target
            addSpellTarget(target, hero);
            damageEntity(target, player, damage, EntityDamageEvent.DamageCause.MAGIC);

            // Add withering effect to the target.
            targetCT.addEffect(new WitheringEffect(this, player, duration, healingReductionPercent));
        }

        darkBolt.remove();
        darkBolts.remove(darkBolt);
    }

    public class WitheringEffect extends HealthRegainReductionEffect {

        public WitheringEffect(Skill skill, Player applier, long duration, double healingReductionPercent) {
            super(skill, "DarkBoltWithering", applier, duration, healingReductionPercent, applyText, expireText);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.DARK);
            types.add(EffectType.WITHER);

            addMobEffect(20, (int) (duration / 1000) * 20, 0, false);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            Heroes.log(Level.INFO, "Applied to the hero at least...");
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            Heroes.log(Level.INFO, "Applied to the mob at least...");
        }
    }
}