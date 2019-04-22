package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.StunEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Effect;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillSuckerPunch extends TargettedSkill {

    public SkillSuckerPunch(Heroes plugin) {
        super(plugin, "SuckerPunch");
        setDescription("You sucker punch your target, dealing $1 physical damage and stunning them for $2 second(s). Damage and Stun duration increases with Strength.");
        setUsage("/skill suckerpunch");
        setArgumentRange(0, 0);
        setIdentifiers("skill suckerpunch");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DEBUFFING, SkillType.DAMAGING,
                SkillType.INTERRUPTING, SkillType.SILENCEABLE, SkillType.NO_SELF_TARGETTING);
    }

    @Override
    public String getDescription(Hero hero) {

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 30, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.7, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 750, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_STRENGTH, 30, false);
        duration += durationIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        String formattedDamage = Util.decFormat.format(damage);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDamage).replace("$2", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() 
    {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 4);
        node.set(SkillSetting.DURATION.node(), 1500);
        node.set(SkillSetting.DURATION_INCREASE_PER_STRENGTH.node(), 30);
        node.set(SkillSetting.DAMAGE.node(), 31);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 0.7);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        if (!damageCheck(player, target)) return SkillResult.INVALID_TARGET_NO_MSG;

        broadcastExecuteText(hero, target);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 30, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.7, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 750, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_STRENGTH, 30, false);
        duration += durationIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);

        plugin.getCharacterManager().getCharacter(target).addEffect(new StunEffect(this, player, duration));
       
        //target.getWorld().spigot().playEffect(target.getEyeLocation(), Effect.CRIT, 0, 0, 0.2F, 0.2F, 0.2F, 0.3F, 45, 16);
        target.getWorld().spawnParticle(Particle.CRIT, target.getEyeLocation(), 45, 0.2, 0.2, 0.2, 0.3);

        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7F, 2.0F);

        return SkillResult.NORMAL;
    }
}