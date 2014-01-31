package com.herocraftonline.heroes.characters.skill.skills.totem;

import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import org.bukkit.entity.Player;

public class TotemEffect extends PeriodicExpirableEffect {

    private SkillBaseTotem totemSkill;
    private Totem totem;    
    private boolean forcedExpire;
    
    public TotemEffect(SkillBaseTotem skill, Totem totem, Player applier, long period, long duration) {
        this(skill, "TotemEffect", totem, applier, period, duration);
    }
    
    
    public TotemEffect(SkillBaseTotem skill, String name, Totem totem, Player applier, long period, long duration) {
        super(skill, name, applier, period, duration);
        totemSkill = skill;
        this.totem = totem;

    }

    @Override
    public void tickHero(Hero hero) {
        totemSkill.usePower(hero, totem);
    }

    @Override
    public void applyToHero(Hero hero) {
        totem.setEffect(this);
        totem.createTotem(totemSkill.getMaterials());
        SkillBaseTotem.totems.add(totem);
        super.applyToHero(hero);
    }
    
    @Override
    public void removeFromHero(Hero hero) {
        if(!forcedExpire && totem.getFireOnNaturalRemove()) {
            totemSkill.usePower(hero, totem);
        }
        totem.destroyTotem();
        SkillBaseTotem.totems.remove(totem);
        super.removeFromHero(hero);
    }
    
    @Override
    public void expire() {
        forcedExpire = true;
        super.expire();
    }
    
    public Totem getTotem() {
        return totem;
    }
    
    public SkillBaseTotem getTotemSkill() {
        return totemSkill;
    }
    
    @Override
    public void tickMonster(Monster monster) {
        // Monsters don't get Totems.
    }

}
