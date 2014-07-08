package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.NauseaEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.totem.SkillBaseTotem;
import com.herocraftonline.heroes.characters.skill.skills.totem.Totem;
import com.herocraftonline.heroes.util.Messaging;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SkillForceTotem extends SkillBaseTotem {

    private boolean ncpEnabled = false;

    public SkillForceTotem(Heroes plugin) {
        super(plugin, "ForceTotem");
        setArgumentRange(0,0);
        setUsage("/skill forcetotem");
        setIdentifiers("skill forcetotem");
        setDescription("Places a force totem at target location that throws non-partied entites in a $1 radius into the air, dealing $2 damage and disorienting them. Lasts for $3 seconds.");
        setTypes(SkillType.FORCE, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
        material = Material.QUARTZ_BLOCK;
        
        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null) {
            ncpEnabled = true;
        }
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
        for(LivingEntity entity : totem.getTargets(hero)) {
            if(!damageCheck(heroP, entity)) {
                continue;
            }
            Player player = null;
            if(entity instanceof Player) {
                player = (Player) entity;
            }
            
            // Let's bypass the nocheat issues...
            if (ncpEnabled) {
                if (player != null) {
                    if (!player.isOp()) {
                        long duration = SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 2000, false);
                        if (duration > 0) {
                            NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(this, player, duration);
                            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(entity);
                            targetCT.addEffect(ncpExemptEffect);
                        }
                    }
                }
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
            entity.getWorld().spigot().playEffect(entity.getLocation().add(0, 0.6, 0), Effect.TILE_BREAK, id, 0, 0, 0, 0, 1, 25, 16);
            entity.getWorld().spigot().playEffect(entity.getLocation().add(0, 0.7, 0), Effect.TILE_BREAK, id, 0, 0, 0, 0, 1, 25, 16);
            entity.getWorld().spigot().playEffect(entity.getLocation().add(0, 0.9, 0), Effect.TILE_BREAK, id, 0, 0, 0, 0, 1, 25, 16);
            entity.getWorld().spigot().playEffect(entity.getLocation().add(0, 1.0, 0), Effect.TILE_BREAK, id, 0, 0, 0, 0, 1, 25, 16);
            entity.getWorld().spigot().playEffect(entity.getLocation().add(0, 1.1, 0), Effect.TILE_BREAK, id, 0, 0, 0, 0, 1, 25, 16);
            entity.getWorld().spigot().playEffect(entity.getLocation().add(0, 1.2, 0), Effect.TILE_BREAK, id, 0, 0, 0, 0, 1, 25, 16);
            
            double damage = getDamage(hero);
            if(damage > 0) {
                damageEntity(entity, heroP, damage);
            }
            character.addEffect(new NauseaEffect(this, "ForceTotemNauseaEffect", heroP, getDisorientationDuration(hero), getDisorientationLevel(hero), null, getUnapplyText()));
            entity.setVelocity(new Vector(0, getLaunch(hero), 0));
            entity.setFallDistance(-512);
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
    
    private class NCPExemptionEffect extends ExpirableEffect {

        public NCPExemptionEffect(Skill skill, Player applier, long duration) {
            super(skill, "NCPExemptionEffect_MOVING", applier, duration);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.exemptPermanently(player, CheckType.MOVING);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.unexempt(player, CheckType.MOVING);
        }
    }
}