package com.herocraftonline.heroes.characters.skill.codari.testing;

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

        targetCharacter.addEffectStack("Stacking", 2000, player, this, name -> new TestEffect(this, name, player));

        return SkillResult.NORMAL;
    }

    public class TestEffect extends PeriodicStackingEffect {

        public TestEffect(Skill skill, String name, Player applier) {
            super(skill, name, applier, 10, 500);
        }

        @Override
        public void tickMonster(Monster monster) {
            applier.sendMessage("    " + ChatComponents.GENERIC_SKILL + ChatColor.GRAY + "Stacks: " + ChatColor.WHITE + getStackCount());
        }

        @Override
        public void tickHero(Hero hero) {
            applier.sendMessage("    " + ChatComponents.GENERIC_SKILL + ChatColor.GRAY + "Stacks: " + ChatColor.WHITE + getStackCount());
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
