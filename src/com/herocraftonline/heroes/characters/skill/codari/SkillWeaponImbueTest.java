package com.herocraftonline.heroes.characters.skill.codari;

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
        setArgumentRange(0, 0);
        setIdentifiers("skill WeaponImbueTest");
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    protected void apply(Hero hero, String[] strings, WeaponDamageEvent weaponDamageEvent) {

        final String NAME = "Test Stacking Effect";

        CharacterTemplate target = plugin.getCharacterManager().getCharacter((LivingEntity) weaponDamageEvent.getEntity());

        TestStackingEffect effect = (TestStackingEffect) target.getEffect(NAME);
        if (effect == null) {
            effect = new TestStackingEffect(this, NAME);
            target.addEffect(effect);
        }

        effect.addStack(hero, 10000, hero.getPlayer(), this);
    }

    public class TestStackingEffect extends PeriodicStackingEffect {

        public TestStackingEffect(Skill skill, String name) {
            super(skill, name, 5, 500);
        }

        @Override
        public void tickMonster(Monster monster) {

        }

        @Override
        public void tickHero(Hero hero) {
            hero.getPlayer().sendMessage("You have " + getStackCount(hero) + " stacks");
        }
    }
}
