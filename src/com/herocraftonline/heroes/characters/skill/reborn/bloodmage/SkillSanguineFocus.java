package com.herocraftonline.heroes.characters.skill.reborn.bloodmage;

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

public class SkillSanguineFocus extends ActiveSkill {
    private static String baseStanceMessage = "    " + ChatComponents.GENERIC_SKILL + ChatColor.RED + "Sanguine Focus: <";
    private static String endStanceMessage = ChatColor.RED + ">";
    private static String unfocusedStanceText = "Unfocused";
    private static String sanguineStanceText = "Sanguine";
    private static String geleniaStanceText = "Gelenia";
    private static String delimiter = ChatColor.WHITE + " | ";

    private static String effectName = "SanguineFocused";

    public SkillSanguineFocus(Heroes plugin) {
        super(plugin, "SanguineFocus");
        setDescription("Every time you cast this skill, you alter your focus of your other abilities." +
                ChatColor.GRAY + "Unfocused: " + ChatColor.WHITE + "Your damage and healing are unchanged.\n" +
                ChatColor.DARK_RED + "Sanguine: " + ChatColor.WHITE + "Your damage is increased by $1% and your healing is reduced by $2%.\n" +
                ChatColor.RED + "Gelenia: " + ChatColor.WHITE + "your healing is increased by $3% and your damage is reduced by $4%.");
        setUsage("/skill sanguinefocus");
        setIdentifiers("skill sanguinefocus");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.FORM_ALTERING, SkillType.SILENCEABLE, SkillType.BUFFING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double sanguineDamageIncrease = SkillConfigManager.getUseSetting(hero, this, "sanguine-damage-percent-increase", 0.25, false);
        double sanguineHealingDecrease = SkillConfigManager.getUseSetting(hero, this, "sanguine-healing-percent-decrease", 0.5, false);

        double geleniaHealingIncrease = SkillConfigManager.getUseSetting(hero, this, "gelenia-healing-percent-increase", 0.35, false);
        double geleniaDamageDecrease = SkillConfigManager.getUseSetting(hero, this, "gelenia-damage-percent-decrease", 0.5, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(sanguineDamageIncrease))
                .replace("$2", Util.decFormat.format(sanguineHealingDecrease))
                .replace("$3", Util.decFormat.format(geleniaHealingIncrease))
                .replace("$4", Util.decFormat.format(geleniaDamageDecrease));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.COOLDOWN.node(), 500);
        config.set(SkillSetting.USE_TEXT.node(), "");
        config.set("sanguine-damage-percent-increase", 0.25);
        config.set("sanguine-healing-percent-decrease", 0.5);
        config.set("gelenia-healing-percent-increase", 0.35);
        config.set("gelenia-damage-percent-decrease", 0.5);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        if (hero.hasEffect(effectName)) {
            BloodStanceEffect effect = (BloodStanceEffect) hero.getEffect(effectName);
            effect.nextStance();
            return SkillResult.NORMAL;
        }

        hero.addEffect(new BloodStanceEffect(this, player));
        return SkillResult.NORMAL;
    }

    private enum BloodStance {
        UNFOCUSED,
        SANGUINE,
        GELENIA
    }

    private class BloodStanceEffect extends Effect {
        private BloodStance currentStance;
        private double sanguineDamageIncrease;
        private double sanguineHealingDecrease;
        private double geleniaHealingIncrease;
        private double geleniaDamageDecrease;


        BloodStanceEffect(Skill skill, Player applier) {
            super(skill, effectName, applier, null, null);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.ENDER);
            types.add(EffectType.FORM);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            Player player = hero.getPlayer();

            this.currentStance = BloodStance.SANGUINE;
            this.sanguineDamageIncrease = SkillConfigManager.getUseSetting(hero, skill, "sanguine-damage-percent-increase", 0.25, false);
            this.sanguineHealingDecrease = SkillConfigManager.getUseSetting(hero, skill, "sanguine-healing-percent-decrease", 0.5, false);

            this.geleniaHealingIncrease = SkillConfigManager.getUseSetting(hero, skill, "gelenia-healing-percent-increase", 0.35, false);
            this.geleniaDamageDecrease = SkillConfigManager.getUseSetting(hero, skill, "gelenia-damage-percent-decrease", 0.5, false);
        }

        public BloodStance getCurrentStance() {
            return this.currentStance;
        }

        public void nextStance() {
            if (this.currentStance == BloodStance.UNFOCUSED) {
                this.currentStance = BloodStance.SANGUINE;

            } else if (this.currentStance == BloodStance.SANGUINE) {
                this.currentStance = BloodStance.GELENIA;
            } else if (this.currentStance == BloodStance.GELENIA) {
                this.currentStance = BloodStance.UNFOCUSED;
            }
            getApplier().sendMessage(getCurrentStanceMessage());
        }

        public String getCurrentStanceMessage() {
            StringBuilder builder = new StringBuilder(baseStanceMessage);
            builder.append(this.currentStance == BloodStance.UNFOCUSED ? ChatColor.DARK_RED : ChatColor.GRAY);
            builder.append(unfocusedStanceText);
            builder.append(delimiter);
            builder.append(this.currentStance == BloodStance.SANGUINE ? ChatColor.DARK_RED : ChatColor.GRAY);
            builder.append(sanguineStanceText);
            builder.append(delimiter);
            builder.append(this.currentStance == BloodStance.GELENIA ? ChatColor.DARK_RED : ChatColor.GRAY);
            builder.append(geleniaStanceText);
            builder.append(endStanceMessage);
            return builder.toString();
        }

        public double getDamageMultiplier() {
            if (this.currentStance == BloodStance.SANGUINE) {
                return 1.0 + this.sanguineDamageIncrease;
            } else if (this.currentStance == BloodStance.GELENIA) {
                return 1.0 - this.geleniaDamageDecrease;
            }
            return 1.0;
        }

        public double getHealingMultiplier() {
            if (this.currentStance == BloodStance.SANGUINE) {
                return 1.0 - this.sanguineHealingDecrease;
            } else if (this.currentStance == BloodStance.GELENIA) {
                return 1.0 + this.geleniaHealingIncrease;
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
        public void onPlayerDeath(SkillDamageEvent event) {
            if (!event.getDamager().hasEffect(effectName))
                return;

            BloodStanceEffect effect = (BloodStanceEffect) event.getDamager().getEffect(effectName);
            if (effect.getCurrentStance() == BloodStance.UNFOCUSED)
                return;

            event.setDamage(event.getDamage() * effect.getDamageMultiplier());
        }

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onPlayerDeath(HeroRegainHealthEvent event) {
            if (event.getHealer() == null || !event.getHealer().hasEffect(effectName))
                return;

            BloodStanceEffect effect = (BloodStanceEffect) event.getHealer().getEffect(effectName);
            if (effect.getCurrentStance() == BloodStance.UNFOCUSED)
                return;

            event.setDelta(event.getDelta() * effect.getHealingMultiplier());
        }
    }
}
