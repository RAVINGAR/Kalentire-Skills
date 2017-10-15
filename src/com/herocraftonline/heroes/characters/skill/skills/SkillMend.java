package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.CompatSound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;

public class SkillMend extends SkillBaseHeal {

    public SkillMend(Heroes plugin) {
        super(plugin, "Mend");
        setDescription("Mend wour wounds, restoring $1 health and curing bleed and fire effects.");
        setUsage("/skill Mend <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill Mend");
        setTypes(SkillType.ABILITY_PROPERTY_LIGHT, SkillType.DISPELLING, SkillType.HEALING, SkillType.SILENCEABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 8);
        node.set(SkillSetting.HEALING.node(), 65);
        node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 1.5);

        return node;
    }

    @Override
    protected void applySoundEffects(World world, LivingEntity target) {
        world.playSound(target.getLocation(), CompatSound.ENTITY_CHICKEN_EGG.value(), 0.5F, 0.01F);
    }

    @Override
    protected void applyParticleEffects(World world, LivingEntity target) {
        // Original skill had no particle effects, placeholder to keep it the same
    }

    @Override
    protected void removeEffects(Hero hero) {
        for (Effect effect : hero.getEffects()) {
            if (effect.isType(EffectType.DISPELLABLE) && effect.isType(EffectType.HARMFUL)) {
                if (effect.isType(EffectType.BLEED)&& effect.isType(EffectType.FIRE)) {
                    hero.removeEffect(effect);
                }
            }
        }
    }
}
