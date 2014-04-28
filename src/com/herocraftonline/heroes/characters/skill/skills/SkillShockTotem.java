package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.totem.SkillBaseTotem;
import com.herocraftonline.heroes.characters.skill.skills.totem.Totem;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
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
        setTypes(SkillType.ABILITY_PROPERTY_LIGHTNING, SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCABLE, SkillType.AGGRESSIVE);
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
        // Sound is up here because it makes sense to hear it just before it happens in MC (where it's simultaneous)
        heroP.getWorld().playSound(heroP.getLocation(), Sound.AMBIENCE_THUNDER, 1.0F, 1.0F);
        for(LivingEntity entity : totem.getTargets(hero)) {
            if(!damageCheck(heroP, entity)) {
                continue;
            }
            // Lightning effect would be here, but it's not very nice.
            plugin.getDamageManager().addSpellTarget(entity, hero, this);
            damageEntity(entity, heroP, getDamage(hero));
            /* This is the new Particle API system for Spigot - the first few int = id, data, offsetX/Y/Z, speed, count, radius)
             * offset controls how spread out the particles are
             * id and data only work for two particles: ITEM_BREAK and TILE_BREAK
             * */
            entity.getWorld().spigot().playEffect(entity.getLocation().add(0, 0.6, 0), Effect.SNOW_SHOVEL, 0, 0, 0, 0, 0, 1, 25, 16);
            entity.getWorld().spigot().playEffect(entity.getLocation().add(0, 0.7, 0), Effect.SNOW_SHOVEL, 0, 0, 0, 0, 0, 1, 25, 16);
            entity.getWorld().spigot().playEffect(entity.getLocation().add(0, 0.9, 0), Effect.SNOW_SHOVEL, 0, 0, 0, 0, 0, 1, 25, 16);
            entity.getWorld().spigot().playEffect(entity.getLocation().add(0, 1.0, 0), Effect.SNOW_SHOVEL, 0, 0, 0, 0, 0, 1, 25, 16);
            entity.getWorld().spigot().playEffect(entity.getLocation().add(0, 1.1, 0), Effect.SNOW_SHOVEL, 0, 0, 0, 0, 0, 1, 25, 16);
            entity.getWorld().spigot().playEffect(entity.getLocation().add(0, 1.2, 0), Effect.SNOW_SHOVEL, 0, 0, 0, 0, 0, 1, 25, 16);        
        }
    }

    @Override
    public ConfigurationSection getSpecificDefaultConfig(ConfigurationSection node) {
        node.set(SkillSetting.DAMAGE.node(), Double.valueOf(50.0));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), Double.valueOf(5.0));
        return node;
    }

    // Methods to grab config info that is specific to this skill
    public double getDamage(Hero h) {
        return SkillConfigManager.getUseSetting(h, this, SkillSetting.DAMAGE, 50.0, false) + SkillConfigManager.getUseSetting(h, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 5.0, false) * h.getAttributeValue(AttributeType.INTELLECT);
    }

}