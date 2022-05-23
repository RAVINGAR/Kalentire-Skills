package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.CharacterRegainHealthEvent;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Created By MysticMight 2021
 */

public class SkillNaturesBoon extends PassiveSkill {
    private String restoreText;

    public SkillNaturesBoon(Heroes plugin) {
        super(plugin, "NaturesBoon");
        setDescription("Healing a target has a $1% chance to restore $2 mana.");
        setTypes(SkillType.BUFFING, SkillType.ABILITY_PROPERTY_LIGHT, SkillType.MANA_INCREASING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int restoredMana = SkillConfigManager.getScaledUseSettingInt(hero, this, "restored-mana", false);
        double chance = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.CHANCE, false);
        return getDescription()
                .replace("$1", Util.decFormat.format(chance * 100))
                .replace("$2", restoredMana+"");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.APPLY_TEXT.node(), "");
        config.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        config.set(SkillSetting.CHANCE.node(), 0.2);
        config.set(SkillSetting.CHANCE_PER_LEVEL.node(), 0.0);
        config.set("restored-mana", 30);
        config.set("restored-mana-per-level", 1);
        config.set("restore-text", "Recovered %mana% mana from healing %target%.");
        return config;
    }

    @Override
    public void init() {
        super.init();
        restoreText = "    " + ChatComponents.GENERIC_SKILL + " "
                + SkillConfigManager.getRaw(this, "restore-text", "Recovered %mana% mana from healing %target%.");
    }

    public class SkillHeroListener implements Listener {
        private final PassiveSkill skill;

        public SkillHeroListener(PassiveSkill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onHealing(CharacterRegainHealthEvent event) {
            if (!(event.getHealer() instanceof Hero))
                return;

            final Hero hero = (Hero) event.getHealer();
            if (!skill.hasPassive(hero))
                return; // Handle only for those with this passive

            boolean healSelf = event.getCharacter().getEntity().equals(hero.getEntity());
            if (healSelf)
                return; // Skip for self heals

            double chance = SkillConfigManager.getScaledUseSettingDouble(hero, this.skill, SkillSetting.CHANCE, false);
            if (Util.nextRand() < chance) {
                int restoredMana = SkillConfigManager.getScaledUseSettingInt(hero, this.skill, "restored-mana", false);
                if (hero.tryRestoreMana(this.skill, restoredMana)) {
                    String newRestoreText = restoreText.replace("%mana%", restoredMana+"")
                            .replace("%target%", CustomNameManager.getCustomName(event.getCharacter().getEntity()));
                    hero.getPlayer().sendMessage(newRestoreText);
                }
            }
        }
    }
}
