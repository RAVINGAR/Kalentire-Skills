package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.ncp.NCPFunction;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import com.herocraftonline.heroes.characters.skill.skills.totem.SkillBaseTotem;
import com.herocraftonline.heroes.characters.skill.skills.totem.Totem;

public class SkillTremorTotem extends SkillBaseTotem {

	public SkillTremorTotem(Heroes plugin) {
        super(plugin, "TremorTotem");
        setDescription("Places a Tremor totem at the target location that deals $1 physical damage and knocks back targets in a $2 radius. Lasts for $3 seconds.");
        setUsage("/skill tremortotem");
        setArgumentRange(0, 0);
        setIdentifiers("skill tremortotem");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.FORCE, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.SILENCEABLE, SkillType.AREA_OF_EFFECT, SkillType.INTERRUPTING);
        material = Material.QUARTZ_BLOCK;
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 25, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.3, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);
    	
    	return getDescription()
    			.replace("$1", damage + "")
    			.replace("$2", getRange(hero) + "")
    			.replace("$3", getDuration(hero)*0.001 + "");
    }

    @Override
    public ConfigurationSection getSpecificDefaultConfig(ConfigurationSection node) {

        node.set(SkillSetting.DAMAGE.node(), 25);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.3);
        node.set("horizontal-power", 1.5);
        node.set("horizontal-power-increase-per-wisdom", 0.0375);
        node.set("vertical-power", 0.25);
        node.set("vertical-power-increase-per-wisdom", 0.0075);
        node.set("ncp-exemption-duration", 1500);
        node.set("max-targets", 5);

        return node;
    }

    @Override
    public void usePower(Hero hero, Totem totem) {
        Player player = hero.getPlayer();


        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 25, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.3, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        double hPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", 2.8, false);
        double vPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", 0.5, false);
        
        int maxTargets = SkillConfigManager.getUseSetting(hero, this, "max-targets", 0, false);
        int targetsHit = 0;
        for (Entity entity : totem.getTargets(hero)) {
            // Check to see if we've exceeded the max targets
            if (maxTargets > 0 && targetsHit >= maxTargets) {
                break;
            }
            if (!(entity instanceof LivingEntity)) {
                continue;
            }

            // Check if the target is damagable
            if (!damageCheck(player, (LivingEntity) entity)) {
                continue;
            }

            final LivingEntity target = (LivingEntity) entity;

            double individualHPower = hPower;
            double individualVPower = vPower;

            Material mat = target.getLocation().getBlock().getRelative(BlockFace.DOWN).getType();

            switch (mat) {
                case WATER:
                case LAVA:
                case SOUL_SAND:
                    individualHPower /= 2;
                    individualVPower /= 2;
                    break;
                default:
                    break;
            }

            // Damage the target
            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.MAGIC, false);

            // Do our knockback
            Location playerLoc = player.getLocation();
            Location targetLoc = target.getLocation();

            double xDir = targetLoc.getX() - playerLoc.getX();
            double zDir = targetLoc.getZ() - playerLoc.getZ();
            double magnitude = Math.sqrt(xDir * xDir + zDir * zDir);

            xDir = xDir / magnitude * individualHPower;
            zDir = zDir / magnitude * individualHPower;
            
//            // The effect code is up here because the targets are being sent flying up. Can't accurately put the effect where we want it then.
//            @SuppressWarnings("deprecation")
//            int id = entity.getLocation().getBlock().getRelative(BlockFace.DOWN).getTypeId();
//            /* This is the new Particle API system for Spigot - the first few int = id, data, offsetX/Y/Z, speed, count, radius)
//             * offset controls how spread out the particles are
//             * id and data only work for two particles: ITEM_BREAK and TILE_BREAK
//             * */
//            entity.getWorld().spigot().playEffect(entity.getLocation().add(0, 0.6, 0), Effect.TILE_BREAK, id, 0, 0, 0, 0, 1, 150, 16);
            entity.getWorld().spawnParticle(Particle.BLOCK_CRACK, entity.getLocation().add(0, 0.6, 0), 150, 0, 0, 0, 1, entity.getLocation().getBlock().getRelative(BlockFace.DOWN).getBlockData());
            
            // Let's bypass the nocheat issues...
            final Vector velocity = new Vector(xDir, individualVPower, zDir);
            NCPUtils.applyExemptions(target, new NCPFunction() {
                
                @Override
                public void execute()
                {
                    target.setVelocity(velocity);                    
                }
            }, Lists.newArrayList("MOVING"), SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 500, false));
            targetsHit++;
        }
    }
}
