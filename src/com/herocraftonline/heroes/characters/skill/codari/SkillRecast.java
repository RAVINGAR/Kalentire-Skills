package com.herocraftonline.heroes.characters.skill.codari;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.skills.RecastData;

public class SkillRecast extends ActiveSkill {


    public SkillRecast(Heroes plugin) {
        super(plugin, "Recast");
        setDescription("ADsadasdasdasdfaasgfg425bb2 6247n7265JOI(*VB# O*(%O@(VY@ BP%V(@");

        setUsage("/skill " + getName());
        setArgumentRange(0, 0);
        setIdentifiers("skill " + getName());
    }

    @Override
    public SkillResult use(Hero hero, String[] strings) {
        broadcastExecuteText(hero);
        startRecast(hero, 5000, new CustomRecastData(25));
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public void recast(Hero hero, RecastData data) {
        hero.getPlayer().sendMessage("RECAST!!!!!");
    }

    @Override
    public void onRecastEnd(Hero hero, RecastData data) {
        hero.getPlayer().sendMessage("END RECAST!!!");
    }

    private class CustomRecastData extends RecastData {

        public int i;

        public CustomRecastData(int i) {
            super("Recast");
            this.i = i;
        }
    }
}
