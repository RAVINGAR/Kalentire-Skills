package com.herocraftonline.heroes.characters.skill.reborn.duelist;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.QuickenEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.configuration.ConfigurationSection;

public class SkillOne extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillOne(Heroes plugin) {
        super(plugin, "One");
        setDescription("You gain a burst of speed for $1 second(s).");
        setUsage("/skill one");
        setArgumentRange(0, 0);
        setIdentifiers("skill one");
        setTypes(SkillType.BUFFING, SkillType.MOVEMENT_INCREASING);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("speed-potion-amplifier", 2);
        node.set(SkillSetting.DURATION.node(), 15000);
        node.set(SkillSetting.APPLY_TEXT.node(), "%hero% gained a burst of speed!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%hero% returned to normal speed!");
        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%hero% gained a burst of speed!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%hero% returned to normal speed!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);

        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        int multiplier = SkillConfigManager.getUseSetting(hero, this, "speed-potion-amplifier", 2, false);
        if (multiplier > 20) {
            multiplier = 20;
        }
        hero.addEffect(new QuickenEffect(this, getName(), hero.getPlayer(), duration, multiplier, applyText, expireText));

        return SkillResult.NORMAL;
    }
}
