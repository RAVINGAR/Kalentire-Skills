package com.herocraftonline.heroes.characters.skill.remastered.cleric;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.CharacterRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Created By MysticMight 2021
 */

public class SkillFaith extends PassiveSkill {

    public SkillFaith(Heroes plugin) {
        super(plugin, "Faith");
        setDescription("While under $1% mana, your heals do $2% more.");
        setTypes(SkillType.BUFFING, SkillType.ABILITY_PROPERTY_LIGHT);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double manaMultiplier = SkillConfigManager.getUseSettingDouble(hero, this, "mana-percentage", false);
        double healMultiplier = SkillConfigManager.getUseSettingDouble(hero, this, "heal-multiplier", false);
        return getDescription()
                .replace("$1", Util.decFormat.format(manaMultiplier * 100))
                .replace("$2", Util.decFormat.format((1 - healMultiplier) * 100));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.APPLY_TEXT.node(), "");
        config.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        config.set("mana-percentage", 0.1);
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

            double manaMultiplier = SkillConfigManager.getUseSettingDouble(hero, skill, "mana-percentage", false);
            if (((double)hero.getMana() / hero.getMaxMana()) <= manaMultiplier) {

                double healOtherMultiplier = SkillConfigManager.getUseSettingDouble(hero, skill, "heal-multiplier", false);
                double healSelfMultiplier = SkillConfigManager.getUseSettingDouble(hero, skill, "heal-self-multiplier", false);

                boolean healSelf = event.getCharacter().getEntity().equals(hero.getEntity());

                double healMultiplier = healSelf ? healSelfMultiplier : healOtherMultiplier;
                event.setDelta(event.getDelta() * healMultiplier);
            }
        }
    }
}
