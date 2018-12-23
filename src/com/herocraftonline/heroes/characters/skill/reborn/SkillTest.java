package com.herocraftonline.heroes.characters.skill.reborn;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;

public class SkillTest extends ActiveSkill {

    public SkillTest(Heroes plugin) {
        super(plugin, "Test");
        setDescription("Skill for Delf to test stuff.");
        setUsage("/skill test");
        setArgumentRange(0, 10);
        setIdentifiers("skill test");
    }

    @Override
    public SkillResult use(Hero hero, String[] strings) {
        return null;
    }

    @Override
    public String getDescription(Hero hero) {
        return null;
    }
}
