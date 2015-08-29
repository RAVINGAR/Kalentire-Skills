package com.herocraftonline.heroes.characters.skill.unusedskills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.SpeedEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class SkillMindandBody extends ActiveSkill {

    public SkillMindandBody(Heroes plugin) {
        super(plugin, "MindandBody");
        setDescription("You break free of any effects that impede your movement.");
        setUsage("/skill mindandbody");
        setArgumentRange(0, 0);
        setIdentifiers("skill mindandbody", "skill mind_and_body", "skill mind");
        setTypes(SkillType.MOVEMENT_PREVENTION_COUNTERING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.STEALTHY);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
    
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("speed-multiplier", 0);
        node.set(SkillSetting.DURATION.node(), 0);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        boolean removed = false;
        for (Effect effect : hero.getEffects()) {
            if (effect.isType(EffectType.SLOW) || effect.isType(EffectType.VELOCITY_DECREASING) || effect.isType(EffectType.WALK_SPEED_DECREASING) || effect.isType(EffectType.ROOT)) {
                removed = true;
                hero.removeEffect(effect); 
            }
        }

        if (removed) {
            int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 7000, false);
            int multiplier = SkillConfigManager.getUseSetting(hero, this, "speed-multiplier", 2, false);
            if (duration > 0 && multiplier > 0) {
                hero.addEffect(new SpeedEffect(this, getName(), player, duration, multiplier, "$1 gained a burst of speed!", "$1 returned to normal speed!"));
                broadcastExecuteText(hero);
                player.getWorld().playSound(player.getLocation(), Sound.BAT_DEATH, 0.8F, 1.0F);
            }
        }
        else {
            Messaging.send(player, "There is no effect impeding your movement!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        return SkillResult.NORMAL;
    }
}
