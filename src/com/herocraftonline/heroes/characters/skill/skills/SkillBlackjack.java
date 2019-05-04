package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
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
        setDescription("Strike your target with a Blackjack, dealing $1 damage and stunning the target for $2 second(s). " +
                "If you are invisible or sneaking, you will instead deal $3 damage.");
        setUsage("/skill blackjack");
        setArgumentRange(0, 0);
        setIdentifiers("skill blackjack");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.AGGRESSIVE, SkillType.DAMAGING, SkillType.STEALTHY);
    }

    @Override
    public String getDescription(Hero hero) {

        int str = hero.getAttributeValue(AttributeType.STRENGTH);

        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 60, false);
        damage += str * damageIncrease;
        double stealthyDamage = SkillConfigManager.getUseSetting(hero, this, "stealthy-damage", 60, false);
        stealthyDamage += str * damageIncrease;

        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 15, false);
        durationIncrease = hero.getAttributeValue(AttributeType.CHARISMA) * durationIncrease;

        double duration = durationIncrease + SkillConfigManager.getUseSetting(hero, this, "stun-duration", 500, false);

        String formattedDamage = Util.decFormat.format(damage);
        String formattedStealthyDamage = Util.decFormat.format(stealthyDamage);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDamage).replace("$2", formattedDuration).replace("$3", formattedStealthyDamage);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 5);
        node.set(SkillSetting.DAMAGE.node(), 30);
        node.set("stealthy-damage", 60);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 1);
        node.set(SkillSetting.DURATION_INCREASE_PER_CHARISMA.node(), 15);
        node.set("stun-duration", 1250);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {

        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        int str = hero.getAttributeValue(AttributeType.STRENGTH);

        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 60, false);
        damage += str * damageIncrease;
        double stealthyDamage = SkillConfigManager.getUseSetting(hero, this, "stealthy-damage", 60, false);
        stealthyDamage += str * damageIncrease;

        // Deal damage
        addSpellTarget(target, hero);

        boolean isStealthy = false;
        for (Effect effect : hero.getEffects()) {
            if (effect.isType(EffectType.INVIS) || effect.isType(EffectType.SNEAK)) {
                isStealthy = true;
                break;
            }
        }

        if (isStealthy)
            damageEntity(target, player, stealthyDamage, EntityDamageEvent.DamageCause.ENTITY_ATTACK);
        else
            damageEntity(target, player, damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK);

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_WOODEN_DOOR_CLOSE, 0.4F, 0.4F);


        // Stun, but only if they are a player.
        if (target instanceof Player) {
            int duration = SkillConfigManager.getUseSetting(hero, this, "stun-duration", 500, false);
            int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 15, false);
            duration += hero.getAttributeValue(AttributeType.CHARISMA) * durationIncrease;

            Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
            targetHero.addEffect(new StunEffect(this, player, duration));
        }

        // Remove any invis effects the player may have on them at the time of use.
        for (final Effect effect : hero.getEffects()) {
            if (effect.isType(EffectType.INVIS)) {
                hero.removeEffect(effect);
            }
        }

        //player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), org.bukkit.Effect.COLOURED_DUST, 0, 0, 0, 0, 0, 1, 150, 16);
        player.getWorld().spawnParticle(Particle.REDSTONE, target.getLocation().add(0, 0.5, 0), 150, 0, 0, 0, 1, new Particle.DustOptions(Color.BLACK, 1));
        return SkillResult.NORMAL;
    }
}