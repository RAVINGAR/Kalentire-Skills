package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.StunEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Util;

public class SkillBlackjack extends TargettedSkill {

    public SkillBlackjack(Heroes plugin) {
        super(plugin, "Blackjack");
        setDescription("Strike your target with a Blackjack, dealing $1 damage and stunning the target for $2 seconds. If you are invisible or sneaking, the stun will instead last $3 seconds.");
        setUsage("/skill blackjack");
        setArgumentRange(0, 0);
        setIdentifiers("skill blackjack");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.AGGRESSIVE, SkillType.DAMAGING, SkillType.STEALTHY);
    }

    @Override
    public String getDescription(Hero hero) {

        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 60, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1, false);
        damage += (int) (hero.getAttributeValue(AttributeType.STRENGTH) * damageIncrease);

        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 15, false);
        durationIncrease = hero.getAttributeValue(AttributeType.CHARISMA) * durationIncrease;

        double normalDuration = durationIncrease + SkillConfigManager.getUseSetting(hero, this, "normal-stun-duration", 500, false);
        double stealthtyDuration = durationIncrease + SkillConfigManager.getUseSetting(hero, this, "stealthy-stun-duration", 1500, false);

        String formattedNormalDuration = Util.decFormat.format(normalDuration / 1000.0);
        String formattedStealthyDuration = Util.decFormat.format(stealthtyDuration / 1000.0);

        return getDescription().replace("$1", damage + "").replace("$2", formattedNormalDuration).replace("$3", formattedStealthyDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 5);
        node.set(SkillSetting.DAMAGE.node(), 60);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 1);
        node.set(SkillSetting.DURATION_INCREASE_PER_CHARISMA.node(), 15);
        node.set("normal-stun-duration", 600);
        node.set("stealthy-stun-duration", 1400);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {

        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 60, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1, false);
        damage += (hero.getAttributeValue(AttributeType.STRENGTH) * damageIncrease);

        // Deal damage
        addSpellTarget(target, hero);
        damageEntity(target, player, damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK);

        player.getWorld().playSound(player.getLocation(), Sound.DOOR_CLOSE, 0.4F, 0.4F);
        player.getWorld().playSound(player.getLocation(), Sound.HURT, 0.2F, 1.0F);

        // Stun, but only if they are a player.
        if (target instanceof Player) {
            int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 15, false);
            int duration = hero.getAttributeValue(AttributeType.CHARISMA) * durationIncrease;

            if (hero.hasEffect("Sneak") || hero.hasEffect("Invisible"))
                duration += SkillConfigManager.getUseSetting(hero, this, "stealthy-stun-duration", 1500, false);
            else
                duration += SkillConfigManager.getUseSetting(hero, this, "normal-stun-duration", 500, false);

            Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
            targetHero.addEffect(new StunEffect(this, player, duration));
        }

        // Remove any invis effects the player may have on them at the time of use.
        for (final Effect effect : hero.getEffects()) {
            if (effect.isType(EffectType.INVIS)) {
                hero.removeEffect(effect);
            }
        }

        return SkillResult.NORMAL;
    }
}