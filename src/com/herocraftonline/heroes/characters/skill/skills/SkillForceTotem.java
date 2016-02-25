package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.NauseaEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.ncp.NCPFunction;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import com.herocraftonline.heroes.characters.skill.skills.totem.SkillBaseTotem;
import com.herocraftonline.heroes.characters.skill.skills.totem.Totem;
import com.herocraftonline.heroes.util.Messaging;

public class SkillForceTotem extends SkillBaseTotem {

    public SkillForceTotem(Heroes plugin) {
        super(plugin, "ForceTotem");
        setArgumentRange(0,0);
        setUsage("/skill forcetotem");
        setIdentifiers("skill forcetotem");
        setDescription("Places a force totem at target location that throws non-partied entites in a $1 radius into the air, dealing $2 damage and disorienting them. Lasts for $3 seconds.");
        setTypes(SkillType.FORCE, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT);
        material = Material.QUARTZ_BLOCK;
    }

    @Override
    public String getDescription(Hero h) {
        return getDescription()
                .replace("$1", getRange(h) + "")
                .replace("$2", getDamage(h) + "")
                .replace("$3", getDuration(h)*0.001 + "");
    }

    @Override
    public void usePower(final Hero hero, Totem totem) {
        Player heroP = hero.getPlayer();
        double damage = getDamage(hero);
        // An example of not limiting if damage is 0. Since this is the case on live, it makes for a good example.
        int maxTargets = damage > 0 ? SkillConfigManager.getUseSetting(hero, this, "max-targets", 0, false) : 0;
        int targetsHit = 0;
        for(final LivingEntity entity : totem.getTargets(hero)) {
            // Check to see if we've exceeded the max targets
            if (maxTargets > 0 && targetsHit >= maxTargets) {
                break;
            }
            
            if(!damageCheck(heroP, entity)) {
                continue;
            }
            Player player = null;
            if(entity instanceof Player) {
                player = (Player) entity;
            }
            
            CharacterTemplate character = plugin.getCharacterManager().getCharacter(entity);
            if(!character.hasEffect("ForceTotemNauseaEffect")) {
                String name;
                if(player != null) {
                    name = player.getName();
                }
                else name = Messaging.getLivingEntityName(character);
                broadcast(entity.getLocation(), getApplyText(), name, heroP.getName());
            }
            else {
                ((ExpirableEffect)character.getEffect("ForceTotemNauseaEffect")).setExpireText(null);
            }
            
            // The effect code is up here because the targets are being sent flying up. Can't accurately put the effect where we want it then.
            @SuppressWarnings("deprecation")
            int id = entity.getLocation().getBlock().getRelative(BlockFace.DOWN).getTypeId();
            /* This is the new Particle API system for Spigot - the first few int = id, data, offsetX/Y/Z, speed, count, radius)
             * offset controls how spread out the particles are
             * id and data only work for two particles: ITEM_BREAK and TILE_BREAK
             * */
            entity.getWorld().spigot().playEffect(entity.getLocation().add(0, 0.6, 0), Effect.TILE_BREAK, id, 0, 0, 0, 0, 1, 150, 16);
            
            if(damage > 0) {
                damageEntity(entity, heroP, damage);
            }
            character.addEffect(new NauseaEffect(this, "ForceTotemNauseaEffect", heroP, getDisorientationDuration(hero), getDisorientationLevel(hero), null, getUnapplyText()));

            // Let's bypass the nocheat issues...
            NCPUtils.applyExemptions(entity, new NCPFunction() {
                
                @Override
                public void execute()
                {
                    entity.setVelocity(new Vector(0, getLaunch(hero), 0));
                    entity.setFallDistance(-512);
                }
            }, Lists.newArrayList("MOVING"), SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 2000, false));
            targetsHit++;
        }
    }

    @Override
    public ConfigurationSection getSpecificDefaultConfig(ConfigurationSection node) {
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "$1 is disoriented by a totem's power!");
        node.set(SkillSetting.UNAPPLY_TEXT.node(), Messaging.getSkillDenoter() + "$1 is no longer disoriented by a totem's power.");
        node.set(SkillSetting.DAMAGE.node(), 50.0);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 5.0);
        node.set("base-launch", 1.0);
        node.set("launch-per-wisdom", 0.01);
        node.set("disorientation-level", 1);
        node.set("disorientation-duration", 5000);
        node.set("ncp-exemption-duration", 2000);
        node.set("max-targets", 5);
        return node;
    }

    // Methods to grab config info that is specific to this skill
    public double getDamage(Hero h) {
        return SkillConfigManager.getUseSetting(h, this, SkillSetting.DAMAGE, 50.0, false) + SkillConfigManager.getUseSetting(h, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 5.0, false) * h.getAttributeValue(AttributeType.INTELLECT);
    }

    public double getLaunch(Hero h) {
        return SkillConfigManager.getUseSetting(h, this, "base-launch", 5.0, false) + SkillConfigManager.getUseSetting(h, this, "launch-per-wisdom", 0.1, false) * h.getAttributeValue(AttributeType.WISDOM);
    }

    public int getDisorientationLevel(Hero h) {
        return SkillConfigManager.getUseSetting(h, this, "disorientation-level", 2, false);
    }

    public int getDisorientationDuration(Hero h) {
        return SkillConfigManager.getUseSetting(h, this, "disorientation-duration", 5000, false);
    }

    public String getApplyText() {
        return SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "$1 is disoriented by a totem's power!");
    }

    public String getUnapplyText() {
        return SkillConfigManager.getRaw(this, SkillSetting.UNAPPLY_TEXT, Messaging.getSkillDenoter() + "$1 is no longer disoriented by a totem's power.");
    }
}