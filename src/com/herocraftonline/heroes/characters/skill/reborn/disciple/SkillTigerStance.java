package com.herocraftonline.heroes.characters.skill.reborn.disciple;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class SkillTigerStance extends ActiveSkill {
    public static String stanceEffectName = "TigerStance";

    private static String baseStanceMessage = "    " + ChatComponents.GENERIC_SKILL + ChatColor.YELLOW + "Current Stance: " + ChatColor.WHITE + "<";
    private static String endStanceMessage = ChatColor.WHITE + ">";
    private static String unfocusedStanceText = "Unfocused";
    private static String tigerStanceText = "Tiger";
    private static String jinStanceText = "Jin";
    private static String delimiter = ChatColor.WHITE + " | ";

    public SkillTigerStance(Heroes plugin) {
        super(plugin, "TigerStance");
        setDescription("Shift the focus of mind, altering the potency of your other abilities.\n" +
                ChatColor.GRAY + "Unfocused: " + ChatColor.WHITE + "Your damage and healing are unchanged.\n" +
                ChatColor.GOLD + "Tiger: " + ChatColor.WHITE + "Your damage is increased by $1% and your healing is reduced by $2%.\n" +
                ChatColor.YELLOW + "Jin: " + ChatColor.WHITE + "your healing is increased by $3% and your damage is reduced by $4%.");
        setUsage("/skill tigerstance or /skill tigerstance <stancename>");
        setIdentifiers("skill tigerstance");
        setArgumentRange(0, 1);
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.FORM_ALTERING, SkillType.SILENCEABLE, SkillType.BUFFING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double tigerDamageIncrease = SkillConfigManager.getUseSetting(hero, this, "tiger-damage-percent-increase", 0.25, false);
        double tigerHealingDecrease = SkillConfigManager.getUseSetting(hero, this, "tiger-healing-percent-decrease", 0.5, false);

        double jinHealingIncrease = SkillConfigManager.getUseSetting(hero, this, "jin-healing-percent-increase", 0.35, false);
        double jinDamageDecrease = SkillConfigManager.getUseSetting(hero, this, "jin-damage-percent-decrease", 0.5, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(tigerDamageIncrease))
                .replace("$2", Util.decFormat.format(tigerHealingDecrease))
                .replace("$3", Util.decFormat.format(jinHealingIncrease))
                .replace("$4", Util.decFormat.format(jinDamageDecrease));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.COOLDOWN.node(), 500);
        config.set(SkillSetting.USE_TEXT.node(), "");
        config.set("tiger-damage-percent-increase", 0.25);
        config.set("tiger-healing-percent-decrease", 0.5);
        config.set("jin-healing-percent-increase", 0.35);
        config.set("jin-damage-percent-decrease", 0.5);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        // Allow the user to choose the stance they want to go to directly. (Doubt most people will, but let's support it.)
        StanceType stanceOverride = null;
        if (args != null && args.length == 1 && args[0] != null) {
            stanceOverride = StanceType.valueOf(args[0].trim().toUpperCase());
        }

        broadcastExecuteText(hero);

        if (!hero.hasEffect(stanceEffectName)) {
            hero.addEffect(new StanceEffect(this, player));
        }

        StanceEffect effect = (StanceEffect) hero.getEffect(stanceEffectName);
        if (stanceOverride != null) {
            effect.setCurrentStance(stanceOverride);
        } else {
            effect.nextStance();
        }

        return SkillResult.NORMAL;
    }

    public enum StanceType {
        UNFOCUSED,
        TIGER,
        JIN
    }

    public static class StanceEffect extends Effect {
        private StanceType currentStance = StanceType.UNFOCUSED;
        private double tigerDamageIncrease;
        private double tigerHealingDecrease;
        private double jinHealingIncrease;
        private double jinDamageDecrease;

        StanceEffect(Skill skill, Player applier) {
            super(skill, stanceEffectName, applier, null, null);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.ENDER);
            types.add(EffectType.FORM);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();

            this.tigerDamageIncrease = SkillConfigManager.getUseSetting(hero, skill, "tiger-damage-percent-increase", 0.25, false);
            this.tigerHealingDecrease = SkillConfigManager.getUseSetting(hero, skill, "tiger-healing-percent-decrease", 0.5, false);

            this.jinHealingIncrease = SkillConfigManager.getUseSetting(hero, skill, "jin-healing-percent-increase", 0.35, false);
            this.jinDamageDecrease = SkillConfigManager.getUseSetting(hero, skill, "jin-damage-percent-decrease", 0.5, false);
        }

        public void setCurrentStance(StanceType stance) {
            if (stance == null) // Just in case
                this.currentStance = StanceType.UNFOCUSED;
            else
                this.currentStance = stance;

            getApplier().sendMessage(getCurrentStanceMessage());
        }

        public StanceType getCurrentStance() {
            return this.currentStance;
        }

        public void nextStance() {
            if (this.currentStance == StanceType.UNFOCUSED) {
                setCurrentStance(StanceType.TIGER);
            } else if (this.currentStance == StanceType.TIGER) {
                setCurrentStance(StanceType.JIN);
            } else if (this.currentStance == StanceType.JIN) {
                setCurrentStance(StanceType.UNFOCUSED);
            }
        }

        public String getCurrentStanceMessage() {
            @SuppressWarnings("StringBufferReplaceableByString")
            StringBuilder builder = new StringBuilder(baseStanceMessage);
            builder.append(this.currentStance == StanceType.UNFOCUSED ? ChatColor.GOLD : ChatColor.GRAY);
            builder.append(unfocusedStanceText);
            builder.append(delimiter);
            builder.append(this.currentStance == StanceType.TIGER ? ChatColor.GOLD : ChatColor.GRAY);
            builder.append(tigerStanceText);
            builder.append(delimiter);
            builder.append(this.currentStance == StanceType.JIN ? ChatColor.GOLD : ChatColor.GRAY);
            builder.append(jinStanceText);
            builder.append(endStanceMessage);
            return builder.toString();
        }

        public double getDamageMultiplier() {
            if (this.currentStance == StanceType.TIGER) {
                return 1.0 + this.tigerDamageIncrease;
            } else if (this.currentStance == StanceType.JIN) {
                return 1.0 - this.jinDamageDecrease;
            }
            return 1.0;
        }

        public double getHealingMultiplier() {
            if (this.currentStance == StanceType.TIGER) {
                return 1.0 - this.tigerHealingDecrease;
            } else if (this.currentStance == StanceType.JIN) {
                return 1.0 + this.jinHealingIncrease;
            }
            return 1.0;
        }
    }

    public class SkillListener implements Listener {
        private final Skill skill;

        SkillListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {
            if (!event.getDamager().hasEffect(stanceEffectName))
                return;

            StanceEffect effect = (StanceEffect) event.getDamager().getEffect(stanceEffectName);
            if (effect.getCurrentStance() == StanceType.UNFOCUSED)
                return;

            event.setDamage(event.getDamage() * effect.getDamageMultiplier());
        }

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onHeal(HeroRegainHealthEvent event) {
            if (event.getHealer() == null || !event.getHealer().hasEffect(stanceEffectName))
                return;

            StanceEffect effect = (StanceEffect) event.getHealer().getEffect(stanceEffectName);
            if (effect.getCurrentStance() == StanceType.UNFOCUSED)
                return;

            event.setDelta(event.getDelta() * effect.getHealingMultiplier());
        }
    }
}
