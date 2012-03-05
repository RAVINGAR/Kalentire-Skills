package com.herocraftonline.heroes.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.effects.common.BlindEffect;
import com.herocraftonline.heroes.hero.Hero;
import com.herocraftonline.heroes.skill.SkillConfigManager;
import com.herocraftonline.heroes.skill.SkillType;
import com.herocraftonline.heroes.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;

public class SkillBlind extends TargettedSkill {

    private String applyText;
    private String expireText;
    
    public SkillBlind(Heroes plugin) {
        super(plugin, "Blind");
        setDescription("You blind the target for $1 seconds.");
        setUsage("/skill blind <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill blind");
        setTypes(SkillType.SILENCABLE, SkillType.ILLUSION, SkillType.HARMFUL);
    }
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 3000);
        node.set(Setting.APPLY_TEXT.node(), "%target% has been blinded!");
        node.set(Setting.EXPIRE_TEXT.node(), "%target% can see again!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT.node(), "%target% has been blinded!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT.node(), "%target% can see again!").replace("%target%", "$1");
    }
    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (!(target instanceof Player)) {
            Messaging.send(player, "You must target a player!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 3000, false);
        hero.addEffect(new BlindEffect(this, duration, applyText, expireText));
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 3000, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }

}
