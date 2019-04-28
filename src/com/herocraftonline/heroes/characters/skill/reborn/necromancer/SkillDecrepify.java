package com.herocraftonline.heroes.characters.skill.reborn.necromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.StaminaRegenDecreaseEffect;
import com.herocraftonline.heroes.characters.effects.common.StaminaRegenPercentDecreaseEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillDecrepify extends TargettedSkill {
    private String applyText;
    private String expireText;

    public SkillDecrepify(Heroes plugin) {
        super(plugin, "Decrepify");
        setDescription("Thicken the blood of your target, causing them to be unable to use stamina for $1 second(s).");
        setUsage("/skill " + getName().toLowerCase());
        setArgumentRange(0, 0);
        setIdentifiers("skill " + getName().toLowerCase());
        setTypes(SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.ABILITY_PROPERTY_DISEASE, SkillType.STAMINA_FREEZING, SkillType.DEBUFFING);
    }

    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 2000, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_INTELLECT, 75, false);
        duration += hero.getAttributeValue(AttributeType.INTELLECT) * durationIncrease;

        return getDescription()
                .replace("$1", Util.decFormat.format(duration / 1000.0));
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 7);
        config.set(SkillSetting.DURATION.node(), 4000);
        config.set(SkillSetting.DURATION_INCREASE_PER_INTELLECT.node(), 0);
        config.set("stamina-degen-percent", 0.5);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target%'s has been decrepified!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target%'s is no longer decrepified.");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                ChatComponents.GENERIC_SKILL + "%target%'s has been decrepified!")
                .replace("%target%", "$1");

        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                ChatComponents.GENERIC_SKILL + "%target%'s is no longer decrepified.")
                .replace("%target%", "$1");
    }

    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        // Get Debuff values
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 2000, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_INTELLECT, 75, false);
        duration += hero.getAttributeValue(AttributeType.INTELLECT) * durationIncrease;

        double degenPercent = SkillConfigManager.getUseSetting(hero, this, "stamina-degen-percent", 0.5, false);

        DecrepifyEffect tbEffect = new DecrepifyEffect(this, player, duration, degenPercent);
        CharacterTemplate targCT = plugin.getCharacterManager().getCharacter(target);
        targCT.addEffect(tbEffect);

        return SkillResult.NORMAL;
    }

    public class DecrepifyEffect extends StaminaRegenPercentDecreaseEffect {
        private int originalStamina;

        DecrepifyEffect(Skill skill, Player applier, long duration, double degenpercent) {
            super(skill, "Decrepified", applier, duration, degenpercent, applyText, expireText);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.HUNGER);

            addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, (int) duration / 50, 0));
        }

        @Override
        public void applyToMonster(Monster monster) {
            addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (getDuration() / 50), 1));

            super.applyToMonster(monster);
        }
    }
}