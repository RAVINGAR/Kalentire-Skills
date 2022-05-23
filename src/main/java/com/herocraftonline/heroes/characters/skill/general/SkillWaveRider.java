package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class SkillWaveRider extends ActiveSkill {

    String applyText;

    public SkillWaveRider(Heroes plugin) {
        super(plugin, "WaveRider");
        setUsage("/skill waverider");
        setIdentifiers("skill waverider");
        setArgumentRange(0, 0);
        setDescription("Create a wave and ride it.");
        setTypes(SkillType.DAMAGING, SkillType.MOVEMENT_INCREASING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.AGGRESSIVE);
    }

    public String getDescription(Hero hero) {
        return getDescription();
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection cs = super.getDefaultConfig();
        return cs;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL
                + "%hero% used WaveRider!")
                .replace("%hero%", "$2");
    }

    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();



        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

}
