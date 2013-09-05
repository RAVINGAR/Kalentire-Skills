package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;

public class SkillCauterize extends TargettedSkill {

    public SkillCauterize(Heroes plugin) {
        super(plugin, "Cauterize");
        setDescription("Cauterize the wounds of your target, extinguishing their fire ticks and removing bleeds.");
        setUsage("/skill cauterize");
        setArgumentRange(0, 0);
        setIdentifiers("skill cauterize");
        setTypes(SkillType.SILENCABLE, SkillType.DISPELLING, SkillType.ABILITY_PROPERTY_FIRE);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (!(target instanceof Player)) {
            return SkillResult.INVALID_TARGET;
        }

        broadcastExecuteText(hero);

        // Remove bleeds
        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
        for (Effect effect : targetHero.getEffects()) {
            if (effect.isType(EffectType.BLEED) && effect.isType(EffectType.HARMFUL)) {
                targetHero.removeEffect(effect);
            }
        }

        ((Player) target).setFireTicks(0);  // Clear fire ticks.

        player.getWorld().playSound(player.getLocation(), Sound.FIZZ, 2.0F, 1.2F);

        return SkillResult.NORMAL;
    }

}