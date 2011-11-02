package com.herocraftonline.dev.heroes.skill.skills;

import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.SkillType;

import org.bukkit.util.config.ConfigurationNode;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.effects.EffectType;
import com.herocraftonline.dev.heroes.effects.ExpirableEffect;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.util.Setting;

public class SkillFarSight extends ActiveSkill {
    
    public SkillFarSight(Heroes plugin) {
        super(plugin, "FarSight");
        setDescription("Slows the target's movement speed & attack speed");
        setUsage("/skill farsight");
        setArgumentRange(0, 1);
        setIdentifiers("skill farsight", "skill fsight");
        setTypes(SkillType.BUFF, SkillType.MOVEMENT, SkillType.SILENCABLE, SkillType.STEALTHY);
    }

    @Override
    public ConfigurationNode getDefaultConfig() {
        ConfigurationNode node = super.getDefaultConfig();
        node.setProperty(Setting.DURATION.node(), 15000);
        return node;
    }

	@Override
	public boolean use(Hero hero, String[] args) {     
		if (hero.hasEffect("Zoom")) {
			hero.removeEffect(hero.getEffect("Zoom"));
			return false;
		}
		
        int duration = getSetting(hero, Setting.DURATION.node(), 15000, false);
        ZoomEffect effect = new ZoomEffect(this, duration);
        hero.addEffect(effect);
        return true;
	}
	
	public class ZoomEffect extends ExpirableEffect {

		public ZoomEffect(Skill skill, long duration) {
			super(skill, "Zoom", duration);
			this.types.add(EffectType.BENEFICIAL);
			this.types.add(EffectType.SLOW);
			addMobEffect(2, (int) (duration / 1000) * 20, 10, false);
		}
	}
}