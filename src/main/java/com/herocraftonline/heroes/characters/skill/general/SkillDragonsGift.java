package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.CharacterDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

/**
 * Created By MysticMight 2021
 */

public class SkillDragonsGift extends PassiveSkill implements Listenable {
    private String damageText;
    private final Listener listener;

    public SkillDragonsGift(Heroes plugin) {
        super(plugin, "DragonsGift");
        setDescription("You fall gracefully and receive $1% fall damage.");
        setTypes(SkillType.ABILITY_PROPERTY_EARTH, SkillType.BUFFING);
        listener = new SkillHeroListener(this);
    }

    @Override
    public String getDescription(Hero hero) {
        double fallDamageMultiplier = SkillConfigManager.getUseSettingDouble(hero, this, "damage-multiplier", false);
        return getDescription().replace("$1", Util.decFormat.format(fallDamageMultiplier * 100.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.APPLY_TEXT.node(), "");
        config.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        config.set("damage-text", ChatComponents.GENERIC_SKILL + "Your fall damage was reduced by $1!");
        config.set("damage-multiplier", 0.9);
        return config;
    }

    @Override
    public void init() {
        super.init();
        damageText = "    " + SkillConfigManager.getRaw(this, "damage-text", ChatComponents.GENERIC_SKILL + "Your fall damage was reduced by $1!");
    }

    @Override
    public Listener getListener() {
        return listener;
    }

    public class SkillHeroListener implements Listener {
        private final PassiveSkill skill;

        public SkillHeroListener(PassiveSkill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEnvironmentDamage(CharacterDamageEvent event) {
            if (!(event.getEntity() instanceof Player) || event.getDamage() == 0)
                return;

            if (event.getCause() != DamageCause.FALL)
                return; // Only handle Fall damage

            Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (hero.hasEffectType(EffectType.SAFEFALL))
                return; // Skip, they are safe from falling already

            if (!skill.hasPassive(hero))
                return; // Handle only for those with this passive

            double fallDamageMultiplier = SkillConfigManager.getUseSettingDouble(hero, skill, "damage-multiplier", false);

            double reducedDamage = event.getDamage() * (1 - fallDamageMultiplier);
            if (!hero.isSuppressing(skill)) {
                String damageText = SkillDragonsGift.this.damageText.replace("$1", Util.decFormat.format(reducedDamage));
                hero.getPlayer().sendMessage(damageText);
            }

            event.setDamage(fallDamageMultiplier * event.getDamage());
        }
    }
}
