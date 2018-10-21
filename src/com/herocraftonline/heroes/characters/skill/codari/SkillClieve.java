package com.herocraftonline.heroes.characters.skill.codari;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;

public class SkillClieve extends ActiveSkill {

    public SkillClieve(Heroes plugin) {
        super(plugin, "Clieve");
        setDescription("Stuff");

        setUsage("/skill " + getName());
        setArgumentRange(0, 0);
        setIdentifiers("skill " + getName());
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public SkillResult use(Hero hero, String[] strings) {
        return null;
    }
}
