package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillCleanseSpirit extends TargettedSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();

    public SkillCleanseSpirit(Heroes plugin) {
        super(plugin, "CleanseSpirit");
        setDescription("Cleanse the spirit of the target, restoring $1 of their health and removing $2 random debuff(s) that does not impede movement. You are only healed for $3 health from this ability.");
        setUsage("/skill cleansespirit <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill cleansespirit");
        setTypes(SkillType.ABILITY_PROPERTY_LIGHT, SkillType.DISPELLING, SkillType.HEALING, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING.node(), Integer.valueOf(125), false);
        double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 2.0, false);
        healing += (hero.getAttributeValue(AttributeType.WISDOM) * healingIncrease);
        
        int effectRemovals = SkillConfigManager.getUseSetting(hero, this, "max-effect-removals", 1, false);
        
        String formattedHealing = Util.decFormat.format(healing);
        String formattedSelfHealing = Util.decFormat.format(healing * Heroes.properties.selfHeal);

        return getDescription().replace("$1", formattedHealing).replace("$2", effectRemovals + "").replace("$3", formattedSelfHealing);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 10);
        node.set(SkillSetting.HEALING.node(), 150);
        node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 3.75);
        node.set("max-effect-removals", 1);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (!(target instanceof Player)) {
            return SkillResult.INVALID_TARGET;
        }

        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING, Integer.valueOf(125), false);
        double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM, 2.0, false);
        healing += (hero.getAttributeValue(AttributeType.WISDOM) * healingIncrease);

        double targetHealth = target.getHealth();
        if (targetHealth >= target.getMaxHealth()) {
            if (player.equals(targetHero.getPlayer())) {
                Messaging.send(player, "You are already at full health.");
            }
            else {
                Messaging.send(player, "Target is already fully healed.");
            }
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(targetHero, healing, this, hero);
        plugin.getServer().getPluginManager().callEvent(hrhEvent);
        if (hrhEvent.isCancelled()) {
            Messaging.send(player, "Unable to heal the target at this time!");
            return SkillResult.CANCELLED;
        }

        targetHero.heal(hrhEvent.getAmount());

        for (Effect effect : targetHero.getEffects()) {
            if (effect.isType(EffectType.DISPELLABLE) && effect.isType(EffectType.HARMFUL)) {
                if (effect.isType(EffectType.FIRE)) {
                    targetHero.removeEffect(effect);
                }
            }
        }
        
        int maxRemovals = SkillConfigManager.getUseSetting(hero, this, "max-effect-removals", 1, false);

        for (Effect effect : targetHero.getEffects()) {
            // This combined with checking for DISPELLABLE and HARMFUL is so huge I'd rather split the lines. Disallow dispelling movement impediment, don't want the class countering itself.
            boolean isMovementImpeding = effect.isType(EffectType.SLOW) || effect.isType(EffectType.VELOCITY_DECREASING) ||
                    effect.isType(EffectType.WALK_SPEED_DECREASING) || effect.isType(EffectType.ROOT);
            if (!isMovementImpeding && effect.isType(EffectType.DISPELLABLE) && effect.isType(EffectType.HARMFUL )) {
                hero.removeEffect(effect);
                // Just in case it's fire
                if (effect.isType(EffectType.FIRE)) {
                    target.setFireTicks(0);
                }
                maxRemovals--;
                if (maxRemovals == 0) {
                    break;
                }
            }
        }

        broadcastExecuteText(hero, target);

        // Should probably add an effect. I'm really not up to it, though.

        return SkillResult.NORMAL;
    }
}
