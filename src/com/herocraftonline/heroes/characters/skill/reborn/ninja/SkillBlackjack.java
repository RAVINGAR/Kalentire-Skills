package com.herocraftonline.heroes.characters.skill.reborn.ninja;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.StunEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillBlackjack extends TargettedSkill {

    public SkillBlackjack(Heroes plugin) {
        super(plugin, "Blackjack");
        setDescription("Strike your target with a Blackjack, dealing $1 damage and stunning the target for $2 second(s).$3");
        setUsage("/skill blackjack");
        setIdentifiers("skill blackjack");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.AGGRESSIVE, SkillType.DAMAGING, SkillType.STEALTHY);
    }

    @Override
    public String getDescription(Hero hero) {
        int str = hero.getAttributeValue(AttributeType.STRENGTH);
        int cha = hero.getAttributeValue(AttributeType.CHARISMA);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 60, false);
        double stealthyDamage = SkillConfigManager.getUseSetting(hero, this, "stealthy-damage", 60, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, "stun-duration", 500, false);
        long stealthyDuration = SkillConfigManager.getUseSetting(hero, this, "stealthy-duration", 60, false);

        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1, false);
        damage += str * damageIncrease;
        stealthyDamage += str * damageIncrease;

        long durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 15, false);
        duration += cha * durationIncrease;
        stealthyDuration += cha * durationIncrease;

        String endText = "";
        if (stealthyDamage > 0 || stealthyDuration > 0) {
            endText = " If you are invisible or sneaking, you will instead ";
        }
        if (stealthyDamage > 0) {
            endText += "do " + stealthyDamage + " damage";
            if (stealthyDuration > 0)
                endText+= " and";
            else
                endText += ".";
        }
        if (stealthyDuration > 0) {
            endText += "stun for " + Util.decFormat.format(stealthyDuration / 1000.0) + " second(s).";
        }

        return getDescription()
                .replace("$1", Util.decFormat.format(damage))
                .replace("$2", Util.decFormat.format(duration / 1000.0))
                .replace("$3", endText);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 4.5);
        config.set(SkillSetting.DAMAGE.node(), 30.0);
        config.set("stealthy-damage", 60.0);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 1.0);
        config.set(SkillSetting.DURATION_INCREASE_PER_CHARISMA.node(), 15.0);
        config.set("stealthy-duration", 1500);
        config.set("stun-duration", 1250);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        int str = hero.getAttributeValue(AttributeType.STRENGTH);
        int cha = hero.getAttributeValue(AttributeType.CHARISMA);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 60, false);
        double stealthyDamage = SkillConfigManager.getUseSetting(hero, this, "stealthy-damage", 60, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, "stun-duration", 500, false);
        long stealthyDuration = SkillConfigManager.getUseSetting(hero, this, "stealthy-duration", 60, false);

        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1, false);
        damage += str * damageIncrease;
        stealthyDamage += str * damageIncrease;

        long durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 15, false);
        duration += cha * durationIncrease;
        stealthyDuration += cha * durationIncrease;

        boolean isStealthy = false;
        for (Effect effect : hero.getEffects()) {
            if (effect.isType(EffectType.INVIS) || effect.isType(EffectType.SNEAK)) {
                isStealthy = true;
                break;
            }
        }

        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);

        addSpellTarget(target, hero);
        if (isStealthy) {
            damageEntity(target, player, stealthyDamage, EntityDamageEvent.DamageCause.ENTITY_ATTACK, false);
            targetCT.addEffect(new StunEffect(this, player, stealthyDuration));
        } else {
            damageEntity(target, player, damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK, false);
            targetCT.addEffect(new StunEffect(this, player, duration));
        }

        // Remove any invis effects the player may have on them at the time of use.
        for (final Effect effect : hero.getEffects()) {
            if (effect.isType(EffectType.INVIS)) {
                hero.removeEffect(effect);
            }
        }

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_WOODEN_DOOR_CLOSE, 0.4F, 0.4F);
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), org.bukkit.Effect.COLOURED_DUST, 0, 0, 0, 0, 0, 1, 150, 16);
//        player.getWorld().spawnParticle(Particle.REDSTONE, target.getLocation().add(0, 0.5, 0), 150, 0, 0, 0, 1, new Particle.DustOptions(Color.BLACK, 1));
        return SkillResult.NORMAL;
    }
}