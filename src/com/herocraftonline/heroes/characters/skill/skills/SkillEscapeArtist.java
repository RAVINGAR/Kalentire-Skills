package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.QuickenEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;

public class SkillEscapeArtist extends ActiveSkill {

    public SkillEscapeArtist(Heroes plugin) {
        super(plugin, "EscapeArtist");
        setDescription("You break free of any effects that impede your movement.");
        setUsage("/skill escapeartist");
        setArgumentRange(0, 0);
        setIdentifiers("skill escapeartist", "skill eartist", "skill escape");
        setTypes(SkillType.MOVEMENT, SkillType.COUNTER, SkillType.PHYSICAL, SkillType.STEALTHY);
    }
    
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("speed-multiplier", 2);
        node.set(Setting.DURATION.node(), 15000);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        boolean removed = false;
        for (Effect effect : hero.getEffects()) {
            if (effect.isType(EffectType.DISABLE) || effect.isType(EffectType.SLOW) || effect.isType(EffectType.STUN) || effect.isType(EffectType.ROOT)) {
                removed = true;
                hero.removeEffect(effect); 
            }
        }

        if (removed) {
        	int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 15000, false);
            int multiplier = SkillConfigManager.getUseSetting(hero, this, "speed-multiplier", 2, false);
            if(multiplier > 20)
            	multiplier = 20;
            hero.addEffect(new QuickenEffect(this, getName(), duration, multiplier, "$1 gained a burst of speed!", "$1 returned to normal speed!"));
            broadcastExecuteText(hero);
            hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.BAT_DEATH , 0.8F, 1.0F);
            return SkillResult.NORMAL;
        } else  {
            Messaging.send(hero.getPlayer(), "There is no effect impeding your movement!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
}
