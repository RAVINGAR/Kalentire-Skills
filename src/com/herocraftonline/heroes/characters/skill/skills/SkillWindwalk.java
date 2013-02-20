package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.QuickenEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;

public class SkillWindwalk extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillWindwalk(Heroes plugin) {
        super(plugin, "Windwalk");
        setDescription("You run with the wind for $1 seconds.");
        setUsage("/skill windwalk");
        setArgumentRange(0, 0);
        setIdentifiers("skill windwalk");
        setTypes(SkillType.BUFF, SkillType.MOVEMENT, SkillType.SILENCABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("speed-multiplier", 2);
        node.set(Setting.DURATION.node(), 15000);
        node.set("apply-text", "%hero% is one with the wind!");
        node.set("expire-text", "%hero% returned to normal speed!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "%hero% is one with the wind!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "%hero% returned to normal speed!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);

        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 15000, false);
        int multiplier = SkillConfigManager.getUseSetting(hero, this, "speed-multiplier", 2, false);
        if (multiplier > 20) {
            multiplier = 20;
        }
        hero.addEffect(new QuickenEffect(this, getName(), duration, multiplier, applyText, expireText));
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENDERMAN_TELEPORT , 10.0F, 1.0F);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 1, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }
}