package com.herocraftonline.heroes.characters.skill.pack2;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;

public class SkillBerserkerRage extends PassiveSkill {

    public SkillBerserkerRage(Heroes plugin) {
        super(plugin, "BerserkerRage");
        setDescription("Your attacks are filled with unyielding rage. Your physical damage is increased by $1% for every 1% of health missing. The damage increase has a maximum threshold of $2%. Your current damage increase is $3%.");
        setArgumentRange(0, 0);
        setEffectTypes(EffectType.BENEFICIAL, EffectType.PHYSICAL);
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.BUFFING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    public String getDescription(Hero hero) {
        Player player = hero.getPlayer();

        double damageIncreasePerHPPercent = SkillConfigManager.getUseSetting(hero, this, "damage-percent-increase-per-hp-percent", 0.0075, false);
        double damageIncreaseThreshhold = SkillConfigManager.getUseSetting(hero, this, "damage-percent-increase-threshhold", 0.40, false);

        int hpPercent = 100 - ((int) ((player.getHealth() / player.getMaxHealth()) * 100));
        double currentDamageModifier = hpPercent * damageIncreasePerHPPercent;

        if (currentDamageModifier > damageIncreaseThreshhold)
            currentDamageModifier = damageIncreaseThreshhold;

        String formattedDamageIncreasePerHPPercent = Util.largeDecFormat.format(damageIncreasePerHPPercent * 100);
        String formattedDamageIncreaseThreshhold = Util.decFormat.format(damageIncreaseThreshhold * 100);
        String formattedCurrentDamageModifier = Util.decFormat.format(currentDamageModifier * 100);

        return getDescription().replace("$1", formattedDamageIncreasePerHPPercent).replace("$2", formattedDamageIncreaseThreshhold).replace("$3", formattedCurrentDamageModifier);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.APPLY_TEXT.node(), "");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "");
        node.set("damage-percent-increase-per-hp-percent", 0.0075);
        node.set("damage-percent-increase-threshhold", 0.40);

        return node;
    }

    public class SkillHeroListener implements Listener {

        public SkillHeroListener() {}

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {
            if (event.getDamage() == 0)
                return;

            // Handle outgoing
            if (event.getDamager() instanceof Hero) {
                Hero hero = (Hero) event.getDamager();

                if (hero.hasEffect(getName())) {
                    Player player = hero.getPlayer();

                    double originalDamage = event.getDamage();
                    event.setDamage(getBloodRageDamage(originalDamage, hero, player));
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

                if (hero.hasEffect(getName())) {
                    Player player = hero.getPlayer();

                    double originalDamage = event.getDamage();
                    event.setDamage(getBloodRageDamage(originalDamage, hero, player));
                }
            }
        }
    }

    private double getBloodRageDamage(double originalDamage, Hero hero, Player player) {
        int hpPercent = 100 - ((int) ((player.getHealth() / player.getMaxHealth()) * 100));
        double damageIncreasePerHPPercent = SkillConfigManager.getUseSetting(hero, this, "damage-percent-increase-per-hp-percent", 0.0075, false);
        double damageModifier = 1 + (hpPercent * damageIncreasePerHPPercent);

        // Heroes.log(Level.INFO, "BloodRage Attack: Damage Modifier: " + damageModifier);

        double damageIncreaseThreshhold = 1 + SkillConfigManager.getUseSetting(hero, this, "damage-percent-increase-threshhold", 0.40, false);
        if (damageModifier > damageIncreaseThreshhold) {
            damageModifier = damageIncreaseThreshhold;
            // Heroes.log(Level.INFO, "BloodRage Attack: Hit Threshhold. New Modifier: " + damageModifier);
        }
///     double newDamage = damageModifier * originalDamage;
        // Heroes.log(Level.INFO, "BloodRage Attack: Original Damage: " + originalDamage + ", New Damage: " + newDamage);

        return damageModifier * originalDamage;
    }
}
