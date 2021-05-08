package com.herocraftonline.heroes.characters.skill.skills;

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
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 3.8, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        return getDescription().replace("$1", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), 180);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.4);
        node.set(SkillSetting.MAX_DISTANCE.node(), 9);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), 0.2);
        node.set(SkillSetting.REAGENT.node(), "GUNPOWDER");
        node.set(SkillSetting.REAGENT_COST.node(), 1);
        node.set("lightning-volume", 0.0F);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 180, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.4, false);
        damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));
        
        float lightningVolume = (float) SkillConfigManager.getUseSetting(hero, this, "lightning-volume", 0.0F, false);
        
        // Lightning like this is too annoying.
        // target.getWorld().strikeLightningEffect(target.getLocation());
        target.getWorld().spigot().strikeLightningEffect(target.getLocation(), true);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, lightningVolume, 1.0F);
        plugin.getDamageManager().addSpellTarget(target, hero, this);
        damageEntity(target, player, damage, DamageCause.MAGIC, false);

        broadcastExecuteText(hero, target);

        return SkillResult.NORMAL;
    }
}
