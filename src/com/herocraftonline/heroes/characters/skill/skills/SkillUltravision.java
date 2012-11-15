package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.configuration.ConfigurationSection;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.WaterBreatheEffect;;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;

public class SkillUltravision extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillUltravision(Heroes plugin) {
        super(plugin, "Ultravision");
        setDescription("You are able to see at night for $1 seconds.");
        setUsage("/skill gills");
        setArgumentRange(0, 0);
        setIdentifiers("skill ultravision");
        setTypes(SkillType.SILENCABLE, SkillType.BUFF);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 30000);
        node.set(Setting.APPLY_TEXT.node(), "%hero% gains Ultravision!");
        node.set(Setting.EXPIRE_TEXT.node(), "%hero% lost Ultravision!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT.node(), "%hero% gains Ultravision!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "%hero% lost Ultravision!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 30000, false);
        hero.addEffect(new WaterBreatheEffect(this, duration, applyText, expireText));
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 1, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }
}
