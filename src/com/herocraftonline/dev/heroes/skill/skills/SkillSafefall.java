package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.configuration.ConfigurationSection;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.effects.common.SafeFallEffect;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Setting;

public class SkillSafefall extends ActiveSkill {

    public SkillSafefall(Heroes plugin) {
        super(plugin, "Safefall");
        setDescription("You float safely to the ground for $1 seconds.");
        setUsage("/skill safefall");
        setArgumentRange(0, 0);
        setIdentifiers("skill safefall");
        setTypes(SkillType.MOVEMENT, SkillType.BUFF, SkillType.SILENCABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 20000);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 20000, false);
        hero.addEffect(new SafeFallEffect(this, duration));
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 20000, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }
}
