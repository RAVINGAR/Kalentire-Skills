package com.herocraftonline.heroes.characters.skill.reborn;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;

import javax.swing.text.html.parser.Entity;

public class SkillSerratedArrows extends PassiveSkill {

    public SkillSerratedArrows(Heroes plugin) {
        super(plugin, "SerratedArrows");
        setDescription("Every %1% arrow you fire will shoot a Serrated Arrow, which will deal bonus damage and pierce through your targets Armor");
        setUsage("/skill serratedarrows");
        setArgumentRange(0, 0);
        setIdentifiers("skill serratedarrows");
        setTypes(SkillType.DAMAGING);
    }

    @Override
    public String getDescription(Hero hero) {
        return null;
    }


    public class SkillEntityListener implements Listener {

        private Skill skill;

        SkillEntityListener(Skill skill) {
            this.skill = skill;
        }


        public void onEntityShootBow(EntityShootBowEvent event) {



        }


    }


}
