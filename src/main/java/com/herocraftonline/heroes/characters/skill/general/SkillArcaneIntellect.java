package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.configuration.ConfigurationSection;

public class SkillArcaneIntellect extends PassiveSkill {
    public SkillArcaneIntellect(Heroes plugin) {
        super(plugin, "ArcaneIntellect");
        setDescription("Your arcane aura inspires those allies around you. Your allies gain a passive intellect bonus of $1 whilst within $2 blocks of you.");
        setTypes(SkillType.BUFFING);
        setEffectTypes(EffectType.BENEFICIAL, EffectType.MAGIC);

    }

    @Override
    public String getDescription(Hero hero) {
        int increase = SkillConfigManager.getUseSettingInt(hero, this, "intellect-increase", true);
        double add = SkillConfigManager.getUseSettingDouble(hero, this, "intellect-increase-per-level", true);
        increase += (int)(add * hero.getHeroLevel(this));

        return getDescription()
                .replace("$1", Util.decFormat.format(increase))
                .replace("$2", Util.decFormat.format(SkillConfigManager.getUseSettingInt(hero, this, SkillSetting.RADIUS.node(), true)));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();

        config.set("intellect-increase", 1.0);
        config.set("intellect-increase-per-level", 0.1);
        config.set(SkillSetting.APPLY_TEXT.node(), "");
        config.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        config.set(SkillSetting.RADIUS.node(), 8);

        return config;
    }
}
