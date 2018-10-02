package com.herocraftonline.heroes.characters.skill.codari;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.PeriodicStackingEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.chat.ChatComponents;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillTargetedTest extends TargettedSkill {

    public SkillTargetedTest(Heroes plugin) {
        super(plugin, "TargetedTest");
        setDescription("Same as Test but with a target");

        setUsage("/skill TargetedTest");
        setArgumentRange(0, 10);
        setIdentifiers("skill TargetedTest");
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] strings) {

        Player player = hero.getPlayer();
        CharacterTemplate targetCharacter = plugin.getCharacterManager().getCharacter(target);

        Test effect = (Test) targetCharacter.getEffect("Stacking");
        if (effect == null) {
            effect = new Test(this, "Stacking", player);
            targetCharacter.addEffect(effect);
        }

        effect.addStack(targetCharacter, 2000, player, this);

        return SkillResult.NORMAL;
    }

    public class Test extends PeriodicStackingEffect {

        private Player applier;

        public Test(Skill skill, String name, Player applier) {
            super(skill, name, 10, 500);
            this.applier = applier;
        }

        @Override
        public void tickMonster(Monster monster) {
            applier.sendMessage("    " + ChatComponents.GENERIC_SKILL + ChatColor.GRAY + "Stacks: " + ChatColor.WHITE + getStackCount(monster));
        }

        @Override
        public void tickHero(Hero hero) {
            applier.sendMessage("    " + ChatComponents.GENERIC_SKILL + ChatColor.GRAY + "Stacks: " + ChatColor.WHITE + getStackCount(hero));
        }

        @Override
        public void apply(CharacterTemplate character) {
            super.apply(character);
            applier.sendMessage("    " + ChatComponents.GENERIC_SKILL + ChatColor.GRAY + "Applied");
        }

        @Override
        public void remove(CharacterTemplate character) {
            super.remove(character);
            applier.sendMessage("    " + ChatComponents.GENERIC_SKILL + ChatColor.GRAY + "Removed");
        }
    }
}
