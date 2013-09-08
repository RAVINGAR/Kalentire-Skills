package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillSoothe extends TargettedSkill {

    public SkillSoothe(Heroes plugin) {
        super(plugin, "Soothe");
        setDescription("Soothes your target, restoring $1 health and curing withering effects.");
        setUsage("/skill soothe <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill soothe");
        setTypes(SkillType.ABILITY_PROPERTY_LIGHT, SkillType.DISPELLING, SkillType.HEALING, SkillType.SILENCABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING.node(), Integer.valueOf(125), false);
        double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), Double.valueOf(2.0), false);
        healing += (hero.getAttributeValue(AttributeType.WISDOM) * healingIncrease);

        String formattedHealing = Util.decFormat.format(healing);

        return getDescription().replace("$1", formattedHealing);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(8));
        node.set(SkillSetting.HEALING.node(), Integer.valueOf(65));
        node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), Double.valueOf(1.5));

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

        broadcastExecuteText(hero, target);

        targetHero.heal(hrhEvent.getAmount());

        // Soothe cures WITHER!
        for (Effect effect : targetHero.getEffects()) {
            if (effect.isType(EffectType.DISPELLABLE) && effect.isType(EffectType.HARMFUL)) {
                if (effect.isType(EffectType.WITHER)) {
                    targetHero.removeEffect(effect);
                }
            }
        }

        player.getWorld().playSound(player.getLocation(), Sound.CHICKEN_EGG_POP, 0.5F, 0.01F);

        return SkillResult.NORMAL;
    }
}