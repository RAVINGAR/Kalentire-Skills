package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillBloodWell extends ActiveSkill {
    private String applyText;
    private String expireText;

    public SkillBloodWell(Heroes plugin) {
        super(plugin, "BloodWell");
        setDescription("Drink the blood of your enemies and restore your health! The BloodWell effect lasts for $1 seconds" +
                " and causes you to gain health for $2% of your physical damage dealt. You cannot gain more than $3 health from this effect.");
        setUsage("/skill bloodwell");
        setArgumentRange(0, 0);
        setIdentifiers("skill bloodwell");
        setTypes(SkillType.BUFFING, SkillType.AGGRESSIVE, SkillType.HEALING, SkillType.ABILITY_PROPERTY_PHYSICAL);

        Bukkit.getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 7500, false);
        double damageHealingPercent = SkillConfigManager.getUseSetting(hero, this, "damage-healing-percent", 0.1, false);
        int maximumHealing = SkillConfigManager.getUseSetting(hero, this, "maximum-healing", 200, false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedhealingPercent = Util.decFormat.format(damageHealingPercent * 100.0);

        return getDescription().replace("$1", formattedDuration).replace("$2", formattedhealingPercent).replace("$3", maximumHealing + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("weapons", Util.axes);
        node.set("damage-healing-percent", 0.1);
        node.set("maximum-healing", 200);
        node.set(SkillSetting.DURATION.node(), 7500);
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

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 7500, false);
        double damageHealingPercent = SkillConfigManager.getUseSetting(hero, this, "damage-healing-percent", 0.1, false);
        int maximumHealing = SkillConfigManager.getUseSetting(hero, this, "maximum-healing", 200, false);

        BloodDrinkEffect effect = new BloodDrinkEffect(this, player, duration, damageHealingPercent, maximumHealing);

        hero.addEffect(effect);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.9F, 0.5F);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 0.5F, 2.0F);

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
                if (hero.hasEffect("BloodWelling")) {
                    if (!damageCheck(hero.getPlayer(), (LivingEntity) event.getEntity()))
                        return;

                    bloodDrinkHeal(hero, event.getDamage());
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.getDamage() == 0 || !(event.getEntity() instanceof LivingEntity) || !(event instanceof EntityDamageByEntityEvent) || event.getCause() != DamageCause.ENTITY_ATTACK) {
                return;
            }

            EntityDamageByEntityEvent edbe = (EntityDamageByEntityEvent) event;
            Entity attacker = edbe.getDamager();
            
            // Handle outgoing
            if (attacker instanceof Player) {
                Player player = (Player) attacker;
                Hero hero = plugin.getCharacterManager().getHero(player);
                if (hero.hasEffect("BloodWelling"))
                    bloodDrinkHeal(hero, event.getDamage());
            }
        }
    }

    private void bloodDrinkHeal(Hero hero, double damage) {
        BloodDrinkEffect bdEffect = (BloodDrinkEffect) hero.getEffect("BloodWelling");

        if (bdEffect != null) {
            int maxHealing = bdEffect.getMaximumHealing();
            double currentTotalHeal = bdEffect.getTotalHealthHealed();

            if (currentTotalHeal < maxHealing) {
                double healing = damage * bdEffect.getDamageHealingPercent();
                if (healing + currentTotalHeal > maxHealing) {
                    healing = maxHealing - currentTotalHeal;
                }

                HeroRegainHealthEvent healEvent = new HeroRegainHealthEvent(hero, healing, this);       // Bypass self heal nerf because this cannot be used on others.
                Bukkit.getPluginManager().callEvent(healEvent);
                if (!healEvent.isCancelled()) {
                    double finalHealing = healEvent.getAmount();
                    hero.heal(finalHealing);

                    Player player = hero.getPlayer();
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 0.3F, 0.6F);

                    bdEffect.setTotalHealthHealed(currentTotalHeal + finalHealing);

                    if (bdEffect.getTotalHealthHealed() >= maxHealing)
                        hero.removeEffect(bdEffect);
                }
            }
        }
    }

    public class BloodDrinkEffect extends ExpirableEffect {
        private double damageHealingPercent;
        private double totalHealthHealed;
        private int maximumHealing;

        public BloodDrinkEffect(Skill skill, Player applier, long duration, double damageHealingPercent, int maximumHealing) {
            super(skill, "BloodWelling", applier, duration, applyText, expireText);

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