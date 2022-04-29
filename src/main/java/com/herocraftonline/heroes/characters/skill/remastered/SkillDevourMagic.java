package com.herocraftonline.heroes.characters.skill.remastered;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
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

public class SkillDevourMagic extends PassiveSkill implements Listenable {

    private String devourText;
    private final Listener listener;

    public SkillDevourMagic(Heroes plugin) {
        super(plugin, "DevourMagic");
        setDescription("You passively devour harmful magic targeted at you, reducing any incoming spell damage by $1% and restoring mana based on the reduced damage at a $2% rate.");
        setTypes(SkillType.MANA_INCREASING, SkillType.SILENCEABLE, SkillType.BUFFING, SkillType.ABILITY_PROPERTY_DARK);
        // Set types for passive effect
        setEffectTypes(EffectType.BENEFICIAL, EffectType.DARK, EffectType.MAGIC);

        listener = new SkillHeroListener(this);
    }

    @Override
    public String getDescription(Hero hero) {
        double resistMultiplier = SkillConfigManager.getUseSettingDouble(hero, this, "resist-multiplier", false);
        double manaConversionMultiplier = SkillConfigManager.getUseSettingDouble(hero, this, "mana-per-damage-multiplier", false);

        return getDescription()
                .replace("$1", Util.decFormat.format(resistMultiplier * 100))
                .replace("$2", Util.decFormat.format(manaConversionMultiplier * 100));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();

        config.set("resist-multiplier", 0.2);
        config.set("mana-per-damage-multiplier", 0.8);
        config.set(SkillSetting.APPLY_TEXT.node(), "");
        config.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        config.set("devour-text", ChatComponents.GENERIC_SKILL + "You devoured %damage% damage from %attacker% as %mana% mana.");

        return config;
    }

    @Override
    public void init() {
        super.init();
        devourText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "You devoured %damage% damage from %attacker% as %mana% mana.");
    }

    @Override
    public void apply(Hero hero) {
        super.apply(hero);
    }

    @Override
    public Listener getListener() {
        return listener;
    }

    public class SkillHeroListener implements Listener {
        private final Skill skill;

        public SkillHeroListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {
            Skill eventSkill = event.getSkill();
            if (eventSkill.isType(SkillType.ABILITY_PROPERTY_PHYSICAL) || !eventSkill.isType(SkillType.DAMAGING))
                return;

            if (!(event.getEntity() instanceof Player))
                return;

            Player player = (Player) event.getEntity();
            Hero hero = plugin.getCharacterManager().getHero(player);
            if (hero.hasEffect(skill.getName())) {
                double oldDamage = event.getDamage();
                double newDamageMultiplier = 1.0 - SkillConfigManager.getUseSettingDouble(hero, skill, "resist-multiplier", false);
                double newDamage = oldDamage * newDamageMultiplier;

                // Give them mana
                double manaConversionRate = SkillConfigManager.getUseSettingDouble(hero, skill, "mana-per-damage-multiplier", false);
                int manaRegain = (int) ((oldDamage - newDamage) * manaConversionRate);
                hero.setMana(hero.getMana() + manaRegain);

                // Reduce damage
                event.setDamage(newDamage);

                if (devourText != null && devourText.length() > 0) {
                    player.sendMessage("    " + devourText
                            .replace("%damage%", Util.decFormat.format(oldDamage - newDamage))
                            .replace("%attacker%", event.getDamager().getName())
                            .replace("%mana%", manaRegain+""));
                }
            }
        }
    }
}
