package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
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

public class SkillBloodDrinker extends ActiveSkill {
    private String applyText;
    private String expireText;

    public SkillBloodDrinker(final Heroes plugin) {
        super(plugin, "BloodDrinker");
        setDescription("Drink the blood of your enemies and restore your health! The BloodDrinker effect lasts for $1 seconds and causes you to gain health for $2% of your physical damage dealt. You cannot gain more than $3 health from this effect.");
        setUsage("/skill blooddrinker");
        setArgumentRange(0, 0);
        setIdentifiers("skill blooddrinker");
        setTypes(SkillType.BUFFING, SkillType.AGGRESSIVE, SkillType.HEALING, SkillType.ABILITY_PROPERTY_PHYSICAL);

        Bukkit.getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    @Override
    public String getDescription(final Hero hero) {

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 7500, false);
        final double damageHealingPercent = SkillConfigManager.getUseSetting(hero, this, "damage-healing-percent", 0.1, false);
        final int maximumHealing = SkillConfigManager.getUseSetting(hero, this, "maximum-healing", 200, false);

        final String formattedDuration = Util.decFormat.format(duration / 1000.0);
        final String formattedhealingPercent = Util.decFormat.format(damageHealingPercent * 100.0);

        return getDescription().replace("$1", formattedDuration).replace("$2", formattedhealingPercent).replace("$3", maximumHealing + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set("weapons", Util.axes);
        node.set("damage-healing-percent", 0.1);
        node.set("maximum-healing", 200);
        node.set(SkillSetting.DURATION.node(), 7500);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is drinking blood!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is no longer drinking blood!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is drinking blood!").replace("%hero%", "$1").replace("$hero$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is no longer drinking blood!").replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {

        final Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 7500, false);
        final double damageHealingPercent = SkillConfigManager.getUseSetting(hero, this, "damage-healing-percent", 0.1, false);
        final int maximumHealing = SkillConfigManager.getUseSetting(hero, this, "maximum-healing", 200, false);

        final BloodDrinkEffect effect = new BloodDrinkEffect(this, player, duration, damageHealingPercent, maximumHealing);

        hero.addEffect(effect);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.9F, 0.5F);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 0.5F, 2.0F);

        return SkillResult.NORMAL;
    }

    private void bloodDrinkHeal(final Hero hero, final double damage) {
        final BloodDrinkEffect bdEffect = (BloodDrinkEffect) hero.getEffect("BloodDrinking");

        if (bdEffect != null) {
            final int maxHealing = bdEffect.getMaximumHealing();
            final double currentTotalHeal = bdEffect.getTotalHealthHealed();

            if (currentTotalHeal < maxHealing) {
                double healing = damage * bdEffect.getDamageHealingPercent();
                if (healing + currentTotalHeal > maxHealing) {
                    healing = maxHealing - currentTotalHeal;
                }

                final HeroRegainHealthEvent healEvent = new HeroRegainHealthEvent(hero, healing, this);       // Bypass self heal nerf because this cannot be used on others.
                Bukkit.getPluginManager().callEvent(healEvent);
                if (!healEvent.isCancelled()) {
                    final double finalHealing = healEvent.getDelta();
                    hero.heal(finalHealing);

                    final Player player = hero.getPlayer();
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 0.3F, 0.6F);

                    bdEffect.setTotalHealthHealed(currentTotalHeal + finalHealing);

                    if (bdEffect.getTotalHealthHealed() >= maxHealing) {
                        hero.removeEffect(bdEffect);
                    }
                }
            }
        }
    }

    private class SkillHeroListener implements Listener {

        public SkillHeroListener() {
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onSkillDamage(final SkillDamageEvent event) {
            if (event.getDamage() == 0) {
                return;
            }

            // Handle outgoing
            if (event.getDamager() instanceof Hero) {
                final Hero hero = (Hero) event.getDamager();
                if (hero.hasEffect("BloodDrinking")) {
                    if (!damageCheck(hero.getPlayer(), (LivingEntity) event.getEntity())) {
                        return;
                    }

                    bloodDrinkHeal(hero, event.getDamage());
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamage(final EntityDamageEvent event) {
            if (event.getDamage() == 0 || !(event.getEntity() instanceof LivingEntity) || !(event instanceof EntityDamageByEntityEvent) || event.getCause() != DamageCause.ENTITY_ATTACK) {
                return;
            }

            final EntityDamageByEntityEvent edbe = (EntityDamageByEntityEvent) event;
            final Entity attacker = edbe.getDamager();

            // Handle outgoing
            if (attacker instanceof Player) {
                final Player player = (Player) attacker;
                final Hero hero = plugin.getCharacterManager().getHero(player);
                if (hero.hasEffect("BloodDrinking")) {
                    bloodDrinkHeal(hero, event.getDamage());
                }
            }
        }
    }

    public class BloodDrinkEffect extends ExpirableEffect {
        private double damageHealingPercent;
        private double totalHealthHealed;
        private int maximumHealing;

        public BloodDrinkEffect(final Skill skill, final Player applier, final long duration, final double damageHealingPercent, final int maximumHealing) {
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

        public void setMaximumHealing(final int maximumHealing) {
            this.maximumHealing = maximumHealing;
        }

        public double getDamageHealingPercent() {
            return damageHealingPercent;
        }

        public void setDamageHealingPercent(final double damageHealingPercent) {
            this.damageHealingPercent = damageHealingPercent;
        }

        public double getTotalHealthHealed() {
            return totalHealthHealed;
        }

        public void setTotalHealthHealed(final double totalHealthHealed) {
            this.totalHealthHealed = totalHealthHealed;
        }
    }
}