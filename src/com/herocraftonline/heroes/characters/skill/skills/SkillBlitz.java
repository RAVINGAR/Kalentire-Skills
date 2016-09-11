package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.CompatSound;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillBlitz extends TargettedSkill {
    
    public SkillBlitz(Heroes plugin) {
        super(plugin, "Blitz");
        setDescription("Strike your target with a blitz of lightning, deal $1 damage to the target.");
        setUsage("/skill blitz");
        setArgumentRange(0, 0);
        setIdentifiers("skill blitz");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_LIGHTNING, SkillType.STEALTHY, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 180, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 2, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        return getDescription().replace("$1", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 5);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), 0.1);
        node.set(SkillSetting.DAMAGE.node(), 180);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 2);
        node.set(SkillSetting.REAGENT.node(), 289);
        node.set(SkillSetting.REAGENT_COST.node(), 1);
        node.set("lightning-volume", 0.0F);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 180, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 2, false);
        damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));
        
        float lightningVolume = (float) SkillConfigManager.getUseSetting(hero, this, "lightning-volume", 0.0F, false);
        
        // Lightning like this is too annoying.
        // target.getWorld().strikeLightningEffect(target.getLocation());
        target.getWorld().spigot().strikeLightningEffect(target.getLocation(), true);
        player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_GENERIC_EXPLODE.value(), 0.5F, 1.0F);
        target.getWorld().playSound(target.getLocation(), CompatSound.ENTITY_LIGHTNING_THUNDER.value(), lightningVolume, 1.0F);
        plugin.getDamageManager().addSpellTarget(target, hero, this);
        damageEntity(target, player, damage, DamageCause.MAGIC, false);

        broadcastExecuteText(hero, target);

        return SkillResult.NORMAL;
    }
}
