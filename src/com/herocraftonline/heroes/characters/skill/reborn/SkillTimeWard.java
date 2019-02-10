package com.herocraftonline.heroes.characters.skill.reborn;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.WalkSpeedDecreaseEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.ArrayList;
import java.util.List;

public class SkillTimeWard extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillTimeWard(Heroes plugin) {
        super(plugin, "TimeWard");
        setDescription("Project your target from the burdens of time for $1 seconds.");
        setArgumentRange(0, 0);
        setUsage("/skill timeward");
        setIdentifiers("skill timeward");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.MULTI_GRESSIVE, SkillType.SILENCEABLE);

//        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        ///double damageThreshold = SkillConfigManager.getUseSetting(hero, this, "maximum-damage-threshold", 300.0, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$2", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
//        config.set("maximum-damage-threshold", 300.0);
        config.set(SkillSetting.DURATION.node(), 5000);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has protected %target% from the effects of time!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is once again susceptible to time.");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL +
                "$hero$ has protected %target% from the effects of time!")
                .replace("%hero%", "$1")
                .replace("%target%", "$2");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL +
                "%target% is once again susceptible to time.")
                .replace("%target%", "$2");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);
//        double damageThreshold = SkillConfigManager.getUseSetting(hero, this, "maximum-damage-threshold", 300.0, false);

        CharacterTemplate ctTarget = plugin.getCharacterManager().getCharacter(target);
        //addSpellTarget(target, hero);
        ctTarget.addEffect(new TimeWardEffect(this, player, duration));

        broadcastExecuteText(hero, target);

        return SkillResult.NORMAL;
    }

    public class TimeWardEffect extends ExpirableEffect {

//        private double storedDamage = 0;
//        private Hero appliedHero;
//        private LivingEntity lastDamager;

        TimeWardEffect(Skill skill, Player applier, long duration) {
            super(skill, "TimeWarded", applier, duration, applyText, expireText);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.MAGIC);
            types.add(EffectType.BENEFICIAL);

//            this.lastDamager = applier;
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
//            this.appliedHero = hero;

            List<ExpirableEffect> effectsToRemove = new ArrayList<ExpirableEffect>();
            for (Effect effect : hero.getEffects()) {
                if (!(effect instanceof ExpirableEffect))
                    continue;
                if (effect == this)
                    continue;
                if (!effect.getName().contains("Time") || effect.getName().equals("DeceleratingTime") || effect.getName().equals("AcceleratingTime")) // TODO: Add "And has EffecType(Temporal) later.
                    continue;

                effectsToRemove.add((ExpirableEffect) effect);
            }

            // Separate loop for concurrent list modification reasons.
            for(ExpirableEffect exEffect : effectsToRemove) {
                hero.removeEffect(exEffect);
            }
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

//            Player player = hero.getPlayer();
//            if (lastDamager != player) {
//                CharacterTemplate ctLastDamager = plugin.getCharacterManager().getCharacter(lastDamager);
//                if (ctLastDamager != null) {
//                    plugin.getDamageManager().addSpellTarget(player, ctLastDamager, skill);
//                    damageEntity(player, lastDamager, storedDamage, EntityDamageEvent.DamageCause.MAGIC);
//                }
//            } else {
//                player.setHealth(player.getHealth() - storedDamage);
//            }
        }

//        void storeDamage(double damage) {
//            storedDamage+= damage;
//            if (storedDamage >= damageThreshold)
//                appliedHero.removeEffect(this);
//        }
//
//        void setLastAttacker(LivingEntity damager) {
//            lastDamager = damager;
//        }
    }

//    public class SkillHeroListener implements Listener {
//
//        @EventHandler
//        public void onEntityDamage(EntityDamageEvent event) {
//            if (!(event.getEntity() instanceof Player))
//                return;
//
//            Hero defenderHero = plugin.getCharacterManager().getHero((Player) event.getEntity());
//            if (!defenderHero.hasEffect("TimeWarded"))
//                return;
//
//            Player defenderPlayer = defenderHero.getPlayer();
//            TimeWardEffect effect = ((TimeWardEffect) defenderHero.getEffect("TimeWarded"));
//            if (effect == null)
//                return;
//
//            effect.storeDamage(event.getDamage());
//            event.setDamage(0);
//
//            if (event instanceof EntityDamageByEntityEvent) {
//                EntityDamageByEntityEvent dmgByEntEvent = (EntityDamageByEntityEvent) event;
//                Entity damager = dmgByEntEvent.getDamager();
//                if (damager instanceof LivingEntity) {
//                    effect.setLastAttacker((LivingEntity) damager);
//                } else if (damager instanceof Projectile) {
//                    ProjectileSource shooter = ((Projectile) damager).getShooter();
//                    if (shooter instanceof LivingEntity)
//                        effect.setLastAttacker((LivingEntity) shooter);
//                }
//            }
//        }
//    }
}
