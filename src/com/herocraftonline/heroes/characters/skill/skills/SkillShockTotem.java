package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.totem.SkillBaseTotem;
import com.herocraftonline.heroes.characters.skill.skills.totem.Totem;
import com.herocraftonline.heroes.util.CompatSound;

// import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillShockTotem extends SkillBaseTotem {

    public SkillShockTotem(Heroes plugin) {
        super(plugin, "ShockTotem");
        setArgumentRange(0,0);
        setUsage("/skill shocktotem");
        setIdentifiers("skill shocktotem");
        setDescription("Places a shock totem at target location that strikes lightning on entities in a $1 radius dealing $2 damage. Lasts for $3 seconds.");
        setTypes(SkillType.ABILITY_PROPERTY_LIGHTNING, SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT);
        material = Material.NETHER_BRICK;
    }

    @Override
    public String getDescription(Hero h) {
        return getDescription()
                .replace("$1", getRange(h) + "")
                .replace("$2", getDamage(h) + "")
                .replace("$3", getDuration(h)*0.001 + "");
    }

    @Override
    public void usePower(Hero hero, Totem totem) {

        Player heroP = hero.getPlayer();
        // Sound used to be up here to go along with the effect that replaced lightning. We use lightning now
        // heroP.getWorld().playSound(heroP.getLocation(), CompatSound.ENTITY_LIGHTNING_THUNDER.value(), 1.0F, 1.0F);
        int maxTargets = SkillConfigManager.getUseSetting(hero, this, "max-targets", 0, false);
        int targetsHit = 0;
        for(LivingEntity entity : totem.getTargets(hero)) {
            // Check to see if we've exceeded the max targets
            if (maxTargets > 0 && targetsHit >= maxTargets) {
                break;
            }
            if(!damageCheck(heroP, entity)) {
                continue;
            }
            plugin.getDamageManager().addSpellTarget(entity, hero, this);
            damageEntity(entity, heroP, getDamage(hero));
            // Strike some lightning
            entity.getWorld().spigot().strikeLightningEffect(entity.getLocation(), true);
            entity.getWorld().playSound(entity.getLocation(), CompatSound.ENTITY_LIGHTNING_THUNDER.value(), getLightningVolume(hero), 1.0F);
            targetsHit++;
            // We have real lightning now, so this is pointless
            /* This is the new Particle API system for Spigot - the first few int = id, data, offsetX/Y/Z, speed, count, radius)
             * offset controls how spread out the particles are
             * id and data only work for two particles: ITEM_BREAK and TILE_BREAK
             * */
            /* entity.getWorld().spigot().playEffect(entity.getLocation().add(0, 0.6, 0), Effect.SNOW_SHOVEL, 0, 0, 0, 0, 0, 1, 25, 16);
            entity.getWorld().spigot().playEffect(entity.getLocation().add(0, 0.7, 0), Effect.SNOW_SHOVEL, 0, 0, 0, 0, 0, 1, 25, 16);
            entity.getWorld().spigot().playEffect(entity.getLocation().add(0, 0.9, 0), Effect.SNOW_SHOVEL, 0, 0, 0, 0, 0, 1, 25, 16);
            entity.getWorld().spigot().playEffect(entity.getLocation().add(0, 1.0, 0), Effect.SNOW_SHOVEL, 0, 0, 0, 0, 0, 1, 25, 16);
            entity.getWorld().spigot().playEffect(entity.getLocation().add(0, 1.1, 0), Effect.SNOW_SHOVEL, 0, 0, 0, 0, 0, 1, 25, 16);
            entity.getWorld().spigot().playEffect(entity.getLocation().add(0, 1.2, 0), Effect.SNOW_SHOVEL, 0, 0, 0, 0, 0, 1, 25, 16);
            */        
        }
    }

    @Override
    public ConfigurationSection getSpecificDefaultConfig(ConfigurationSection node) {
        node.set(SkillSetting.DAMAGE.node(), 50.0);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 5.0);
        node.set("lightning-volume", 0.0F);
        node.set("max-targets", 5);
        return node;
    }

    // Methods to grab config info that is specific to this skill
    public double getDamage(Hero h) {
        return SkillConfigManager.getUseSetting(h, this, SkillSetting.DAMAGE, 50.0, false) + SkillConfigManager.getUseSetting(h, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 5.0, false) * h.getAttributeValue(AttributeType.INTELLECT);
    }
    
    public float getLightningVolume(Hero h) {
    	return (float) SkillConfigManager.getUseSetting(h, this, "lightning-volume", 0.0F, false);
    }

}