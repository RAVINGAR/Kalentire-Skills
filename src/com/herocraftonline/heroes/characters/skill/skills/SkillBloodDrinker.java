package com.herocraftonline.heroes.characters.skill.skills;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillBloodDrinker extends ActiveSkill {
    private String applyText;
    private String expireText;

    public SkillBloodDrinker(Heroes plugin) {
        super(plugin, "BloodDrinker");
        setDescription("Drink the blood of your enemies and restore your health! The BloodDrinker effect lasts for $1 seconds and causes you to gain health for $2% of your physical damage dealt. You cannot gain more than $3 health from this effect.");
        setUsage("/skill blooddrinker");
        setArgumentRange(0, 0);
        setIdentifiers("skill blooddrinker");
        setTypes(SkillType.BUFFING, SkillType.AGGRESSIVE, SkillType.HEALING, SkillType.ABILITY_PROPERTY_PHYSICAL);

        Bukkit.getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(7500), false);
        double damageHealingPercent = SkillConfigManager.getUseSetting(hero, this, "damage-healing-percent", Double.valueOf(0.1), false);
        int maximumHealing = SkillConfigManager.getUseSetting(hero, this, "maximum-healing", Integer.valueOf(200), false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedhealingPercent = Util.decFormat.format(damageHealingPercent * 100.0);

        return getDescription().replace("$1", formattedDuration).replace("$2", formattedhealingPercent).replace("$3", maximumHealing + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("weapons", Util.axes);
        node.set("damage-healing-percent", Double.valueOf(0.1));
        node.set("maximum-healing", Integer.valueOf(200));
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(7500));
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%hero% is drinking blood!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%hero% is no longer drinking blood!");

        return node;
    }

    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%hero% is drinking blood!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%hero% is no longer drinking blood!").replace("%hero%", "$1");
    }

    public SkillResult use(Hero hero, String[] args) {

        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(7500), false);
        double damageHealingPercent = SkillConfigManager.getUseSetting(hero, this, "damage-healing-percent", Double.valueOf(0.1), false);
        int maximumHealing = SkillConfigManager.getUseSetting(hero, this, "maximum-healing", Integer.valueOf(200), false);

        BloodDrinkEffect effect = new BloodDrinkEffect(this, player, duration, damageHealingPercent, maximumHealing);

        hero.addEffect(effect);

        player.getWorld().playSound(player.getLocation(), Sound.ENDERDRAGON_GROWL, 0.5F, 0.1F);

        return SkillResult.NORMAL;
    }

    private class SkillHeroListener implements Listener {

        public SkillHeroListener() {}

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {
            if (event.getDamage() == 0)
                return;

            // Handle outgoing
            if (event.getDamager() instanceof Hero) {
                Hero hero = (Hero) event.getDamager();

                if (hero.hasEffect("BloodDrinking")) {
                    bloodDrinkHeal(hero, event.getDamage());
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (event.getDamage() == 0)
                return;

            // Handle outgoing
            if (event.getDamager() instanceof Hero) {
                Hero hero = (Hero) event.getDamager();

                if (hero.hasEffect("BloodDrinking")) {
                    bloodDrinkHeal(hero, event.getDamage());
                }
            }
        }
    }

    private void bloodDrinkHeal(Hero hero, double damage) {
        BloodDrinkEffect bdEffect = (BloodDrinkEffect) hero.getEffect("BloodDrinking");

        if (bdEffect != null) {
            int maxHealing = bdEffect.getMaximumHealing();
            double currentTotalHeal = bdEffect.getTotalHealthHealed();

            Heroes.log(Level.INFO, "BloodDrinker Debug: MaxHealing: " + maxHealing + ", Current Total Heal: " + currentTotalHeal);

            if (currentTotalHeal < maxHealing) {

                double healing = damage * bdEffect.getDamageHealingPercent();
                Heroes.log(Level.INFO, "BloodDrinker Debug: HealthToHeal: " + healing);
                if (healing + currentTotalHeal > maxHealing) {
                    healing = maxHealing - currentTotalHeal;
                    Heroes.log(Level.INFO, "BloodDrinker Debug: Hit Cap. New HealthToHeal: " + healing);
                }

                HeroRegainHealthEvent healEvent = new HeroRegainHealthEvent(hero, healing, this, hero);
                Bukkit.getPluginManager().callEvent(healEvent);
                if (!healEvent.isCancelled()) {
                    hero.heal(healEvent.getAmount());
                }

                bdEffect.setTotalHealthHealed(currentTotalHeal + healing);

                if (bdEffect.getTotalHealthHealed() >= maxHealing)
                    hero.removeEffect(bdEffect);
            }
        }
    }

    public class BloodDrinkEffect extends ExpirableEffect {
        private double damageHealingPercent;
        private double totalHealthHealed;
        private int maximumHealing;

        public BloodDrinkEffect(Skill skill, Player applier, long duration, double damageHealingPercent, int maximumHealing) {
            super(skill, "BloodDrinking", applier, duration, applyText, expireText);

            types.add(EffectType.PHYSICAL);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.HEALING);

            this.damageHealingPercent = damageHealingPercent;
            this.maximumHealing = maximumHealing;
        }

        public int getMaximumHealing() {
            return maximumHealing;
        }

        public void setMaximumHealing(int maximumHealing) {
            this.maximumHealing = maximumHealing;
        }

        public double getDamageHealingPercent() {
            return damageHealingPercent;
        }

        public void setDamageHealingPercent(double damageHealingPercent) {
            this.damageHealingPercent = damageHealingPercent;
        }

        public double getTotalHealthHealed() {
            return totalHealthHealed;
        }

        public void setTotalHealthHealed(double totalHealthHealed) {
            this.totalHealthHealed = totalHealthHealed;
        }
    }
}