package com.herocraftonline.heroes.characters.skill.reborn;

import com.herocraftonline.heroes.util.Util;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;

public class SkillBolt extends TargettedSkill {
    
    public SkillBolt(Heroes plugin) {
        super(plugin, "Bolt");
        setDescription("Calls a bolt of lightning down on the target dealing $1 damage.");
        setUsage("/skill bolt");
        setArgumentRange(0, 0);
        setIdentifiers("skill bolt");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_LIGHTNING, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 150, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.0, false);
        damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        return getDescription()
                .replace("$1", Util.decFormat.format(damage));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 180);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.0);
        config.set(SkillSetting.MAX_DISTANCE.node(), 12);
        config.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), 0.0);
        config.set(SkillSetting.REAGENT.node(), "GUNPOWDER");
        config.set(SkillSetting.REAGENT_COST.node(), 1);
        config.set("lightning-volume", 0.5F);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 180, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.0, false);
        damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        float lightningVolume = (float) SkillConfigManager.getUseSetting(hero, this, "lightning-volume", 0.5F, false);

        target.getWorld().spigot().strikeLightningEffect(target.getLocation(), true);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, lightningVolume, 1.0F);
        plugin.getDamageManager().addSpellTarget(target, hero, this);
        damageEntity(target, player, damage, DamageCause.MAGIC, false);

        return SkillResult.NORMAL;
    }
}
