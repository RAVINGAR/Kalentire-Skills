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

public class SkillAbsolution extends TargettedSkill {
    public VisualEffect fplayer = new VisualEffect();

    public SkillAbsolution(Heroes plugin) {
        super(plugin, "Absolution");
        setDescription("You restore $1 health to your target and remove DARK effects. Only heals for $2 if self targetted.");
        setUsage("/skill absolution <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill absolution");
        setTypes(SkillType.HEALING, SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_LIGHT);
    }

    @Override
    public String getDescription(Hero hero) {
        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING.node(), Integer.valueOf(125), false);
        double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), Double.valueOf(2.0), false);
        healing += (hero.getAttributeValue(AttributeType.WISDOM) * healingIncrease);

        String formattedHealing = Util.decFormat.format(healing);
        String formattedSelfHealing = Util.decFormat.format(healing * Heroes.properties.selfHeal);

        return getDescription().replace("$1", formattedHealing).replace("$2", formattedSelfHealing);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(5));
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), Double.valueOf(0.15));
        node.set(SkillSetting.HEALING.node(), Integer.valueOf(125));
        node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), Double.valueOf(2.0));

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
        double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM, Double.valueOf(2.0), false);
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
                if (effect.isType(EffectType.DARK)) {
                    targetHero.removeEffect(effect);
                }
            }
        }

        broadcastExecuteText(hero, target);

        /* this is our fireworks shit
        try {
            fplayer.playFirework(player.getWorld(),
                                 target.getLocation().add(0, 1.5, 0),
                                 FireworkEffect.builder()
                                               .flicker(false)
                                               .trail(false)
                                               .with(FireworkEffect.Type.BURST)
                                               .withColor(Color.MAROON)
                                               .withFade(Color.WHITE)
                                               .build());
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }*/

        player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.6, 0), org.bukkit.Effect.INSTANT_SPELL, 0, 0, 0, 0, 0, 1, 25, 16);
        return SkillResult.NORMAL;
    }
}
