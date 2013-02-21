package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.configuration.ConfigurationSection;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillFarSight extends ActiveSkill {
    
    public SkillFarSight(Heroes plugin) {
        super(plugin, "FarSight");
        setDescription("You are able to look far into the distance, but your movement is slowed.");
        setUsage("/skill farsight");
        setArgumentRange(0, 1);
        setIdentifiers("skill farsight", "skill fsight");
        setTypes(SkillType.BUFF, SkillType.MOVEMENT, SkillType.SILENCABLE, SkillType.STEALTHY);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 15000);
        return node;
    }

	@Override
	public SkillResult use(Hero hero, String[] args) {     
		if (hero.hasEffect("Zoom")) {
			hero.removeEffect(hero.getEffect("Zoom"));
			return SkillResult.REMOVED_EFFECT;
		}
		
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 15000, false);
        ZoomEffect effect = new ZoomEffect(this, duration);
        hero.addEffect(effect);
        return SkillResult.NORMAL;
	}
	
	public class ZoomEffect extends ExpirableEffect {

		public ZoomEffect(Skill skill, long duration) {
			super(skill, "Zoom", duration);
			this.types.add(EffectType.BENEFICIAL);
			this.types.add(EffectType.SLOW);
			addMobEffect(2, (int) (duration / 1000) * 20, 10, false);
		}
	}

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
}