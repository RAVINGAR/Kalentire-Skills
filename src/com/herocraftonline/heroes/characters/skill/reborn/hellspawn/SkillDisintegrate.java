package com.herocraftonline.heroes.characters.skill.reborn.hellspawn;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.interfaces.Burning;
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

import java.util.ArrayList;
import java.util.List;

public class SkillDisintegrate extends TargettedSkill {

    public SkillDisintegrate(Heroes plugin) {
        super(plugin, "Disintegrate");
        setDescription("You Disintegrate an enemy, dealing $1 damage and consuming all of their Withering effects. " +
                "You deal an additional $2 damage for each effect consumed this way. " +
                "The maximum additional damage that can be dealt this way is $3.");
        setUsage("/skill disintegrate");
        setIdentifiers("skill disintegrate");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_WITHER, SkillType.ABILITY_PROPERTY_DARK, SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
    }

    public String getDescription(Hero hero) {
        double baseDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 25.0, false);
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

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 25.0, false);
        double damagePerStack = SkillConfigManager.getUseSetting(hero, this, "damage-per-effect-consumed", 20.0, false);
        double addedDamage = 0;

        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);

        for (final Effect effect : targetCT.getEffects()) {
            if (!effect.isType(EffectType.WITHER))
                continue;

            addedDamage+= damagePerStack;
            targetCT.removeEffect(effect);
        }

        if (addedDamage > 0) {
            double maximumAddedDamage = SkillConfigManager.getUseSetting(hero, this, "maximum-consume-damage", 100.0, false);
            if (addedDamage > maximumAddedDamage) {
                addedDamage = maximumAddedDamage;
            }
            hero.getPlayer().sendMessage("    " + ChatComponents.GENERIC_SKILL + ChatColor.DARK_PURPLE + "Disintegrate Consume Damage: " + addedDamage);
        }

        damage+= addedDamage;
        addSpellTarget(target, hero);
        damageEntity(target, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.MAGIC);

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
