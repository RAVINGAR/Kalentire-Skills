package com.herocraftonline.heroes.characters.skill.remastered;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Collections;

/**
 * Created By MysticMight 2021
 */

public class SkillDread extends PassiveSkill implements Listenable {
    private static final String dreadDebuffEffectName = "DreadDebuff";
    private String debuffApplyText;
    private String debuffExpireText;
    private final Listener listener;

    public SkillDread(Heroes plugin) {
        super(plugin, "Dread");
        setDescription("$1% chance on attack to apply a debuff on target which reduces their damage output by $2% for " +
                "$3 second(s)");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DEBUFFING);
        listener = new SkillHeroListener(this);
    }

    @Override
    public String getDescription(Hero hero) {
        double chance = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.CHANCE, false);
        double damageMultiplier = SkillConfigManager.getUseSettingDouble(hero, this, "damage-multiplier", true);
        int duration = SkillConfigManager.getUseSettingInt(hero, this, SkillSetting.DURATION, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(chance * 100))
                .replace("$2", Util.decFormat.format(damageMultiplier * 100))
                .replace("$3", Util.decFormat.format(duration / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.APPLY_TEXT.node(), "");
        config.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        config.set("debuff-apply-text", "todo dread debuff applied to %target%");
        config.set("debuff-expire-text", "todo dread debuff expired to %target%");
        config.set(SkillSetting.CHANCE.node(), 0.01);
        config.set(SkillSetting.CHANCE_PER_LEVEL.node(), 0.0);
        config.set("damage-multiplier", 0.9);
        config.set(SkillSetting.DURATION.node(), 10000);
        return config;
    }

    @Override
    public void init() {
        super.init();

        this.debuffApplyText = SkillConfigManager.getRaw(this, "debuff-apply-text", "")
                .replace("%target%", "$1").replace("%hero%", "$2");
        this.debuffExpireText = SkillConfigManager.getRaw(this, "debuff-expire-text", "")
                .replace("%target%", "$1").replace("%hero%", "$2");
    }

    @Override
    public Listener getListener() {
        return listener;
    }

    public class SkillHeroListener implements Listener {
        private PassiveSkill skill;

        public SkillHeroListener(PassiveSkill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (event.getDamage() == 0 || !(event.getDamager() instanceof Hero) || !(event.getEntity() instanceof LivingEntity))
                return;

            final Hero attacker = (Hero) event.getDamager();
            if (!skill.hasPassive(attacker))
                return;

            double chance = SkillConfigManager.getScaledUseSettingDouble(attacker, skill, SkillSetting.CHANCE, false);
            if (Util.nextRand() < chance) {
                // apply debuff to target
                final LivingEntity defenderEntity = (LivingEntity)event.getEntity();
                final CharacterTemplate defenderC = plugin.getCharacterManager().getCharacter(defenderEntity);

                double damageMultiplier = SkillConfigManager.getUseSettingDouble(attacker, skill, "damage-multiplier", true);
                int duration = SkillConfigManager.getUseSettingInt(attacker, skill, SkillSetting.DURATION, false);
                defenderC.addEffect(new DreadDebuffEffect(skill, attacker.getPlayer(), duration, damageMultiplier));
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onReduceWeaponDamage(WeaponDamageEvent event) {
            if (event.getDamage() == 0 || !(event.getDamager() instanceof Hero))
                return;

            final Hero attacker = (Hero) event.getDamager();
            if (!attacker.hasEffect(dreadDebuffEffectName))
                return;

            final DreadDebuffEffect debuffEffect = (DreadDebuffEffect) attacker.getEffect(dreadDebuffEffectName);
            assert debuffEffect != null;

            event.setDamage(event.getDamage() * debuffEffect.getOutputDamageMultiplier());
        }
    }

    public class DreadDebuffEffect extends ExpirableEffect {
        private final double outputDamageMultiplier;

        public DreadDebuffEffect(Skill skill, Player applier, long duration, double outputDamageMultiplier) {
            super(skill, dreadDebuffEffectName, applier, duration, debuffApplyText, debuffExpireText);

            Collections.addAll(types, EffectType.HARMFUL, EffectType.PHYSICAL, EffectType.DARK);
            this.outputDamageMultiplier = outputDamageMultiplier;
        }

        public double getOutputDamageMultiplier() {
            return outputDamageMultiplier;
        }
    }
}
