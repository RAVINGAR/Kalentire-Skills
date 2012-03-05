package com.herocraftonline.heroes.skill.skills;

import org.bukkit.configuration.ConfigurationSection;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.effects.common.SafeFallEffect;
import com.herocraftonline.heroes.hero.Hero;
import com.herocraftonline.heroes.skill.ActiveSkill;
import com.herocraftonline.heroes.skill.SkillConfigManager;
import com.herocraftonline.heroes.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;

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
