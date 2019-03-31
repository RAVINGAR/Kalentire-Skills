package com.herocraftonline.heroes.characters.skill.reborn.chronomancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillDimensionalRift extends TargettedSkill {
    public SkillDimensionalRift(Heroes plugin) {
        super(plugin, "DimensionalRift");
        setDescription("Open a rift in space between you and your target, distorting both time and space. " +
                "You and your target will switch places and all spellcasting will be interrupted. " +
                "As a result of the time disruption, all enemies within $1 block of either location will take $2 damage, " +
                "your target will lose $3 beneficial effect, and you will lose $4 harmful effect.");
        setUsage("/skill dimensionalrift");
        setArgumentRange(0, 0);
        setIdentifiers("skill dimensionalrift");
        setTypes(SkillType.ABILITY_PROPERTY_TEMPORAL, SkillType.SILENCEABLE, SkillType.TELEPORTING, SkillType.INTERRUPTING, SkillType.MULTI_GRESSIVE, SkillType.NO_SELF_TARGETTING);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 10);
        config.set("max-debuff-removals", 1);
        config.set("max-buff-removals", 1);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);

        if (targetCT.hasEffectType(EffectType.STUN) || targetCT.hasEffectType(EffectType.ROOT))
            return SkillResult.INVALID_TARGET;
        else if (hero.hasEffectType(EffectType.STUN) || hero.hasEffectType(EffectType.ROOT))
            return SkillResult.INVALID_TARGET;

        broadcastExecuteText(hero, target);

        Location pLocation = player.getLocation();
        pLocation.setYaw(target.getLocation().getYaw());
        pLocation.setPitch(target.getLocation().getPitch());

        Location tLocation = target.getLocation();
        tLocation.setYaw(player.getLocation().getYaw());
        tLocation.setPitch(player.getLocation().getPitch());

        dispelTarget(hero, hero);
        playFirework(player.getLocation());

        if (hero.isAlliedTo(target))
            dispelTarget(hero, targetCT);
        else
            purgeTarget(hero, targetCT);
        playFirework(target.getLocation());

        player.teleport(tLocation);
        target.teleport(pLocation);

        return SkillResult.NORMAL;
    }

    private void playFirework(Location location) {
        FireworkEffect firework = FireworkEffect.builder()
                .flicker(true)
                .trail(false)
                .withColor(Color.PURPLE)
                .withColor(Color.PURPLE)
                .withColor(Color.BLACK)
                .withFade(Color.BLACK)
                .with(FireworkEffect.Type.BURST)
                .build();
        VisualEffect.playInstantFirework(firework, location);
    }

    private void purgeTarget(Hero hero, CharacterTemplate targetCT) {
        int maxBuffRemovals = SkillConfigManager.getUseSetting(hero, this, "max-buff-removals", 1, false);
        if (maxBuffRemovals < 1)
            return;

        for (Effect effect : targetCT.getEffects()) {
            if (effect.isType(EffectType.DISPELLABLE) && effect.isType(EffectType.BENEFICIAL)) {
                hero.removeEffect(effect);
                maxBuffRemovals--;
                if (maxBuffRemovals == 0) {
                    break;
                }
            }
        }
    }

    private void dispelTarget(Hero hero, CharacterTemplate targetCT) {
        int maxDebuffRemovals = SkillConfigManager.getUseSetting(hero, this, "max-debuff-removals", 1, false);
        if (maxDebuffRemovals < 1)
            return;

        for (Effect effect : targetCT.getEffects()) {
            if (effect.isType(EffectType.DISPELLABLE) && effect.isType(EffectType.HARMFUL)) {
                targetCT.removeEffect(effect);
                maxDebuffRemovals--;
                if (maxDebuffRemovals == 0) {
                    break;
                }
            }
        }
    }
}