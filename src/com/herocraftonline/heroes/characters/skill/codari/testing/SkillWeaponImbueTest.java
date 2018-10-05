package com.herocraftonline.heroes.characters.skill.codari.testing;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.PeriodicStackingEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseWeaponImbue;
import org.bukkit.entity.LivingEntity;

public class SkillWeaponImbueTest extends SkillBaseWeaponImbue {

    public SkillWeaponImbueTest(Heroes plugin) {
        super(plugin, "WeaponImbueTest");
        setDescription("Imbues your weapon with the amazing power of debugging!");

        setUsage("/skill WeaponImbueTest");
        setArgumentRange(0, 10);
        setIdentifiers("skill WeaponImbueTest");
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    protected void apply(Hero hero, String[] strings, WeaponDamageEvent weaponDamageEvent) {

    }
}
