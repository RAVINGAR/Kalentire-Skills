package com.herocraftonline.heroes.characters.skill.remastered.paladin;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.CharacterRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Created By MysticMight 2021
 */

public class SkillHolyInspiration extends PassiveSkill {

    public SkillHolyInspiration(Heroes plugin) {
        super(plugin, "HolyInspiration");
        setDescription("Heals on you are $1% stronger.");
        setTypes(SkillType.BUFFING, SkillType.ABILITY_PROPERTY_LIGHT);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double healMultiplier = SkillConfigManager.getUseSettingDouble(hero, this, "heal-multiplier", false);
        return getDescription().replace("$1", Util.decFormat.format((1 - healMultiplier) * 100));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.APPLY_TEXT.node(), "");
        config.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        config.set("heal-multiplier", 1.1);
        config.set("heal-self-multiplier", 1.1);
        return config;
    }

    public class SkillHeroListener implements Listener {
        private final PassiveSkill skill;

        public SkillHeroListener(PassiveSkill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onHealing(CharacterRegainHealthEvent event) {
            if (!(event.getHealer() instanceof Hero))
                return;

            final Hero hero = (Hero) event.getHealer();
            if (!skill.hasPassive(hero))
                return; // Handle only for those with this passive

            double healOtherMultiplier = SkillConfigManager.getUseSettingDouble(hero, skill, "heal-multiplier", false);
            double healSelfMultiplier = SkillConfigManager.getUseSettingDouble(hero, skill, "heal-self-multiplier", false);

            boolean healSelf = event.getCharacter().getEntity().equals(hero.getEntity());

            double healMultiplier = healSelf ? healSelfMultiplier : healOtherMultiplier;
            event.setDelta(event.getDelta() * healMultiplier);
        }
    }
}
