package com.herocraftonline.dev.heroes.skill.skills;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.effects.Effect;
import com.herocraftonline.dev.heroes.effects.EffectType;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Messaging;

public class SkillEscapeArtist extends ActiveSkill {

    public SkillEscapeArtist(Heroes plugin) {
        super(plugin, "EscapeArtist");
        setDescription("Dispels any effects that impede your movement");
        setUsage("/skill escapeartist");
        setArgumentRange(0, 0);
        setIdentifiers("skill escapeartist", "skill eartist", "skill escape");
        setTypes(SkillType.MOVEMENT, SkillType.COUNTER, SkillType.PHYSICAL, SkillType.STEALTHY);
    }

    @Override
    public boolean use(Hero hero, String[] args) {
        boolean removed = false;
        for (Effect effect : hero.getEffects()) {
            if (effect.isType(EffectType.SLOW) || effect.isType(EffectType.STUN) || effect.isType(EffectType.ROOT)) {
                removed = true;
                hero.removeEffect(effect); 
            }
        }

        if (removed) {
            broadcastExecuteText(hero);
            return true;
        } else  {
            Messaging.send(hero.getPlayer(), "There is no effect impeding your movement!");
            return false;
        }
    }

}
