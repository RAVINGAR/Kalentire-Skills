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
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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
import com.herocraftonline.heroes.util.Messaging;

public class SkillDarkBolt extends ActiveSkill {

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

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 80, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.25, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

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

        WitherSkull darkBolt = player.launchProjectile(WitherSkull.class);
        darkBolt.setShooter(player);

        double mult = SkillConfigManager.getUseSetting(hero, this, "velocity-multiplier", 1.5, false);
        darkBolt.setVelocity(darkBolt.getVelocity().multiply(mult));

        darkBolt.setIsIncendiary(false);
        darkBolt.setYield(0.0F);

        int ticksLived = SkillConfigManager.getUseSetting(hero, this, "ticks-lived", 2, false);
        darkBolt.setTicksLived(ticksLived);

        darkBolts.put(darkBolt, System.currentTimeMillis());

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {

        private final Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onProjectileHit(ProjectileHitEvent event) {
            if (!(event.getEntity() instanceof WitherSkull))
                return;

            final WitherSkull darkBolt = (WitherSkull) event.getEntity();

            if (darkBolts.containsKey(darkBolt)) {
                // Remove it so it doesn't interact with the world at all.
                darkBolt.remove();
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled() || !(event instanceof EntityDamageByEntityEvent) || !(event.getEntity() instanceof LivingEntity)) {
                return;
            }

            EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
            Entity projectile = subEvent.getDamager();
            if (!(projectile instanceof WitherSkull) || !darkBolts.containsKey(projectile)) {
                return;
            }

            LivingEntity targetLE = (LivingEntity) subEvent.getEntity();
            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(targetLE);

            Entity dmger = ((WitherSkull) projectile).getShooter();

            if (dmger instanceof Player) {
                Hero hero = plugin.getCharacterManager().getHero((Player) dmger);

                if (!damageCheck((Player) dmger, targetLE)) {
                    event.setCancelled(true);
                    return;
                }

                double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, Integer.valueOf(80), false);
                double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, Double.valueOf(1.5), false);
                damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

                // Damage the target
                addSpellTarget(targetLE, hero);
                damageEntity(targetLE, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.MAGIC);

                int duration = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DURATION, 17500, false);
                double healingReductionPercent = SkillConfigManager.getUseSetting(hero, skill, "healing-reduction-percent", Double.valueOf(0.15), false);

                targetCT.addEffect(new WitheringEffect(skill, (Player) dmger, duration, healingReductionPercent));

                darkBolts.remove(projectile);
                event.setCancelled(true);
            }
        }
    }

    public class WitheringEffect extends HealthRegainReductionEffect {

        public WitheringEffect(Skill skill, Player applier, long duration, double healingReductionPercent) {
            super(skill, "DarkBoltWithering", applier, duration, healingReductionPercent, applyText, expireText);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.DARK);
            types.add(EffectType.WITHER);

            addMobEffect(9, (int) ((duration + 4000) / 1000) * 20, 3, false);
            addMobEffect(20, (int) (duration / 1000) * 20, 1, false);
        }
    }
}