package com.herocraftonline.heroes.characters.skill.pack5;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;

import org.bukkit.Effect;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillArcaneblast extends TargettedSkill {

    public SkillArcaneblast(Heroes plugin) {
        super(plugin, "Arcaneblast");
        setDescription("You arcaneblast the target for $1 light damage.");
        setUsage("/skill arcaneblast");
        setArgumentRange(0, 0);
        setIdentifiers("skill arcaneblast");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_LIGHT, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
    }

    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 200, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 2.5, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        return getDescription().replace("$1", damage + "");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 10);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), 0.2);
        node.set(SkillSetting.DAMAGE.node(), 200);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 2.5);

        return node;
    }

    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 200, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 2.5, false);
        damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.MAGIC);

        broadcastExecuteText(hero, target);

        //public void playEffect(Location location, Effect effect,  id,  data,  offsetX,  offsetY,  offsetZ,  speed,  particleCount,  radius)
        //target.getWorld().spigot().playEffect(target.getLocation(), Effect.EXPLOSION, 1, 1, 0F, 1F, 0F, 10F, 200, 10);
        target.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, 200, 0, 1, 0, 10);
        //target.getWorld().spigot().playEffect(target.getLocation(), Effect.EXPLOSION_LARGE, 1, 1, 0F, 1F, 0F, 0.1F, 10, 10);
        target.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, 10, 0, 1, 0, 0.1);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_FIREWORK_LARGE_BLAST, 7.0F, 0.5F);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 7.0F, 1.0F);
        
        return SkillResult.NORMAL;
    }
}
