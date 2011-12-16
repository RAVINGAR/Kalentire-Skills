package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.effects.common.SilenceEffect;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.skill.TargettedSkill;
import com.herocraftonline.dev.heroes.util.Setting;

public class SkillSilence extends TargettedSkill {

    public SkillSilence(Heroes plugin) {
        super(plugin, "Silence");
        setDescription("Silences your target, making them unable to use some skills");
        setUsage("/skill silence <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill silence");
        setTypes(SkillType.DEBUFF, SkillType.SILENCABLE, SkillType.HARMFUL);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 5000);
        node.set(Setting.EXPIRE_TEXT.node(), "%hero% is no longer silenced!");
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        if (!(target instanceof Player))
        	return SkillResult.INVALID_TARGET;

        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
        SilenceEffect sEffect = new SilenceEffect(this, duration);
        plugin.getHeroManager().getHero((Player) target).addEffect(sEffect);
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }
}
