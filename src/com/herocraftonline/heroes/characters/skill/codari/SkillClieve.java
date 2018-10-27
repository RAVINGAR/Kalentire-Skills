package com.herocraftonline.heroes.characters.skill.codari;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseWeaponImbue;

public class SkillClieve extends SkillBaseWeaponImbue {

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
    protected void apply(Hero hero, String[] strings, WeaponDamageEvent weaponDamageEvent) {

    }
}
