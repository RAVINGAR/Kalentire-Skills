package com.herocraftonline.heroes.characters.skill.reborn.druid;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class SkillGiftOfEir extends ActiveSkill {

    public SkillGiftOfEir(Heroes plugin, String name) {
        super(plugin, name);
    }

    @Override
    public SkillResult use(Hero hero, String[] strings) {
        return null;
    }

    @Override
    public String getDescription(Hero hero) {
        return null;
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.MANA.node(), 60);
        node.set(SkillSetting.RADIUS.node(), 5);
        node.set(SkillSetting.DURATION.node(), 5000);
        return node;
    }

    public ArrayList<Location> circle(Location centerPoint, int particleAmount, double circleRadius) {
        World world = centerPoint.getWorld();

        double increment = (2 * Math.PI) / particleAmount;

        ArrayList<Location> locations = new ArrayList<Location>();

        for (int i = 0; i < particleAmount; i++) {
            double angle = i * increment;
            double x = centerPoint.getX() + (circleRadius * Math.cos(angle));
            double z = centerPoint.getZ() + (circleRadius * Math.sin(angle));
            locations.add(new Location(world, x, centerPoint.getY(), z));
        }
        return locations;
    }
    public class ManaShareEffect extends PeriodicExpirableEffect {

        public ManaShareEffect(Skill skill, Player applier, long duration, int period) {
            super(skill, "ManaShareEffect", applier, duration, period);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.AREA_OF_EFFECT);
            //Maybe
            types.add(EffectType.MANA_REGEN_INCREASING);



        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
        }

        @Override
        public void tickMonster(Monster monster) {

        }

        @Override
        public void tickHero(Hero hero) {

        }
    }
}

