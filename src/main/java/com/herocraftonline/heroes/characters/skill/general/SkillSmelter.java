package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;

public class SkillSmelter extends PassiveSkill {
    public SkillSmelter(Heroes plugin) {
        super(plugin, "SkillSmelter");

        setDescription("You are particularly skilled at extraction alloys from ores. You have a chance to extract additional materials from raw ores");
        setArgumentRange(0, 0);
        setEffectTypes(EffectType.BENEFICIAL);
    }

    @Override
    public String getDescription(Hero hero) {
        return null;
    }
}
