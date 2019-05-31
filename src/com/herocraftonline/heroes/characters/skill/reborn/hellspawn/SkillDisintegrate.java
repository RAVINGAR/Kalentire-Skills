package com.herocraftonline.heroes.characters.skill.reborn.hellspawn;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.interfaces.Stacked;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillDisintegrate extends TargettedSkill {

    public SkillDisintegrate(Heroes plugin) {
        super(plugin, "Disintegrate");
        setDescription("You Disintegrate an enemy, dealing $1 damage and consuming all of their Withering effects. " +
                "You deal an additional $2 damage for each effect consumed. " +
                "The maximum bonus damage that can be dealt is $3.");
        setUsage("/skill disintegrate");
        setIdentifiers("skill disintegrate");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_WITHER, SkillType.ABILITY_PROPERTY_DARK, SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
    }

    public String getDescription(Hero hero) {
        double baseDamage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        double damagePerStack = SkillConfigManager.getUseSetting(hero, this, "damage-per-effect-consumed", 20.0, false);
        double maximumBurningDamage = SkillConfigManager.getUseSetting(hero, this, "maximum-consume-damage", 100.0, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(baseDamage))
                .replace("$2", Util.decFormat.format(damagePerStack))
                .replace("$3", Util.decFormat.format(maximumBurningDamage));
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 25.0);
        config.set("damage-per-effect-consumed", 20.0);
        config.set("maximum-consume-damage", 100.0);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        double damagePerStack = SkillConfigManager.getUseSetting(hero, this, "damage-per-effect-consumed", 20.0, false);
        double addedDamage = 0;

        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);

        for (final Effect effect : targetCT.getEffects()) {
            if (!effect.isType(EffectType.WITHER))
                continue;

            if (effect instanceof Stacked) {
                addedDamage += ((Stacked) effect).getStackCount() * damagePerStack;
            } else {
                addedDamage += damagePerStack;
            }

            targetCT.removeEffect(effect);
        }

        double maximumAddedDamage = SkillConfigManager.getUseSetting(hero, this, "maximum-consume-damage", 100.0, false);
        if (addedDamage > maximumAddedDamage) {
            addedDamage = maximumAddedDamage;
        }

        double totalDamage = damage + addedDamage;
        String message = "    " + ChatComponents.GENERIC_SKILL + ChatColor.DARK_PURPLE + "Disintegrate Damage: " + Util.decFormat.format(totalDamage);

        if (addedDamage > 0) {
            message+= " (" + addedDamage + " from Withering Effects)";
        }

        hero.getPlayer().sendMessage(message);

        addSpellTarget(target, hero);
        damageEntity(target, hero.getPlayer(), totalDamage, EntityDamageEvent.DamageCause.MAGIC);

        FireworkEffect firework = FireworkEffect.builder()
                .flicker(false)
                .trail(false)
                .withColor(Color.FUCHSIA)
                .withColor(Color.BLACK)
                .withColor(Color.BLACK)
                .withColor(Color.ORANGE)
                .with(FireworkEffect.Type.BURST)
                .build();
        VisualEffect.playInstantFirework(firework, target.getLocation());

        return SkillResult.NORMAL;
    }
}
