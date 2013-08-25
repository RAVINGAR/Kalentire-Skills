/*
package com.herocraftonline.heroes.characters.skill.oldskills;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;

public class SkillConstrict extends TargettedSkill {
    
    private String applyText;
    private String expireText;
    
    public SkillConstrict(Heroes plugin) {
        super(plugin, "Constrict");
        setDescription("You slow the target's movement & attack speed for $1 seconds.");
        setUsage("/skill constrict");
        setArgumentRange(0, 0);
        setIdentifiers("skill constrict");
        setTypes(SkillType.DEBUFF, SkillType.MOVEMENT, SkillType.SILENCABLE, SkillType.HARMFUL, SkillType.DAMAGING);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("speed-multiplier", 3);
        node.set(SkillSetting.DURATION.node(), 15000);
        node.set(SkillSetting.APPLY_TEXT.node(), "%target% has been constricted by %hero%!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%target% is no longer constricted!");
        return node;
    }

    @Override
    public void init() {
        //super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%target% has been constricted by %hero%!").replace("%target%", "$1").replace("%hero%", "$2");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%target% is no longer constricted!").replace("%target%", "$1");
    }
    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
    	//Player player = hero.getPlayer();
        if (!(target instanceof Player))
            return SkillResult.INVALID_TARGET;
        
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 15000, false);
        int multiplier = SkillConfigManager.getUseSetting(hero, this, "speed-multiplier", 2, false);
        if (multiplier > 20) {
            multiplier = 20;
        }
        SlowEffect effect = new SlowEffect(this, duration, multiplier, true, applyText, expireText, hero);
        effect.types.add(EffectType.MAGIC);
        plugin.getCharacterManager().getHero((Player) target).addEffect(effect);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.SPIDER_WALK , 0.8F, 1.0F);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 15000, false);;
        return getDescription().replace("$1", duration / 1000 + "");
    }
}
*/