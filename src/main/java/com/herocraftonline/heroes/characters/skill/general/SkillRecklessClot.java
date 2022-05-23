package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Created By MysticMight 2021
 */

public class SkillRecklessClot extends PassiveSkill {

    public SkillRecklessClot(Heroes plugin) {
        super(plugin, "RecklessClot");
        setDescription("While you are under $1% health, deal $2% more damage with weapons and $3% more for skills.");
        setTypes(SkillType.BUFFING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double healthMultiplier = SkillConfigManager.getUseSettingDouble(hero, this, "health-percentage", false);
        double weaponMultiplier = SkillConfigManager.getUseSettingDouble(hero, this, "weapon-multiplier", false);
        double skillMultiplier = SkillConfigManager.getUseSettingDouble(hero, this, "skill-multiplier", false);
        return getDescription()
                .replace("$1", Util.decFormat.format(healthMultiplier * 100))
                .replace("$2", Util.decFormat.format((1 - weaponMultiplier) * 100))
                .replace("$3", Util.decFormat.format((1 - skillMultiplier) * 100));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.APPLY_TEXT.node(), "");
        config.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        config.set("health-percentage", 0.2);
        config.set("weapon-multiplier", 1.2);
        config.set("skill-multiplier", 1.2);
        return config;
    }

    public class SkillHeroListener implements Listener {
        private final PassiveSkill skill;

        public SkillHeroListener(PassiveSkill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {
            if (!(event.getDamager() instanceof Hero) || event.getDamage() == 0)
                return;

            final Hero hero = (Hero) event.getDamager();
            if (!skill.hasPassive(hero))
                return; // Handle only for those with this passive

            Player player = hero.getPlayer();

            double healthMultiplier = SkillConfigManager.getUseSettingDouble(hero, skill, "health-percentage", false);
            if ((player.getHealth() / player.getMaxHealth()) <= healthMultiplier) {
                double skillMultiplier = SkillConfigManager.getUseSettingDouble(hero, skill, "skill-multiplier", false);
                event.setDamage(event.getDamage() * skillMultiplier);
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onSkillDamage(WeaponDamageEvent event) {
            if (!(event.getDamager() instanceof Hero) || event.getDamage() == 0)
                return;

            final Hero hero = (Hero) event.getDamager();
            if (!skill.hasPassive(hero))
                return; // Handle only for those with this passive

            Player player = hero.getPlayer();

            double healthMultiplier = SkillConfigManager.getUseSettingDouble(hero, skill, "health-percentage", false);
            if ((player.getHealth() / player.getMaxHealth()) <= healthMultiplier) {
                double weaponMultiplier = SkillConfigManager.getUseSettingDouble(hero, skill, "weapon-multiplier", false);
                event.setDamage(event.getDamage() * weaponMultiplier);
            }
        }
    }
}
