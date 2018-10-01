package com.herocraftonline.heroes.characters.skill.codari;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseWeaponImbue;

public class SkillWeaponImbueTest extends SkillBaseWeaponImbue {

    public SkillWeaponImbueTest(Heroes plugin, String name) {
        super(plugin, name);
        setDescription("Imbues your weapon with the amazing power of debugging!");
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    protected void apply(Hero hero, String[] strings, WeaponDamageEvent weaponDamageEvent) {
        hero.getPlayer().sendMessage("APPLIED!");
    }
}
