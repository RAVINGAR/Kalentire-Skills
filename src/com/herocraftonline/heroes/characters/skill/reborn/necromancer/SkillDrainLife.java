package com.herocraftonline.heroes.characters.skill.reborn.necromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.LineEffect;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

public class SkillDrainLife extends TargettedSkill {

    public SkillDrainLife(Heroes plugin) {
        super(plugin, "DrainLife");
        setDescription("Drain the life of your target, dealing $1 damage and restoring $2 of your own health.");
        setUsage("/skill drainlife");
        setArgumentRange(0, 0);
        setIdentifiers("skill drainlife");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_DARK, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);

        double healMult = SkillConfigManager.getUseSetting(hero, this, "healing-multiplier", 1.5, false);

        String formattedDamage = Util.decFormat.format(damage);
        String formattedHeal = Util.decFormat.format(damage * healMult);

        return getDescription().replace("$1", formattedDamage).replace("$2", formattedHeal);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 12);
        config.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), 0.0);
        config.set(SkillSetting.DAMAGE.node(), 40);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.0);
        config.set("healing-multiplier", 1.5);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.MAGIC);

        double multiplier = SkillConfigManager.getUseSetting(hero, this, "healing-multiplier", 1.5, false);
        hero.tryHeal(hero, this, damage * multiplier, true);

        EffectManager effectManager = new EffectManager(plugin);
        LineEffect lineVisual = new LineEffect(effectManager);
        lineVisual.particle = Particle.REDSTONE;
        lineVisual.color = Color.RED;
        lineVisual.setLocation(player.getLocation().add(new Vector(0, 0.5, 0)));
        lineVisual.setTargetEntity(target);

        effectManager.start(lineVisual);

        player.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0F, 1.0F);

        return SkillResult.NORMAL;
    }
}
