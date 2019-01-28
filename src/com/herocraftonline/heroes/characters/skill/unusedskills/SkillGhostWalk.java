package com.herocraftonline.heroes.characters.skill.unusedskills;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.NightvisionEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillGhostWalk extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillGhostWalk(Heroes plugin) {
        super(plugin, "Ghost Walk");
        setDescription("You walk as a ghost at night for $1 seconds.");
        setUsage("/skill Ghost Walk");
        setArgumentRange(0, 0);
        setIdentifiers("skill Ghost Walk");
        setTypes(SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_ILLUSION);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 180000);
        node.set(SkillSetting.APPLY_TEXT.node(), "%hero% gains Ghost Walk!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%hero% lost Ghost Walk!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), "%hero% gains Ghost!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%hero% lost Ghost!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 180000, false);
        //hero.addEffect(new NightvisionEffect(this, hero.getPlayer(), duration, applyText, expireText));
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENTITY_WITHER_SPAWN , 0.5F, 1.0F);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 1, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }
}
