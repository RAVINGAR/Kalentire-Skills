package com.herocraftonline.heroes.characters.skill.remastered;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillFarSight extends ActiveSkill {

    public SkillFarSight(Heroes plugin) {
        super(plugin, "FarSight");
        setDescription("You are able to look far into the distance, but your movement is slowed.");
        setUsage("/skill farsight");
        setArgumentRange(0, 1);
        setIdentifiers("skill farsight", "skill fsight");
        setTypes(SkillType.BUFFING, SkillType.SILENCEABLE, SkillType.STEALTHY);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 12000);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        if (hero.hasEffect("Zoom")) {
            hero.removeEffect(hero.getEffect("Zoom"));
            return SkillResult.REMOVED_EFFECT;
        }

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 12000, false);
        ZoomEffect effect = new ZoomEffect(this, hero.getPlayer(), duration);
        hero.addEffect(effect);

        return SkillResult.NORMAL;
    }

    public class ZoomEffect extends ExpirableEffect {

        public ZoomEffect(Skill skill, Player applier, long duration) {
            super(skill, "Zoom", applier, duration);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.SLOW);

            addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (duration / 1000) * 20, 10), false);
        }
    }
}