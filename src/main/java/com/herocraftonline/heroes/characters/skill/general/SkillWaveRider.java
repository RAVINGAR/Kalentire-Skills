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

    public SkillWaveRider(final Heroes plugin) {
        super(plugin, "WaveRider");
        setUsage("/skill waverider");
        setIdentifiers("skill waverider");
        setArgumentRange(0, 0);
        setDescription("Create a wave and ride it.");
        setTypes(SkillType.DAMAGING, SkillType.MOVEMENT_INCREASING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(final Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        return super.getDefaultConfig();
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL
                        + "%hero% used WaveRider!")
                .replace("%hero%", "$2").replace("$hero$", "$2");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();


        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

}
