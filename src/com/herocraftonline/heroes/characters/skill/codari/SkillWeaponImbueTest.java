package com.herocraftonline.heroes.characters.skill.codari;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseWeaponImbue;

public class SkillWeaponImbueTest extends SkillBaseWeaponImbue {

    public SkillWeaponImbueTest(Heroes plugin) {
        super(plugin, "WeaponImbueTest");
        setDescription("Imbues your weapon with the amazing power of debugging!");

        setUsage("/skill WeaponImbueTest");
        setArgumentRange(0, 0);
        setIdentifiers("skill WeaponImbueTest");
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
