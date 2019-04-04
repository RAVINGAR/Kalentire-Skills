package com.herocraftonline.heroes.characters.skill.reborn.shared;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import static org.bukkit.Color.*;

public class SkillHemorrhage extends TargettedSkill {

    //private static final Particle.DustOptions skillEffectDustOptions = new Particle.DustOptions(Color.RED, 1);

    public SkillHemorrhage(Heroes plugin) {
        super(plugin, "Hemorrhage");
        this.setDescription("Deliver a strong bash to your target, dealing $1 physical damage and interrupting their casting.");
        this.setUsage("/skill hemorrhage");
        this.setArgumentRange(0, 0);
        this.setIdentifiers("skill hemorrhage");
        this.setTypes(SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 60, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        String formattedDamage = Util.decFormat.format(damage);

        return getDescription().replace("$1", formattedDamage);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 3);
        config.set(SkillSetting.DAMAGE.node(), 30);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 0.7);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 30, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.7, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        // entrance of the hook
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SQUID_HURT, 0.2F, 0.5F);
        DustOptions hookEnter = new Particle.DustOptions(Color.RED, 1);
        player.getWorld().spawnParticle(Particle.REDSTONE, target.getEyeLocation(),25, 0, 0, 0, 1, hookEnter);

        // do damage
        addSpellTarget(target, hero);
        damageEntity(target, player, damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK);

        // removal of the hook
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SQUID_HURT, 0.4F, 1.0F);
        DustOptions hookExit = new Particle.DustOptions(Color.RED, 3);
        player.getWorld().spawnParticle(Particle.REDSTONE, target.getEyeLocation(),25, 0, 0, 0, 1, hookExit);
        return SkillResult.NORMAL;
    }
}
