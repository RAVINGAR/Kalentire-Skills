package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

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

public class SkillEscapeArtist extends ActiveSkill {

    public SkillEscapeArtist(Heroes plugin) {
        super(plugin, "EscapeArtist");
        setDescription("You break free of any effects that impede your movement.");
        setUsage("/skill escapeartist");
        setArgumentRange(0, 0);
        setIdentifiers("skill escapeartist", "skill eartist", "skill escape");
        setTypes(SkillType.MOVEMENT_PREVENTION_COUNTERING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.STEALTHY);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
    
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("speed-multiplier", Integer.valueOf(0));
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(0));

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        boolean removed = false;
        for (Effect effect : hero.getEffects()) {
            if (effect.isType(EffectType.DISABLE) || effect.isType(EffectType.SLOW) || effect.isType(EffectType.STUN) || effect.isType(EffectType.ROOT)) {
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
