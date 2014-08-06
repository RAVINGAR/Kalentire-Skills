package com.herocraftonline.heroes.characters.skill.unusedskills;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SilenceEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;

public class SkillSilence extends TargettedSkill {

    public SkillSilence(Heroes plugin) {
        super(plugin, "Silence");
        setDescription("Silences your target, making them unable to use skills for $1 seconds.");
        setUsage("/skill silence");
        setArgumentRange(0, 0);
        setIdentifiers("skill silence");
        setTypes(SkillType.DEBUFFING, SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.INTERRUPTING);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%hero% is no longer silenced!");
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        if (!(target instanceof Player))
        	return SkillResult.INVALID_TARGET;

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        SilenceEffect sEffect = new SilenceEffect(this, hero.getPlayer(), duration);
        plugin.getCharacterManager().getHero((Player) target).addEffect(sEffect);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENDERMAN_TELEPORT , 0.8F, 1.0F);
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }
    
    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }
}
