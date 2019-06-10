package com.herocraftonline.heroes.characters.skill.reborn.pyromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.common.interfaces.Burning;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Collection;

public class SkillCombust extends TargettedSkill {

    public SkillCombust(Heroes plugin) {
        super(plugin, "Combust");
        setDescription("You Combust all enemies within $4 blocks of your target, absorbing all of their current burning effects, " +
                "dealing $1 damage in addition to all of the absorbed burning damage at a $2% increase rate. " +
                "The maximum amount of damage gained from burning effects is $3");
        setUsage("/skill combust");
        setIdentifiers("skill combust");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_FIRE, SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
    }

    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        double damageEffectiveness = SkillConfigManager.getUseSetting(hero, this, "burning-damage-effectiveness", 1.5, false);
        double maximumBurningDamage = SkillConfigManager.getUseSetting(hero, this, "maximum-burning-damage", 100.0, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(damage))
                .replace("$2", Util.decFormat.format(damageEffectiveness * 100))
                .replace("$3", Util.decFormat.format(maximumBurningDamage));
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 25.0);
        config.set(SkillSetting.RADIUS.node(), 5);
        config.set("burning-damage-effectiveness", 1.5);
        config.set("maximum-burning-damage", 100.0);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        double damageEffectiveness = SkillConfigManager.getUseSetting(hero, this, "damage-effectiveness", 1.5, false);
        double maximumBurningDamage = SkillConfigManager.getUseSetting(hero, this, "maximum-burning-damage", 100.0, false);
        combustTarget(hero, target, damage, damageEffectiveness, maximumBurningDamage);

        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5.0, false);

        Collection<Entity> nearbyEnts = target.getWorld().getNearbyEntities(target.getLocation(), radius, radius / 2, radius);
        for (Entity aoeEnt : nearbyEnts) {
            if (!(aoeEnt instanceof LivingEntity) || aoeEnt.equals(target))
                continue;
            if (aoeEnt.getFireTicks() < 1)
                continue;

            LivingEntity aoeTarget = (LivingEntity) aoeEnt;
            if (!damageCheck(player, aoeTarget))
                continue;

            combustTarget(hero, aoeTarget, damage, damageEffectiveness, maximumBurningDamage);
        }

        FireworkEffect firework = FireworkEffect.builder()
                .flicker(false)
                .trail(false)
                .withColor(Color.RED)
                .withColor(Color.RED)
                .withColor(Color.ORANGE)
                .withFade(Color.BLACK)
                .with(FireworkEffect.Type.BURST)
                .build();
        VisualEffect.playInstantFirework(firework, target.getLocation());

        return SkillResult.NORMAL;
    }

    public void combustTarget(Hero hero, LivingEntity target, double damage, double damageEffectiveness, double maximumBurningDamage) {
        double addedDamage = 0;

        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
        boolean foundBurningEffect = false;
        for (final Effect effect : targetCT.getEffects()) {
            if (!(effect instanceof Burning))
                continue;

            Burning burningEffect = (Burning) effect;
            addedDamage = burningEffect.getRemainingDamage() * damageEffectiveness;
            targetCT.removeEffect(effect);
            target.setFireTicks(0);
            foundBurningEffect = true;
        }

        if (!foundBurningEffect) {
            addedDamage = plugin.getDamageManager().calculateFireTickDamage(target, damageEffectiveness);
            target.setFireTicks(0);
        }

        if (addedDamage > maximumBurningDamage) {
            addedDamage = maximumBurningDamage;
        }

        double totalDamage = damage + addedDamage;
        String message = "    " + ChatComponents.GENERIC_SKILL + ChatColor.GOLD + "Combust Damage: " + Util.decFormat.format(totalDamage);

        if (addedDamage > 0) {
            message += " (" + addedDamage + " from Burning)";
        }

        hero.getPlayer().sendMessage(message);


        addSpellTarget(target, hero);
        damageEntity(target, hero.getPlayer(), totalDamage, EntityDamageEvent.DamageCause.MAGIC);
    }
}
