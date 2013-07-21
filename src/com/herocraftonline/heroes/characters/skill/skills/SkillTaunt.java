package com.herocraftonline.heroes.characters.skill.skills;

import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillTaunt extends ActiveSkill {

    public SkillTaunt(Heroes plugin) {
        super(plugin, "Taunt");
        setDescription("You taunt nearby enemies.");
        setUsage("/skill taunt");
        setArgumentRange(0, 0);
        setIdentifiers("skill taunt");
        setTypes(SkillType.PHYSICAL);
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        List<Entity> entities = hero.getPlayer().getNearbyEntities(5, 5, 5);
        for (Entity n : entities) {
            if (n instanceof Monster) {
                ((Monster) n).setTarget(hero.getPlayer());
            }
        }
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
}
