package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;

public class SkillBloodRage extends PassiveSkill {

    public SkillBloodRage(Heroes plugin) {
        super(plugin, "BloodRage");
        setDescription("Passive: Your attacks are filled with unyielding rage. Your physical damage is increased by $1% for every 1% of health missing. The damage increase has a maximum threshhold of $2%.");
        setArgumentRange(0, 0);
        setEffectTypes(EffectType.BENEFICIAL, EffectType.PHYSICAL);
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.BUFFING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    public String getDescription(Hero hero) {
        double damageIncreasePerHPPercent = SkillConfigManager.getUseSetting(hero, this, "damage-percent-increase-per-hp-percent", Double.valueOf(0.0075), false);
        double damageIncreaseThreshhold = SkillConfigManager.getUseSetting(hero, this, "damage-percent-increase-threshhold", Double.valueOf(0.40), false);

        String formattedDamageIncreasePerHPPercent = Util.decFormat.format(damageIncreasePerHPPercent * 100);
        String formattedDamageIncreaseThreshhold = Util.decFormat.format(damageIncreaseThreshhold * 100);

        return getDescription().replace("$1", formattedDamageIncreasePerHPPercent).replace("$2", formattedDamageIncreaseThreshhold);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.APPLY_TEXT.node(), "");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "");
        node.set("damage-percent-increase-per-hp-percent", Double.valueOf(0.0075));
        node.set("damage-percent-increase-threshhold", Double.valueOf(0.40));

        return node;
    }

    public class SkillHeroListener implements Listener {

        private Skill skill;

        public SkillHeroListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {

            // Handle outgoing
            if (event.getDamager() instanceof Hero) {
                Hero hero = plugin.getCharacterManager().getHero((Player) event.getDamager());

                if (hero.hasEffect(getName())) {
                    Player player = hero.getPlayer();

                    int hpPercent = (int) ((player.getHealth() / player.getMaxHealth()) * 100);
                    double damageIncreasePerHPPercent = SkillConfigManager.getUseSetting(hero, skill, "damage-percent-increase-per-hp-percent", Double.valueOf(0.0075), false);
                    double damageModifier = hpPercent * damageIncreasePerHPPercent;

                    double damageIncreaseThreshhold = SkillConfigManager.getUseSetting(hero, skill, "damage-percent-increase-threshhold", Double.valueOf(0.40), false);
                    if (damageModifier > damageIncreaseThreshhold)
                        damageModifier = damageIncreaseThreshhold;

                    double newDamage = damageModifier * event.getDamage();
                    event.setDamage(newDamage);
                }
            }
        }

        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {

            // Handle outgoing
            if (event.getDamager() instanceof Hero) {
                Hero hero = plugin.getCharacterManager().getHero((Player) event.getDamager());

                if (hero.hasEffect(getName())) {
                    Player player = hero.getPlayer();

                    int hpPercent = (int) ((player.getHealth() / player.getMaxHealth()) * 100);
                    double damageIncreasePerHPPercent = SkillConfigManager.getUseSetting(hero, skill, "damage-percent-increase-per-hp-percent", Double.valueOf(0.057), false);
                    double damageModifier = hpPercent * damageIncreasePerHPPercent;

                    double damageIncreaseThreshhold = SkillConfigManager.getUseSetting(hero, skill, "damage-percent-increase-threshhold", Double.valueOf(0.40), false);
                    if (damageModifier > damageIncreaseThreshhold)
                        damageModifier = damageIncreaseThreshhold;

                    double newDamage = damageModifier * event.getDamage();
                    event.setDamage(newDamage);
                }
            }
        }
    }
}
