package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.configuration.ConfigurationSection;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillAbsolution extends SkillBaseHeal {

    public SkillAbsolution(Heroes plugin) {
        super(plugin, "Absolution");
        setDescription("You restore $1 health to your target and remove DARK effects. Only heals for $2 if self targetted.");
        setUsage("/skill absolution <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill absolution");
        setTypes(SkillType.HEALING, SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_LIGHT);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 5);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), 0.15);
        node.set(SkillSetting.HEALING.node(), 125);
        node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 2.0);

        return node;
    }

    @Override
    protected void removeEffects(Hero hero) {
        for (Effect effect : hero.getEffects()) {
            if (effect.isType(EffectType.DISPELLABLE) && effect.isType(EffectType.HARMFUL)) {
                if (effect.isType(EffectType.DARK)) {
                    hero.removeEffect(effect);
                }
            }
        }
    }
}
