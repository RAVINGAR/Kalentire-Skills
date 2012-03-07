package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Setting;

public class SkillSlow extends TargettedSkill {
    
    private String applyText;
    private String expireText;
    
    public SkillSlow(Heroes plugin) {
        super(plugin, "Slow");
        setDescription("You slow the target's movement & attack speed for $1 seconds.");
        setUsage("/skill slow");
        setArgumentRange(0, 1);
        setIdentifiers("skill slow");
        setTypes(SkillType.DEBUFF, SkillType.MOVEMENT, SkillType.SILENCABLE, SkillType.HARMFUL);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("speed-multiplier", 2);
        node.set(Setting.DURATION.node(), 15000);
        node.set(Setting.APPLY_TEXT.node(), "%target% has been slowed by %hero%!");
        node.set(Setting.EXPIRE_TEXT.node(), "%target% is no longer slowed!");
        return node;
    }


    @Override
    public void init() {
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "%target% has been slowed by %hero%!").replace("%target%", "$1").replace("%hero%", "$2");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "%target% is no longer slowed!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        if (!(target instanceof Player))
            return SkillResult.INVALID_TARGET;
        
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 15000, false);
        int multiplier = SkillConfigManager.getUseSetting(hero, this, "speed-multiplier", 2, false);
        if (multiplier > 20) {
            multiplier = 20;
        }
        SlowEffect effect = new SlowEffect(this, duration, multiplier, true, applyText, expireText, hero);
        plugin.getCharacterManager().getHero((Player) target).addEffect(effect);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 15000, false);;
        return getDescription().replace("$1", duration / 1000 + "");
    }
}