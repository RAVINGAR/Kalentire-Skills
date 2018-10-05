package com.herocraftonline.heroes.characters.skill.pack5;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;

import org.bukkit.Effect;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.List;

public class SkillMegabolt extends TargettedSkill {

    public SkillMegabolt(Heroes plugin) {
        super(plugin, "Megabolt");
        setDescription("Calls down multiple bolts of lightning on your target, dealing $1 damage to them and all enemies within $2 blocks.");
        setUsage("/skill megabolt");
        setArgumentRange(0, 0);
        setIdentifiers("skill megabolt");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_LIGHTNING, SkillType.AREA_OF_EFFECT, SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 100, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.75, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

        return getDescription().replace("$1", damage + "").replace("$2", radius + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 7);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), 0.125);
        node.set(SkillSetting.DAMAGE.node(), 100);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.75);
        node.set(SkillSetting.RADIUS.node(), 5);
        node.set(SkillSetting.REAGENT.node(), 289);
        node.set(SkillSetting.REAGENT_COST.node(), 2);
        node.set("lightning-volume", 0.0F);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 100, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.75, false);
        damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        float lightningVolume = (float) SkillConfigManager.getUseSetting(hero, this, "lightning-volume", 0.0F, false);
        
        broadcastExecuteText(hero, target);

        // Damage the first target      
        // Lightning like this is too annoying.
        // target.getWorld().strikeLightningEffect(target.getLocation());
        target.getWorld().spigot().strikeLightningEffect(target.getLocation(), true);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, lightningVolume, 1.0F);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, lightningVolume, 1.0F);
        //target.getWorld().spigot().playEffect(target.getLocation(), Effect.EXPLOSION_HUGE, 0, 0, 0, 0, 0, 0, 1, 16);
        target.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, target.getLocation(), 1, 0, 0, 0, 0);
        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.MAGIC, false);

        List<Entity> entities = target.getNearbyEntities(radius, radius, radius);
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }

            // Check if the target is damagable
            if (!damageCheck(player, (LivingEntity) entity)) {
                continue;
            }

            LivingEntity newTarget = (LivingEntity) entity;

            // Damage the target
            addSpellTarget(newTarget, hero);
            damageEntity(newTarget, player, damage, DamageCause.MAGIC);
        }

        return SkillResult.NORMAL;
    }
}
