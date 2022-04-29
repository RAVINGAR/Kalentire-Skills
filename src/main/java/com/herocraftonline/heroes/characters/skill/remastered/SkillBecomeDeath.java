package com.herocraftonline.heroes.characters.skill.remastered;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;

import java.util.LinkedHashMap;
import java.util.Map;

public class SkillBecomeDeath extends PassiveSkill implements Listenable {

    private final Listener listener;

    public SkillBecomeDeath(Heroes plugin) {
        super(plugin, "BecomeDeath");
        setDescription("Undead do not see you unless you provoke them. Additionally, you can breathe underwater.");
        setTypes(SkillType.SILENCEABLE, SkillType.BUFFING, SkillType.ABILITY_PROPERTY_DARK);
        listener = new SkillEntityListener(this);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        //config.set(SkillSetting.DURATION.node(), 120000);
        config.set(SkillSetting.APPLY_TEXT.node(), "");
        config.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        return config;
    }

    @Override
    public void apply(Hero hero) {
        // Note we don't want the default passive effect, we're making our own with a custom constructor
        hero.addEffect(new BecomeDeathEffect(this, hero.getPlayer()));
    }

    @Override
    public Listener getListener() {
        return listener;
    }

    public class SkillEntityListener implements Listener {
        private final PassiveSkill skill;

        public SkillEntityListener(PassiveSkill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEntityTarget(EntityTargetEvent event) {
            if (!(event.getEntity() instanceof LivingEntity) || !(event.getTarget() instanceof Player))
                return;

            final LivingEntity entity = (LivingEntity) event.getEntity();
            if (!(Util.isUndead(plugin, entity)))
                return;

            Hero hero = plugin.getCharacterManager().getHero((Player) event.getTarget());
            if (!hero.hasEffect(skill.getName()))
                return;

            BecomeDeathEffect bdEffect = (BecomeDeathEffect) hero.getEffect(skill.getName());
            assert bdEffect != null;
            if (!bdEffect.hasProvokedMob(entity))
                event.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.getDamage() == 0 || !(event.getEntity() instanceof LivingEntity))
                return;

            final LivingEntity entity = (LivingEntity) event.getEntity();
            if (!Util.isUndead(plugin, entity) || !(event instanceof EntityDamageByEntityEvent))
                return;

            EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
            if (subEvent.getDamager() instanceof Player) {
                Hero hero = plugin.getCharacterManager().getHero((Player) subEvent.getDamager());
                if (skill.hasPassive(hero)) {
                    BecomeDeathEffect bdEffect = (BecomeDeathEffect) hero.getEffect(skill.getName());
                    assert bdEffect != null;
                    bdEffect.addProvokedMob(entity);
                }
            }
            else if (subEvent.getDamager() instanceof Projectile) {
                if (((Projectile) subEvent.getDamager()).getShooter() instanceof Player) {
                    Hero hero = plugin.getCharacterManager().getHero((Player) ((Projectile) subEvent.getDamager()).getShooter());
                    if (skill.hasPassive(hero)) {
                        BecomeDeathEffect bdEffect = (BecomeDeathEffect) hero.getEffect(skill.getName());
                        assert bdEffect != null;
                        bdEffect.addProvokedMob(entity);
                    }
                }
            }
        }
    }

    public class BecomeDeathEffect extends PassiveEffect {
        private static final long cooldownMilliseconds = 5000L;
        private final Map<LivingEntity, Long> provokedMobs = new LinkedHashMap<LivingEntity, Long>(10) {
            private static final long serialVersionUID = 2196792527721771866L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<LivingEntity, Long> eldest) {
                return (size() > 30 || eldest.getValue() + cooldownMilliseconds <= System.currentTimeMillis());
            }
        };

        public BecomeDeathEffect(Skill skill, Player applier) {
            super(skill, applier, null);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.DARK);
            types.add(EffectType.MAGIC);
            types.add(EffectType.WATER_BREATHING); // Yeah this works, we don't need a water breathing potion
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            provokedMobs.clear();
        }

        public void addProvokedMob(LivingEntity entity) {
            provokedMobs.put(entity, System.currentTimeMillis());
        }

        public boolean hasProvokedMob(LivingEntity entity) {
            return provokedMobs.containsKey(entity);
        }
    }
}
