package com.herocraftonline.heroes.characters.skill.public1;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;

import java.util.List;

public class SkillTaunt extends ActiveSkill {

    public SkillTaunt(Heroes plugin) {
        super(plugin, "Taunt");
        this.setDescription("You taunt nearby enemies.");
        this.setUsage("/skill taunt");
        this.setArgumentRange(0, 0);
        this.setIdentifiers("skill taunt");
        this.setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL);
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        final List<Entity> entities = hero.getPlayer().getNearbyEntities(5, 5, 5);
        for (final Entity n : entities) {
            if (n instanceof Monster) {
                ((Monster) n).setTarget(hero.getPlayer());
            }
        }
        this.broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        return this.getDescription();
    }

}
