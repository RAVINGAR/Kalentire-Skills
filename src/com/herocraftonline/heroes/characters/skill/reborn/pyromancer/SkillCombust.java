package com.herocraftonline.heroes.characters.skill.reborn.pyromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.common.interfaces.Burning;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

public class SkillCombust extends TargettedSkill {

    public SkillCombust(Heroes plugin) {
        super(plugin, "Combust");
        setDescription("You Combust an enemy, absorbing all of their current fire ticks, dealing $1 damage in addition to all of the absorbed burning damage at a $2% increase rate.");
        setUsage("/skill combust");
        setIdentifiers("skill combust");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_FIRE, SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
    }

    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 25.0, false);
        double damageEffectiveness = SkillConfigManager.getUseSetting(hero, this, "burning-damage-effectiveness", 1.5, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(damage))
                .replace("$2", Util.decFormat.format(damageEffectiveness * 100));
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 25.0);
        config.set("burning-damage-effectiveness", 1.5);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {

        broadcastExecuteText(hero, target);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 25.0, false);
        double damageEffectiveness = SkillConfigManager.getUseSetting(hero, this, "damage-effectiveness", 1.5, false);
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

        damage+= addedDamage;
        if (addedDamage > 0)
            hero.getPlayer().sendMessage(ChatComponents.GENERIC_SKILL + "Combust Burning Damage: " + addedDamage);

        addSpellTarget(target, hero);
        damageEntity(target, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.MAGIC);

        FireworkEffect firework = FireworkEffect.builder()
                .flicker(false)
                .trail(true)
                .withColor(Color.RED)
                .withColor(Color.RED)
                .withColor(Color.ORANGE)
                .withFade(Color.BLACK)
                .with(FireworkEffect.Type.BURST)
                .build();
        VisualEffect.playInstantFirework(firework, target.getLocation());

        return SkillResult.NORMAL;
    }
}
