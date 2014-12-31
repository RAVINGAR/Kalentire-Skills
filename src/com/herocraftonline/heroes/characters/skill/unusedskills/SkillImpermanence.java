package com.herocraftonline.heroes.characters.skill.unusedskills;

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
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;

public class SkillImpermanence extends ActiveSkill {

    public SkillImpermanence(Heroes plugin) {
        super(plugin, "Impermanence");
        setDescription("You break free of any effects that impede your movement.");
        setUsage("/skill impermanence");
        setArgumentRange(0, 0);
        setIdentifiers("skill impermanence", "skill imp", "skill imper");
        setTypes(SkillType.MOVEMENT_INCREASING, SkillType.DISABLE_COUNTERING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.STEALTHY);
    }
    
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("speed-multiplier", 2);
        node.set(SkillSetting.DURATION.node(), 15000);
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
        	int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 15000, false);
            int multiplier = SkillConfigManager.getUseSetting(hero, this, "speed-multiplier", 2, false);
            if(multiplier > 20)
            	multiplier = 20;
            hero.addEffect(new QuickenEffect(this, getName(), hero.getPlayer(), duration, multiplier, "$1 gained a burst of speed!", "$1 returned to normal speed!"));
            hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.SHEEP_SHEAR , 0.8F, 1.0F);
            broadcastExecuteText(hero);
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
