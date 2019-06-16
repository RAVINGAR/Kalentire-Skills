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

public class SkillFocusOfMind extends ActiveSkill {
    public static String stanceEffectName = "FocusOfMind";

    private static String baseStanceMessage = "    " + ChatComponents.GENERIC_SKILL + ChatColor.RED + "Current Focus: " + ChatColor.WHITE + "<";
    private static String endStanceMessage = ChatColor.WHITE + ">";
    private static String unfocusedStanceText = "Unfocused";
    private static String sanguineStanceText = "Sanguine";
    private static String vigorousStanceText = "Vigorous";
    private static String delimiter = ChatColor.WHITE + " | ";

    public SkillFocusOfMind(Heroes plugin) {
        super(plugin, "FocusOfMind");
        setDescription("Shift the focus of mind, altering the potency of your other abilities.\n" +
                ChatColor.GRAY + "Unfocused: " + ChatColor.WHITE + "Your damage and healing are unchanged.\n" +
                ChatColor.DARK_RED + "Sanguine: " + ChatColor.WHITE + "Your damage is increased by $1% and your healing is reduced by $2%.\n" +
                ChatColor.RED + "Vigorous: " + ChatColor.WHITE + "your healing is increased by $3% and your damage is reduced by $4%.");
        setUsage("/skill focusofmind or /skill focusofmind <stancename>");
        setIdentifiers("skill focusofmind");
        setArgumentRange(0, 1);
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.FORM_ALTERING, SkillType.SILENCEABLE, SkillType.BUFFING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double sanguineDamageIncrease = SkillConfigManager.getUseSetting(hero, this, "sanguine-damage-percent-increase", 0.25, false);
        double sanguineHealingDecrease = SkillConfigManager.getUseSetting(hero, this, "sanguine-healing-percent-decrease", 0.5, false);

        double vigorousHealingIncrease = SkillConfigManager.getUseSetting(hero, this, "vigorous-healing-percent-increase", 0.35, false);
        double vigorousDamageDecrease = SkillConfigManager.getUseSetting(hero, this, "vigorous-damage-percent-decrease", 0.5, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(sanguineDamageIncrease))
                .replace("$2", Util.decFormat.format(sanguineHealingDecrease))
                .replace("$3", Util.decFormat.format(vigorousHealingIncrease))
                .replace("$4", Util.decFormat.format(vigorousDamageDecrease));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.COOLDOWN.node(), 500);
        config.set(SkillSetting.USE_TEXT.node(), "");
        config.set("sanguine-damage-percent-increase", 0.25);
        config.set("sanguine-healing-percent-decrease", 0.5);
        config.set("vigorous-healing-percent-increase", 0.35);
        config.set("vigorous-damage-percent-decrease", 0.5);
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
            hero.addEffect(new FocusEffect(this, player));
        }

        FocusEffect effect = (FocusEffect) hero.getEffect(stanceEffectName);
        if (stanceOverride != null) {
            effect.setCurrentStance(stanceOverride);
        } else {
            effect.nextStance();
        }

        return SkillResult.NORMAL;
    }

    public enum StanceType {
        UNFOCUSED,
        SANGUINE,
        VIGOROUS
    }

    public static class FocusEffect extends Effect {
        private StanceType currentStance = StanceType.UNFOCUSED;
        private double sanguineDamageIncrease;
        private double sanguineHealingDecrease;
        private double vigorousHealingIncrease;
        private double vigorousDamageDecrease;

        FocusEffect(Skill skill, Player applier) {
            super(skill, stanceEffectName, applier, null, null);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.ENDER);
            types.add(EffectType.FORM);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();

            this.sanguineDamageIncrease = SkillConfigManager.getUseSetting(hero, skill, "sanguine-damage-percent-increase", 0.25, false);
            this.sanguineHealingDecrease = SkillConfigManager.getUseSetting(hero, skill, "sanguine-healing-percent-decrease", 0.5, false);

            this.vigorousHealingIncrease = SkillConfigManager.getUseSetting(hero, skill, "vigorous-healing-percent-increase", 0.35, false);
            this.vigorousDamageDecrease = SkillConfigManager.getUseSetting(hero, skill, "vigorous-damage-percent-decrease", 0.5, false);
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
                setCurrentStance(StanceType.SANGUINE);
            } else if (this.currentStance == StanceType.SANGUINE) {
                setCurrentStance(StanceType.VIGOROUS);
            } else if (this.currentStance == StanceType.VIGOROUS) {
                setCurrentStance(StanceType.UNFOCUSED);
            }
        }

        public String getCurrentStanceMessage() {
            @SuppressWarnings("StringBufferReplaceableByString")
            StringBuilder builder = new StringBuilder(baseStanceMessage);
            builder.append(this.currentStance == StanceType.UNFOCUSED ? ChatColor.DARK_RED : ChatColor.GRAY);
            builder.append(unfocusedStanceText);
            builder.append(delimiter);
            builder.append(this.currentStance == StanceType.SANGUINE ? ChatColor.DARK_RED : ChatColor.GRAY);
            builder.append(sanguineStanceText);
            builder.append(delimiter);
            builder.append(this.currentStance == StanceType.VIGOROUS ? ChatColor.DARK_RED : ChatColor.GRAY);
            builder.append(vigorousStanceText);
            builder.append(endStanceMessage);
            return builder.toString();
        }

        public double getDamageMultiplier() {
            if (this.currentStance == StanceType.SANGUINE) {
                return 1.0 + this.sanguineDamageIncrease;
            } else if (this.currentStance == StanceType.VIGOROUS) {
                return 1.0 - this.vigorousDamageDecrease;
            }
            return 1.0;
        }

        public double getHealingMultiplier() {
            if (this.currentStance == StanceType.SANGUINE) {
                return 1.0 - this.sanguineHealingDecrease;
            } else if (this.currentStance == StanceType.VIGOROUS) {
                return 1.0 + this.vigorousHealingIncrease;
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
            if (!event.getDamager().hasEffect(stanceEffectName))
                return;

            FocusEffect effect = (FocusEffect) event.getDamager().getEffect(stanceEffectName);
            if (effect.getCurrentStance() == StanceType.UNFOCUSED)
                return;

            event.setDamage(event.getDamage() * effect.getDamageMultiplier());
        }

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onPlayerDeath(HeroRegainHealthEvent event) {
            if (event.getHealer() == null || !event.getHealer().hasEffect(stanceEffectName))
                return;

            FocusEffect effect = (FocusEffect) event.getHealer().getEffect(stanceEffectName);
            if (effect.getCurrentStance() == StanceType.UNFOCUSED)
                return;

            event.setDelta(event.getDelta() * effect.getHealingMultiplier());
        }
    }
}
